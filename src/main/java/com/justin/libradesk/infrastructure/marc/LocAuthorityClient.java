package com.justin.libradesk.infrastructure.marc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.justin.libradesk.config.AppConfig;
import com.justin.libradesk.dto.AuthoritySuggestion;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Looks up authorized name/subject headings from the id.loc.gov suggest service
 * (LCNAF / LCSH), returning {@link AuthoritySuggestion}s parsed from its JSON.
 * HTTP is behind an {@link HttpFetcher} seam so tests run offline against a
 * recorded fixture.
 */
public class LocAuthorityClient {

    private static final String DEFAULT_BASE_URL = "https://id.loc.gov/authorities";

    @FunctionalInterface
    public interface HttpFetcher {
        String get(URI uri) throws IOException, InterruptedException;
    }

    private final String baseUrl;
    private final HttpFetcher fetcher;
    private final ObjectMapper mapper = new ObjectMapper();

    public LocAuthorityClient(AppConfig config) {
        this(config.getString("loc.authority.url", DEFAULT_BASE_URL), defaultFetcher());
    }

    LocAuthorityClient(String baseUrl, HttpFetcher fetcher) {
        this.baseUrl = baseUrl;
        this.fetcher = fetcher;
    }

    public List<AuthoritySuggestion> suggestNames(String query) {
        return suggest("names", query);
    }

    public List<AuthoritySuggestion> suggestSubjects(String query) {
        return suggest("subjects", query);
    }

    private List<AuthoritySuggestion> suggest(String vocabulary, String query) {
        URI uri = URI.create(baseUrl + "/" + vocabulary + "/suggest2/?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&count=10");
        try {
            return parse(fetcher.get(uri));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Authority lookup interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Authority lookup failed: " + e.getMessage(), e);
        }
    }

    List<AuthoritySuggestion> parse(String json) {
        List<AuthoritySuggestion> suggestions = new ArrayList<>();
        try {
            JsonNode hits = mapper.readTree(json).get("hits");
            if (hits != null && hits.isArray()) {
                for (JsonNode hit : hits) {
                    String label = text(hit, "suggestLabel");
                    if (label != null) {
                        suggestions.add(new AuthoritySuggestion(label, text(hit, "uri")));
                    }
                }
            }
            return suggestions;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse authority response", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static HttpFetcher defaultFetcher() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        return uri -> {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " from " + uri);
            }
            return response.body();
        };
    }
}
