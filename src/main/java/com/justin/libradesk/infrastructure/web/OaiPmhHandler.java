package com.justin.libradesk.infrastructure.web;

import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.service.CatalogService;
import com.justin.libradesk.infrastructure.marc.MarcService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal OAI-PMH 2.0 provider under {@code /oai}. Supports Identify,
 * ListMetadataFormats, ListIdentifiers, ListRecords and GetRecord, disseminating
 * each bib as {@code marcxml} (reusing {@link MarcService}) or {@code oai_dc}.
 * Selective harvesting (sets, resumption tokens, date ranges) is out of scope.
 */
class OaiPmhHandler implements HttpHandler {

    private static final String NS = "http://www.openarchives.org/OAI/2.0/";
    private static final String ID_PREFIX = "oai:libradesk:";

    private final CatalogService catalogService;
    private final MarcService marcService;

    OaiPmhHandler(CatalogService catalogService, MarcService marcService) {
        this.catalogService = catalogService;
        this.marcService = marcService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> q = WebSupport.query(exchange);
        String verb = q.getOrDefault("verb", "");
        String prefix = q.getOrDefault("metadataPrefix", "marcxml");
        StringBuilder body = new StringBuilder();
        switch (verb) {
            case "Identify" -> identify(body);
            case "ListMetadataFormats" -> listMetadataFormats(body);
            case "ListIdentifiers" -> listRecords(body, prefix, true);
            case "ListRecords" -> listRecords(body, prefix, false);
            case "GetRecord" -> getRecord(body, q.get("identifier"), prefix);
            default -> error(body, "badVerb", "Illegal or missing verb");
        }
        WebSupport.sendXml(exchange, 200, envelope(verb, body.toString()));
    }

    private void identify(StringBuilder b) {
        b.append("<Identify>")
                .append("<repositoryName>LibraDesk</repositoryName>")
                .append("<baseURL>/oai</baseURL>")
                .append("<protocolVersion>2.0</protocolVersion>")
                .append("<adminEmail>admin@libradesk.local</adminEmail>")
                .append("<earliestDatestamp>1970-01-01</earliestDatestamp>")
                .append("<deletedRecord>no</deletedRecord>")
                .append("<granularity>YYYY-MM-DD</granularity>")
                .append("</Identify>");
    }

    private void listMetadataFormats(StringBuilder b) {
        b.append("<ListMetadataFormats>")
                .append("<metadataFormat><metadataPrefix>marcxml</metadataPrefix>")
                .append("<schema>http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd</schema>")
                .append("<metadataNamespace>http://www.loc.gov/MARC21/slim</metadataNamespace>")
                .append("</metadataFormat>")
                .append("<metadataFormat><metadataPrefix>oai_dc</metadataPrefix>")
                .append("<schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema>")
                .append("<metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/</metadataNamespace>")
                .append("</metadataFormat>")
                .append("</ListMetadataFormats>");
    }

    private void listRecords(StringBuilder b, String prefix, boolean identifiersOnly) {
        if (!isSupported(prefix)) {
            error(b, "cannotDisseminateFormat", "Unsupported metadataPrefix: " + prefix);
            return;
        }
        List<Book> books = catalogService.listBooks();
        if (books.isEmpty()) {
            error(b, "noRecordsMatch", "No records");
            return;
        }
        String wrapper = identifiersOnly ? "ListIdentifiers" : "ListRecords";
        b.append('<').append(wrapper).append('>');
        for (Book book : books) {
            if (identifiersOnly) {
                b.append(header(book));
            } else {
                b.append("<record>").append(header(book))
                        .append("<metadata>").append(metadata(book, prefix)).append("</metadata>")
                        .append("</record>");
            }
        }
        b.append("</").append(wrapper).append('>');
    }

    private void getRecord(StringBuilder b, String identifier, String prefix) {
        if (!isSupported(prefix)) {
            error(b, "cannotDisseminateFormat", "Unsupported metadataPrefix: " + prefix);
            return;
        }
        Optional<Book> book = parseId(identifier).flatMap(catalogService::getBook);
        if (book.isEmpty()) {
            error(b, "idDoesNotExist", "No record for identifier: " + identifier);
            return;
        }
        b.append("<GetRecord><record>").append(header(book.get()))
                .append("<metadata>").append(metadata(book.get(), prefix)).append("</metadata>")
                .append("</record></GetRecord>");
    }

    private String header(Book book) {
        String datestamp = book.getCreatedAt() != null
                ? book.getCreatedAt().toLocalDate().toString() : "1970-01-01";
        return "<header><identifier>" + ID_PREFIX + book.getId() + "</identifier>"
                + "<datestamp>" + datestamp + "</datestamp></header>";
    }

    private String metadata(Book book, String prefix) {
        if (prefix.equals("oai_dc")) {
            return dublinCore(book);
        }
        return WebSupport.marcRecordElement(marcService.toXmlString(catalogService.toMarcData(book)));
    }

    private String dublinCore(Book book) {
        StringBuilder dc = new StringBuilder();
        dc.append("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ")
                .append("xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        dc.append("<dc:title>").append(WebSupport.xml(book.getTitle())).append("</dc:title>");
        if (book.getIsbn() != null) {
            dc.append("<dc:identifier>").append(WebSupport.xml(book.getIsbn())).append("</dc:identifier>");
        }
        if (book.getPublishedYear() != null) {
            dc.append("<dc:date>").append(book.getPublishedYear()).append("</dc:date>");
        }
        if (book.getLanguage() != null) {
            dc.append("<dc:language>").append(WebSupport.xml(book.getLanguage())).append("</dc:language>");
        }
        dc.append("</oai_dc:dc>");
        return dc.toString();
    }

    private boolean isSupported(String prefix) {
        return prefix.equals("marcxml") || prefix.equals("oai_dc");
    }

    private Optional<Long> parseId(String identifier) {
        if (identifier == null || !identifier.startsWith(ID_PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(identifier.substring(ID_PREFIX.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void error(StringBuilder b, String code, String message) {
        b.append("<error code=\"").append(code).append("\">").append(WebSupport.xml(message)).append("</error>");
    }

    private String envelope(String verb, String body) {
        String responseDate = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<OAI-PMH xmlns=\"" + NS + "\">"
                + "<responseDate>" + responseDate + "</responseDate>"
                + "<request" + (verb.isBlank() ? "" : " verb=\"" + WebSupport.xml(verb) + "\"") + ">/oai</request>"
                + body
                + "</OAI-PMH>";
    }
}
