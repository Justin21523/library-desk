package com.justin.libradesk.dto;

import java.util.List;
import java.util.Map;

/**
 * Result of an OPAC search: the matching records plus facet value→count maps
 * computed over the matched set (for narrowing by author, subject, year,
 * language, or material type).
 */
public record CatalogSearchResult(List<CatalogRecord> records,
                                  Map<String, Long> authorFacet,
                                  Map<String, Long> subjectFacet,
                                  Map<String, Long> yearFacet,
                                  Map<String, Long> languageFacet,
                                  Map<String, Long> materialFacet) {
}
