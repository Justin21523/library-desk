package com.justin.libradesk.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helpers for the HTTP handlers: a configured Jackson mapper, response
 * writers (JSON/XML/text), and query-string parsing. Kept dependency-free
 * beyond the JDK server and the existing Jackson.
 */
final class WebSupport {

    static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private WebSupport() {
    }

    static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        send(exchange, status, "application/json; charset=utf-8", bytes);
    }

    static void sendXml(HttpExchange exchange, int status, String xml) throws IOException {
        send(exchange, status, "text/xml; charset=utf-8", xml.getBytes(StandardCharsets.UTF_8));
    }

    static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        send(exchange, status, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8));
    }

    static void send(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /** Parses the request's query string into a decoded key→value map. */
    static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) {
            return params;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                params.put(decode(pair), "");
            } else {
                params.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return params;
    }

    static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    /** Escapes the five XML predefined entities. */
    static String xml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static final Pattern RECORD =
            Pattern.compile("<(\\w+:)?record\\b[^>]*>.*?</(\\w+:)?record>", Pattern.DOTALL);

    /**
     * Extracts the standalone {@code <record>…</record>} element from a marc4j
     * MARCXML document (which wraps records in a {@code <collection>} and uses a
     * {@code marc:} prefix), re-declaring the MARC21 slim namespace on the record
     * so it is self-contained when embedded in an OAI-PMH or SRU response.
     */
    static String marcRecordElement(String marcXml) {
        Matcher matcher = RECORD.matcher(marcXml);
        if (!matcher.find()) {
            return "";
        }
        String record = matcher.group();
        String prefix = matcher.group(1); // e.g. "marc:" or null
        if (prefix != null) {
            String name = prefix.substring(0, prefix.length() - 1); // "marc"
            if (!record.contains("xmlns:" + name)) {
                record = record.replaceFirst("<" + prefix + "record",
                        "<" + prefix + "record xmlns:" + name + "=\"http://www.loc.gov/MARC21/slim\"");
            }
        } else if (!record.contains("xmlns")) {
            record = record.replaceFirst("<record",
                    "<record xmlns=\"http://www.loc.gov/MARC21/slim\"");
        }
        return record;
    }
}
