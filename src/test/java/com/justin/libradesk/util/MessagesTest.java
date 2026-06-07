package com.justin.libradesk.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesTest {

    @AfterEach
    void resetLocale() {
        Messages.setLocale("en");
    }

    @Test
    void defaultBundleIsEnglish() {
        Messages.setLocale("en");
        assertEquals("Dashboard", Messages.get("nav.dashboard"));
    }

    @Test
    void traditionalChineseBundleIsAvailable() {
        Messages.setLocale("zh-TW");
        assertEquals("儀表板", Messages.get("nav.dashboard"));
    }

    @Test
    void missingKeyReturnsTheKey() {
        Messages.setLocale("en");
        assertEquals("no.such.key", Messages.get("no.such.key"));
    }
}
