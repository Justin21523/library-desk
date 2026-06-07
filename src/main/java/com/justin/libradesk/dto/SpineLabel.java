package com.justin.libradesk.dto;

/** One spine/pocket label: the call number, the copy barcode, and the title. */
public record SpineLabel(String callNumber, String barcode, String title) {
}
