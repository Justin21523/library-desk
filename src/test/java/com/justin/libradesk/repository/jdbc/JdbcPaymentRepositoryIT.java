package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.FeeType;
import com.justin.libradesk.domain.enumtype.FineStatus;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Fine;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcPaymentRepositoryIT extends AbstractRepositoryIT {

    private JdbcFineRepository fineRepository;
    private JdbcPaymentRepository paymentRepository;
    private Long patronId;

    @BeforeEach
    void setUp() {
        fineRepository = new JdbcFineRepository(databaseManager);
        paymentRepository = new JdbcPaymentRepository(databaseManager);
        patronId = new JdbcPatronRepository(databaseManager)
                .save(new Patron(null, "M1", "Payer", null, null,
                        PatronType.PUBLIC, PatronStatus.ACTIVE, FIXED)).getId();
    }

    private Long lostItemFine() {
        Fine fine = new Fine(null, patronId, null, new BigDecimal("25.00"), FineStatus.UNPAID, FIXED, null);
        fine.setFeeType(FeeType.LOST_ITEM);
        fine.setPaidAmount(new BigDecimal("5.00"));
        fine.setNote("replacement");
        return fineRepository.save(fine).getId();
    }

    @Test
    void fineFeeTypePaidAmountAndNoteRoundTrip() {
        Fine read = fineRepository.findById(lostItemFine()).orElseThrow();

        assertEquals(FeeType.LOST_ITEM, read.getFeeType());
        assertEquals(0, read.getPaidAmount().compareTo(new BigDecimal("5.00")));
        assertEquals("replacement", read.getNote());
    }

    @Test
    void paymentsAreListedByFineAndByPatron() {
        Long fineId = lostItemFine();
        paymentRepository.save(new Payment(null, fineId, new BigDecimal("5.00"), "CASH", null, FIXED, "admin"));
        paymentRepository.save(new Payment(null, fineId, new BigDecimal("3.00"), "CARD", null, FIXED, "admin"));

        assertEquals(2, paymentRepository.findByFine(fineId).size());
        assertEquals(2, paymentRepository.findByPatron(patronId).size());
    }
}
