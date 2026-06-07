package com.justin.libradesk.infrastructure.web;

/**
 * Seam for checking an 856 e-resource link. The default implementation issues a
 * real HTTP request; tests supply a fake so they run offline. Mirrors the
 * {@code HttpFetcher} pattern used by {@code LocSruClient}.
 */
public interface LinkChecker {

    /**
     * @param url the link to check
     * @return the HTTP status code, or {@code 0} if the request could not be made
     */
    int check(String url);
}
