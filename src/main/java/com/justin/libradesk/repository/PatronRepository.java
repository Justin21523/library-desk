package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Patron;

import java.util.Optional;

public interface PatronRepository extends Repository<Patron, Long> {

    Optional<Patron> findByMembershipNo(String membershipNo);
}
