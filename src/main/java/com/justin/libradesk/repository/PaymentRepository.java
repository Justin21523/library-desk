package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Payment;

import java.util.List;

public interface PaymentRepository {

    Payment save(Payment payment);

    List<Payment> findByFine(Long fineId);

    /** @return all payments made against a patron's fines, most recent first. */
    List<Payment> findByPatron(Long patronId);
}
