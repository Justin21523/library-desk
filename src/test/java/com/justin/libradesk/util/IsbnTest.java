package com.justin.libradesk.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsbnTest {

    @Test
    void validatesIsbn13() {
        assertTrue(Isbn.isValid("978-0-13-468599-1"));   // Effective Java 3rd
        assertFalse(Isbn.isValid("978-0-13-468599-2"));  // bad check digit
    }

    @Test
    void validatesIsbn10IncludingCheckX() {
        assertTrue(Isbn.isValid("0-201-61622-X"));       // check digit X
        assertFalse(Isbn.isValid("0-201-61622-5"));
    }

    @Test
    void normalizeStripsSeparators() {
        assertEquals("9780134685991", Isbn.normalize("978-0 13 468599 1"));
    }

    @Test
    void convertsIsbn10To13() {
        assertEquals("9780201616224", Isbn.to13("0-201-61622-X"));
        assertTrue(Isbn.isValid(Isbn.to13("0-201-61622-X")));
    }
}
