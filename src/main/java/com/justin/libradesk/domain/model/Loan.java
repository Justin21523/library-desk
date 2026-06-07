package com.justin.libradesk.domain.model;

import com.justin.libradesk.domain.enumtype.LoanStatus;

import java.time.LocalDateTime;

/**
 * A borrowing of a single {@link BookCopy} by a {@link Patron}.
 */
public class Loan {

    private Long id;
    private Long copyId;
    private Long patronId;
    private LocalDateTime loanedAt;
    private LocalDateTime dueAt;
    private LocalDateTime returnedAt;
    private LoanStatus status;
    private int renewalCount;

    public Loan() {
    }

    public Loan(Long id, Long copyId, Long patronId, LocalDateTime loanedAt, LocalDateTime dueAt,
                LocalDateTime returnedAt, LoanStatus status) {
        this.id = id;
        this.copyId = copyId;
        this.patronId = patronId;
        this.loanedAt = loanedAt;
        this.dueAt = dueAt;
        this.returnedAt = returnedAt;
        this.status = status;
    }

    /**
     * @param now the reference instant to compare against
     * @return {@code true} when the loan is unreturned and past its due date
     */
    public boolean isOverdue(LocalDateTime now) {
        return returnedAt == null && now.isAfter(dueAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCopyId() {
        return copyId;
    }

    public void setCopyId(Long copyId) {
        this.copyId = copyId;
    }

    public Long getPatronId() {
        return patronId;
    }

    public void setPatronId(Long patronId) {
        this.patronId = patronId;
    }

    public LocalDateTime getLoanedAt() {
        return loanedAt;
    }

    public void setLoanedAt(LocalDateTime loanedAt) {
        this.loanedAt = loanedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    /** @return how many times this loan has been renewed (checked against the policy limit). */
    public int getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(int renewalCount) {
        this.renewalCount = renewalCount;
    }
}
