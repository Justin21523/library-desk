package com.justin.libradesk.infrastructure.web;

import com.justin.libradesk.domain.service.CatalogSearchService;
import com.justin.libradesk.domain.service.CatalogService;
import com.justin.libradesk.domain.service.PatronService;
import com.justin.libradesk.infrastructure.marc.MarcService;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Read-only interoperability server built on the JDK {@link HttpServer}: a JSON
 * REST API ({@code /api}), an OAI-PMH provider ({@code /oai}), and an SRU server
 * ({@code /sru}). Created in the composition root and started only when
 * {@code api.server.enabled=true}; tests start it on an ephemeral port (0).
 */
public class ApiServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);

    private final RestHandler restHandler;
    private final OaiPmhHandler oaiHandler;
    private final SruHandler sruHandler;

    private HttpServer server;

    public ApiServer(CatalogService catalogService, CatalogSearchService searchService,
                     PatronService patronService, MarcService marcService) {
        this.restHandler = new RestHandler(catalogService, patronService);
        this.oaiHandler = new OaiPmhHandler(catalogService, marcService);
        this.sruHandler = new SruHandler(searchService, catalogService, marcService);
    }

    /**
     * Starts the server on the given port (0 = an ephemeral port).
     *
     * @return the bound port
     */
    public synchronized int start(int port) {
        if (server != null) {
            return port();
        }
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to start API server on port " + port, e);
        }
        server.createContext("/api", restHandler);
        server.createContext("/oai", oaiHandler);
        server.createContext("/sru", sruHandler);
        server.setExecutor(Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "libradesk-api");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        log.info("API server listening on port {} (/api, /oai, /sru)", port());
        return port();
    }

    public synchronized int port() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    @Override
    public synchronized void close() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
