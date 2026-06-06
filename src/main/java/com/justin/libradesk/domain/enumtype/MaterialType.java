package com.justin.libradesk.domain.enumtype;

/**
 * The form of a bibliographic resource (a simplified view of the MARC leader /
 * 33x fields). Defaults to {@link #BOOK} for manually entered records.
 */
public enum MaterialType {
    BOOK,
    EBOOK,
    SERIAL,
    AUDIOVISUAL,
    OTHER
}
