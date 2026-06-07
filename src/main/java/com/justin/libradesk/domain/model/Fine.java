package com.justin.libradesk.domain.model;

import com.justin.libradesk.domain.enumtype.FeeType;
import com.justin.libradesk.domain.enumtype.FineStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A monetary penalty raised against a {@link Patron}, usually for an overdue
 * return (the originating {@link Loan} is referenced by {@code loanId}).
 */
public class Fine {

    private Long id;
    private Long patronId;
    private Long loanId;
    private BigDecimal amount;
    private FineStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
    private FeeType feeType = FeeType.OVERDUE;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private String note;

    public Fine() {
    }

    public Fine(Long id, Long patronId, Long loanId, BigDecimal amount, FineStatus status,
                LocalDateTime createdAt, LocalDateTime settledAt) {
        this.id = id;
        this.patronId = patronId;
        this.loanId = loanId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.settledAt = settledAt;
    }

    /** @return the amount still owed on this fine ({@code amount - paidAmount}). */
    public BigDecimal balance() {
        return amount.subtract(paidAmount);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatronId() {
        return patronId;
    }

    public void setPatronId(Long patronId) {
        this.patronId = patronId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public FineStatus getStatus() {
        return status;
    }

    public void setStatus(FineStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
        this.feeType = feeType;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
