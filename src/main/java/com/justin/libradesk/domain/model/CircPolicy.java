package com.justin.libradesk.domain.model;

import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.enumtype.PatronType;

import java.math.BigDecimal;

/**
 * One row of the circulation policy matrix: the loan rules that apply to a
 * patron type and (optionally) a material type. A {@code materialType} of
 * {@code null} is the default row for the patron type; an exact match overrides
 * it. {@code id} is {@code null} until persisted.
 *
 * @param fineCap maximum overdue fine per loan ({@code 0} = no cap)
 */
public record CircPolicy(Long id, PatronType patronType, MaterialType materialType,
                         int loanDays, int maxLoans, int renewalLimit, int maxHolds,
                         BigDecimal finePerDay, BigDecimal fineCap, int graceDays) {
}
