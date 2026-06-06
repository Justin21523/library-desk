package com.justin.libradesk.domain.model;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;

import java.time.LocalDateTime;

/**
 * A library member who can borrow copies and place reservations.
 */
public class Patron {

    private Long id;
    private String membershipNo;
    private String fullName;
    private String email;
    private String phone;
    private PatronType patronType;
    private PatronStatus status;
    private LocalDateTime createdAt;

    public Patron() {
    }

    public Patron(Long id, String membershipNo, String fullName, String email, String phone,
                  PatronType patronType, PatronStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.membershipNo = membershipNo;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.patronType = patronType;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** @return {@code true} when this patron is allowed to borrow at all. */
    public boolean canBorrow() {
        return status == PatronStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMembershipNo() {
        return membershipNo;
    }

    public void setMembershipNo(String membershipNo) {
        this.membershipNo = membershipNo;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public PatronType getPatronType() {
        return patronType;
    }

    public void setPatronType(PatronType patronType) {
        this.patronType = patronType;
    }

    public PatronStatus getStatus() {
        return status;
    }

    public void setStatus(PatronStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
