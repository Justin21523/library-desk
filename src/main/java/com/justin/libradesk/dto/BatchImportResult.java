package com.justin.libradesk.dto;

import java.util.List;

/**
 * Summary of a batch MARC import: how many records were imported, which were
 * skipped as duplicates, and which failed (with messages).
 */
public record BatchImportResult(int imported, List<String> duplicates, List<String> errors) {
}
