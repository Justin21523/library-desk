package com.justin.libradesk.util;

import com.justin.libradesk.domain.enumtype.ClassificationScheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CallNumberTest {

    @Test
    void deweyNumbersSortIntoShelfOrder() {
        List<String> input = List.of("100", "20", "5.13", "5.1");

        List<String> sorted = input.stream()
                .sorted((a, b) -> key(ClassificationScheme.DDC, a).compareTo(key(ClassificationScheme.DDC, b)))
                .toList();

        assertEquals(List.of("5.1", "5.13", "20", "100"), sorted);
    }

    @Test
    void lccNumbersSortByClassThenNumber() {
        List<String> input = List.of("QB1", "QA76.73", "QA76", "Z1");

        List<String> sorted = input.stream()
                .sorted((a, b) -> key(ClassificationScheme.LCC, a).compareTo(key(ClassificationScheme.LCC, b)))
                .toList();

        assertEquals(List.of("QA76", "QA76.73", "QB1", "Z1"), sorted);
    }

    @Test
    void blankCallNumberYieldsEmptyKey() {
        assertEquals("", CallNumber.shelfKey(ClassificationScheme.DDC, null));
        assertEquals("", CallNumber.shelfKey(ClassificationScheme.LCC, "  "));
    }

    private static String key(ClassificationScheme scheme, String callNumber) {
        return CallNumber.shelfKey(scheme, callNumber);
    }
}
