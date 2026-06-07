package com.justin.libradesk.domain.enumtype;

/**
 * The reason a {@code Fine} (charge) was raised against a patron.
 */
public enum FeeType {
    /** Accrued for a late return. */
    OVERDUE,
    /** Replacement charge for a copy declared lost. */
    LOST_ITEM,
    /** Flat handling fee, typically added alongside a lost-item charge. */
    PROCESSING,
    /** Charge for a copy returned damaged. */
    DAMAGE
}
