package com.justin.libradesk.infrastructure.web;

import com.justin.libradesk.domain.service.CatalogService;
import com.justin.libradesk.domain.service.CatalogSearchService;
import com.justin.libradesk.dto.CatalogRecord;
import com.justin.libradesk.infrastructure.marc.MarcService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Minimal SRU 1.2 server under {@code /sru}. {@code operation=searchRetrieve}
 * runs the OPAC keyword search and returns matching bibs as MARCXML records;
 * {@code operation=explain} (the default) returns a short explain document. The
 * CQL query is treated as a keyword — full CQL parsing is out of scope.
 */
class SruHandler implements HttpHandler {

    private static final String NS = "http://www.loc.gov/zing/srw/";
    private static final int DEFAULT_MAX = 10;

    private final CatalogSearchService searchService;
    private final CatalogService catalogService;
    private final MarcService marcService;

    SruHandler(CatalogSearchService searchService, CatalogService catalogService, MarcService marcService) {
        this.searchService = searchService;
        this.catalogService = catalogService;
        this.marcService = marcService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> q = WebSupport.query(exchange);
        String operation = q.getOrDefault("operation", "explain");
        if (operation.equals("searchRetrieve")) {
            WebSupport.sendXml(exchange, 200, searchRetrieve(q));
        } else {
            WebSupport.sendXml(exchange, 200, explain());
        }
    }

    private String searchRetrieve(Map<String, String> q) {
        String query = q.getOrDefault("query", "");
        int max = parseMax(q.get("maximumRecords"));
        List<CatalogRecord> records = searchService.search(query).records();
        int total = records.size();
        List<CatalogRecord> page = records.stream().limit(max).toList();

        StringBuilder b = new StringBuilder();
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<searchRetrieveResponse xmlns=\"").append(NS).append("\">")
                .append("<version>1.2</version>")
                .append("<numberOfRecords>").append(total).append("</numberOfRecords>")
                .append("<records>");
        int position = 1;
        for (CatalogRecord record : page) {
            String marc = WebSupport.marcRecordElement(
                    marcService.toXmlString(catalogService.toMarcData(record.book())));
            b.append("<record>")
                    .append("<recordSchema>info:srw/schema/1/marcxml-v1.1</recordSchema>")
                    .append("<recordPacking>xml</recordPacking>")
                    .append("<recordData>").append(marc).append("</recordData>")
                    .append("<recordPosition>").append(position++).append("</recordPosition>")
                    .append("</record>");
        }
        b.append("</records></searchRetrieveResponse>");
        return b.toString();
    }

    private String explain() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<explainResponse xmlns=\"" + NS + "\"><version>1.2</version><record>"
                + "<recordSchema>http://explain.z3950.org/dtd/2.0/</recordSchema>"
                + "<recordPacking>xml</recordPacking>"
                + "<recordData><explain><serverInfo><host>localhost</host></serverInfo>"
                + "<databaseInfo><title>LibraDesk SRU</title></databaseInfo>"
                + "</explain></recordData></record></explainResponse>";
    }

    private int parseMax(String value) {
        if (value == null) {
            return DEFAULT_MAX;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return DEFAULT_MAX;
        }
    }
}
