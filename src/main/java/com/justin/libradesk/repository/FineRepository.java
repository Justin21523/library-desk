package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Fine;

import java.math.BigDecimal;
import java.util.List;

public interface FineRepository extends Repository<Fine, Long> {

    List<Fine> findByPatron(Long patronId);

    /** @return all UNPAID fines, most recent first. */
    List<Fine> findUnpaid();

    /** @return the sum of a patron's UNPAID fines (never null; zero if none). */
    BigDecimal unpaidTotal(Long patronId);
}
