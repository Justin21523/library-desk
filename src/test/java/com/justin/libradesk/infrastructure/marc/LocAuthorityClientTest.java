package com.justin.libradesk.infrastructure.marc;

import com.justin.libradesk.dto.AuthoritySuggestion;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocAuthorityClientTest {

    private String fixture() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/marc/loc-authority-suggest.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesSuggestHits() throws Exception {
        String json = fixture();
        LocAuthorityClient client = new LocAuthorityClient("http://example/authorities", uri -> json);

        List<AuthoritySuggestion> results = client.suggestNames("twain");

        assertEquals(2, results.size());
        assertEquals("Twain, Mark, 1835-1910", results.get(0).label());
        assertTrue(results.get(0).uri().contains("n79021164"));
    }

    @Test
    void buildsSubjectSuggestUri() throws Exception {
        String json = fixture();
        java.util.concurrent.atomic.AtomicReference<URI> seen = new java.util.concurrent.atomic.AtomicReference<>();
        LocAuthorityClient client = new LocAuthorityClient("http://example/authorities", uri -> {
            seen.set(uri);
            return json;
        });

        client.suggestSubjects("java");

        assertTrue(seen.get().toString().contains("/subjects/suggest2/"), seen.get().toString());
        assertTrue(seen.get().toString().contains("q=java"), seen.get().toString());
    }
}
