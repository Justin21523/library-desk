package com.justin.libradesk.infrastructure.web;

import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.service.CatalogService;
import com.justin.libradesk.domain.service.PatronService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only JSON REST API under {@code /api}: books (+ copies), items by
 * barcode, and patrons by membership number. Maps domain objects to plain maps
 * serialized by Jackson.
 */
class RestHandler implements HttpHandler {

    private final CatalogService catalogService;
    private final PatronService patronService;

    RestHandler(CatalogService catalogService, PatronService patronService) {
        this.catalogService = catalogService;
        this.patronService = patronService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                WebSupport.sendJson(exchange, 405, Map.of("error", "method not allowed"));
                return;
            }
            String[] parts = exchange.getRequestURI().getPath().replaceAll("^/+|/+$", "").split("/");
            // parts[0] == "api"
            if (parts.length == 2 && parts[1].equals("books")) {
                listBooks(exchange);
            } else if (parts.length == 3 && parts[1].equals("books")) {
                bookDetail(exchange, parseId(parts[2]));
            } else if (parts.length == 3 && parts[1].equals("items")) {
                item(exchange, parts[2]);
            } else if (parts.length == 3 && parts[1].equals("patrons")) {
                patron(exchange, parts[2]);
            } else {
                WebSupport.sendJson(exchange, 404, Map.of("error", "not found"));
            }
        } catch (NumberFormatException e) {
            WebSupport.sendJson(exchange, 400, Map.of("error", "invalid id"));
        } catch (RuntimeException e) {
            WebSupport.sendJson(exchange, 500, Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    private void listBooks(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> books = catalogService.listBooks().stream()
                .map(RestHandler::bookSummary).toList();
        WebSupport.sendJson(exchange, 200, books);
    }

    private void bookDetail(HttpExchange exchange, long id) throws IOException {
        Book book = catalogService.getBook(id).orElse(null);
        if (book == null) {
            WebSupport.sendJson(exchange, 404, Map.of("error", "book not found"));
            return;
        }
        Map<String, Object> body = bookSummary(book);
        body.put("copies", catalogService.listCopies(id).stream().map(RestHandler::copy).toList());
        WebSupport.sendJson(exchange, 200, body);
    }

    private void item(HttpExchange exchange, String barcode) throws IOException {
        BookCopy found = catalogService.findCopyByBarcode(barcode).orElse(null);
        if (found == null) {
            WebSupport.sendJson(exchange, 404, Map.of("error", "item not found"));
            return;
        }
        WebSupport.sendJson(exchange, 200, copy(found));
    }

    private void patron(HttpExchange exchange, String membershipNo) throws IOException {
        Patron found = patronService.findByMembershipNo(membershipNo).orElse(null);
        if (found == null) {
            WebSupport.sendJson(exchange, 404, Map.of("error", "patron not found"));
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("membershipNo", found.getMembershipNo());
        body.put("fullName", found.getFullName());
        body.put("type", found.getPatronType().name());
        body.put("status", found.getStatus().name());
        WebSupport.sendJson(exchange, 200, body);
    }

    private static Map<String, Object> bookSummary(Book book) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", book.getId());
        map.put("isbn", book.getIsbn());
        map.put("title", book.getTitle());
        map.put("year", book.getPublishedYear());
        map.put("materialType", book.getMaterialType() == null ? null : book.getMaterialType().name());
        map.put("recordStatus", book.getRecordStatus() == null ? null : book.getRecordStatus().name());
        return map;
    }

    private static Map<String, Object> copy(BookCopy copy) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("barcode", copy.getBarcode());
        map.put("status", copy.getStatus().name());
        map.put("bookId", copy.getBookId());
        return map;
    }

    private static long parseId(String value) {
        return Long.parseLong(value);
    }
}
