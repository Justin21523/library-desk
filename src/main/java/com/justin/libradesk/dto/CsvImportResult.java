package com.justin.libradesk.dto;

import java.util.List;

/**
 * Summary of a CSV import: how many rows were saved, how many were skipped, and
 * the per-row error messages for the skipped ones.
 */
public record CsvImportResult(int imported, int skipped, List<String> errors) {
}
