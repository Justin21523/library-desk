package com.justin.libradesk.infrastructure.marc;

import com.justin.libradesk.config.AppConfig;
import com.justin.libradesk.util.Isbn;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
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
 * Copy cataloging against the Library of Congress SRU service: search by ISBN or
 * title and return candidate records as {@link MarcData}. The SRU response wraps
 * MARCXML records in {@code srw:} elements, so each MARC21-slim {@code record} is
 * extracted and decoded via {@link MarcService#fromXmlString}.
 *
 * <p>HTTP access is behind an {@link HttpFetcher} seam so tests can run offline
 * against a recorded fixture.
 */
public class LocSruClient {

    private static final String DEFAULT_BASE_URL = "http://lx2.loc.gov:210/lcdb";
    private static final String MARC_SLIM_NS = "http://www.loc.gov/MARC21/slim";

    /** Fetches the body of an HTTP GET; the seam that tests replace. */
    @FunctionalInterface
    public interface HttpFetcher {
        String get(URI uri) throws IOException, InterruptedException;
    }

    private final MarcService marcService;
    private final String baseUrl;
    private final HttpFetcher fetcher;

    public LocSruClient(MarcService marcService, AppConfig config) {
        this(marcService, config.getString("loc.sru.url", DEFAULT_BASE_URL), defaultFetcher());
    }

    LocSruClient(MarcService marcService, String baseUrl, HttpFetcher fetcher) {
        this.marcService = marcService;
        this.baseUrl = baseUrl;
        this.fetcher = fetcher;
    }

    public List<MarcData> searchByIsbn(String isbn) {
        return search("bath.isbn=" + Isbn.normalize(isbn));
    }

    public List<MarcData> searchByTitle(String title) {
        return search("bath.title=" + title);
    }

    private List<MarcData> search(String cqlQuery) {
        try {
            return parse(fetcher.get(buildUri(cqlQuery)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LoC search interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("LoC search failed: " + e.getMessage(), e);
        }
    }

    URI buildUri(String cqlQuery) {
        String query = "version=1.1&operation=searchRetrieve&recordSchema=marcxml&maximumRecords=5&query="
                + URLEncoder.encode(cqlQuery, StandardCharsets.UTF_8);
        return URI.create(baseUrl + "?" + query);
    }

    /** Extracts each MARC21-slim record from an SRU response and decodes it. */
    List<MarcData> parse(String sruXml) {
        List<MarcData> results = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(sruXml)));
            NodeList records = document.getElementsByTagNameNS(MARC_SLIM_NS, "record");
            for (int i = 0; i < records.getLength(); i++) {
                results.add(marcService.fromXmlString(serialize(records.item(i))));
            }
            return results;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse SRU response", e);
        }
    }

    private String serialize(Node node) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
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
