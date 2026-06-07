package com.justin.libradesk.infrastructure.marc;

import com.justin.libradesk.util.Isbn;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight MARC21 bibliographic record validation for the editor. Returns
 * human-readable issues prefixed {@code ERROR}/{@code WARN}; callers block saving
 * on any ERROR and surface WARNs for the cataloguer to consider.
 */
public class MarcValidator {

    public List<String> validate(Record record) {
        List<String> issues = new ArrayList<>();

        if (record.getLeader() == null || record.getLeader().toString().length() != 24) {
            issues.add("ERROR: Leader must be exactly 24 characters");
        }
        if (record.getVariableField("245") == null) {
            issues.add("ERROR: 245 (title statement) is required");
        }
        if (record.getVariableField("008") == null) {
            issues.add("WARN: 008 (fixed-length data elements) is recommended");
        }

        for (VariableField field : record.getVariableFields()) {
            if (field instanceof DataField data) {
                if (data.getSubfields().isEmpty()) {
                    issues.add("ERROR: field " + data.getTag() + " has no subfields");
                }
                if (!isValidIndicator(data.getIndicator1()) || !isValidIndicator(data.getIndicator2())) {
                    issues.add("WARN: field " + data.getTag() + " has an unusual indicator");
                }
            }
        }

        for (VariableField field : record.getVariableFields("020")) {
            Subfield a = ((DataField) field).getSubfield('a');
            if (a != null && !a.getData().isBlank() && !Isbn.isValid(a.getData().split(" ")[0])) {
                issues.add("WARN: ISBN '" + a.getData() + "' fails its checksum");
            }
        }
        return issues;
    }

    public boolean hasErrors(List<String> issues) {
        return issues.stream().anyMatch(issue -> issue.startsWith("ERROR"));
    }

    private static boolean isValidIndicator(char indicator) {
        return indicator == ' ' || Character.isLetterOrDigit(indicator);
    }
}
