package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.FineStatus;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Fine;
import com.justin.libradesk.domain.model.Patron;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcFineRepositoryIT extends AbstractRepositoryIT {

    private JdbcFineRepository repository;
    private Long patronId;

    @BeforeEach
    void setUp() {
        repository = new JdbcFineRepository(databaseManager);
        patronId = new JdbcPatronRepository(databaseManager)
                .save(new Patron(null, "M1", "Pat", null, null,
                        PatronType.STUDENT, PatronStatus.ACTIVE, FIXED)).getId();
    }

    private Fine unpaid(String amount) {
        return new Fine(null, patronId, null, new BigDecimal(amount), FineStatus.UNPAID, FIXED, null);
    }

    @Test
    void savesAndReadsBackWithNullLoan() {
        Fine saved = repository.save(unpaid("2.50"));

        Fine found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(0, found.getAmount().compareTo(new BigDecimal("2.50")));
        assertEquals(FineStatus.UNPAID, found.getStatus());
    }

    @Test
    void unpaidTotalSumsOnlyUnpaidFines() {
        repository.save(unpaid("2.00"));
        Fine toPay = repository.save(unpaid("3.00"));
        toPay.setStatus(FineStatus.PAID);
        toPay.setSettledAt(FIXED);
        repository.save(toPay);

        assertEquals(0, repository.unpaidTotal(patronId).compareTo(new BigDecimal("2.00")));
        assertEquals(1, repository.findUnpaid().size());
    }
}
