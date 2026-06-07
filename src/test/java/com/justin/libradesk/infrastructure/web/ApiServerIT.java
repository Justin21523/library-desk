package com.justin.libradesk.infrastructure.web;

import com.justin.libradesk.config.AppConfig;
import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.repository.jdbc.AbstractRepositoryIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the interoperability server end-to-end: it starts on an ephemeral
 * port over the Testcontainers database and is queried with the JDK HttpClient,
 * asserting JSON (REST), and MARCXML (OAI-PMH and SRU) responses.
 */
class ApiServerIT extends AbstractRepositoryIT {

    private final HttpClient http = HttpClient.newHttpClient();
    private int port;

    @BeforeEach
    void startServer() {
        if (AppContext.tryGet() == null) {
            AppContext.initialize(AppConfig.load(), databaseManager);
        }
        port = AppContext.get().apiServer().start(0);
        Book book = new Book(null, "978-0001", "Clean Code", null, null, 2008, LocalDateTime.now());
        AppContext.get().catalogService().addBook(book, "test");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void restReturnsBooksAsJson() throws Exception {
        HttpResponse<String> response = get("/api/books");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Clean Code"), response.body());
    }

    @Test
    void oaiListRecordsReturnsMarcXml() throws Exception {
        HttpResponse<String> response = get("/oai?verb=ListRecords&metadataPrefix=marcxml");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<ListRecords>"), response.body());
        assertTrue(response.body().contains("http://www.loc.gov/MARC21/slim"), response.body());
        assertTrue(response.body().contains("Clean Code"), response.body());
    }

    @Test
    void oaiIdentifyReturnsRepositoryName() throws Exception {
        HttpResponse<String> response = get("/oai?verb=Identify");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<repositoryName>LibraDesk</repositoryName>"), response.body());
    }

    @Test
    void sruSearchRetrieveReturnsMarcXmlRecords() throws Exception {
        HttpResponse<String> response = get("/sru?operation=searchRetrieve&query=Clean");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<searchRetrieveResponse"), response.body());
        assertTrue(response.body().contains("<numberOfRecords>1</numberOfRecords>"), response.body());
        assertTrue(response.body().contains("Clean Code"), response.body());
    }
}
