package com.justin.libradesk.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * UI string lookup. Holds the active {@link ResourceBundle} (English default,
 * {@code zh-TW} available) so FXML {@code %key} references and code share one
 * source of translated strings. The locale is set once at startup from the
 * {@code ui.locale} setting via {@link #setLocale(String)}.
 */
public final class Messages {

    private static final String BASE = "i18n.messages";

    private static volatile ResourceBundle bundle =
            ResourceBundle.getBundle(BASE, Locale.ENGLISH);

    private Messages() {
    }

    /** Sets the active locale from a tag such as {@code en} or {@code zh-TW}. */
    public static void setLocale(String tag) {
        Locale locale = (tag == null || tag.isBlank())
                ? Locale.ENGLISH : Locale.forLanguageTag(tag.trim());
        bundle = ResourceBundle.getBundle(BASE, locale);
    }

    /** @return the active resource bundle (for {@code FXMLLoader.setResources}). */
    public static ResourceBundle bundle() {
        return bundle;
    }

    /** @return the translated string for a key, or the key itself if absent. */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
