package com.justin.libradesk.infrastructure.marc;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocSruClientTest {

    private final MarcService marcService = new MarcService();

    private String fixture() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/marc/loc-sru-sample.xml")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesMarcRecordsOutOfTheSruEnvelope() throws Exception {
        String sru = fixture();
        LocSruClient client = new LocSruClient(marcService, "http://example/loc", uri -> sru);

        List<MarcData> results = client.searchByIsbn("978-0-13-468599-1");

        assertEquals(1, results.size());
        MarcData data = results.get(0);
        assertEquals("Effective Java", data.book().getTitle());
        assertEquals("9780134685991", data.book().getIsbn());
        assertEquals("Addison-Wesley", data.publisherName());
        assertTrue(data.authorNames().contains("Bloch, Joshua."));
    }

    @Test
    void buildsAnSruQueryUri() {
        LocSruClient client = new LocSruClient(marcService, "http://example/loc", uri -> "");

        URI uri = client.buildUri("bath.isbn=9780134685991");

        assertTrue(uri.toString().contains("recordSchema=marcxml"), uri.toString());
        assertTrue(uri.toString().contains("9780134685991"), uri.toString());
    }
}
