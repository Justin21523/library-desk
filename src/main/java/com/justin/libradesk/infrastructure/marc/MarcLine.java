package com.justin.libradesk.infrastructure.marc;

/**
 * One editable MARC field line (MarcEdit-style): a 3-char {@code tag}, a 2-char
 * {@code indicators} string (space or {@code _} = blank), and {@code content}.
 * For control fields (00x) content is the raw value; for data fields it is the
 * subfields written inline as {@code $aValue$bValue}.
 */
public record MarcLine(String tag, String indicators, String content) {
}
