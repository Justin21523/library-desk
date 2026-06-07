package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.CircPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcCircPolicyRepositoryIT extends AbstractRepositoryIT {

    private JdbcCircPolicyRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcCircPolicyRepository(databaseManager);
    }

    private CircPolicy policy(PatronType type, MaterialType material, int loanDays) {
        return new CircPolicy(null, type, material, loanDays, 5, 2, 5,
                new BigDecimal("0.50"), new BigDecimal("20.00"), 0);
    }

    @Test
    void findForPrefersExactMatchOverDefault() {
        repository.save(policy(PatronType.STUDENT, null, 14));            // default row
        repository.save(policy(PatronType.STUDENT, MaterialType.EBOOK, 7)); // exact row

        assertEquals(7, repository.findFor(PatronType.STUDENT, MaterialType.EBOOK).orElseThrow().loanDays());
    }

    @Test
    void findForFallsBackToDefaultRow() {
        repository.save(policy(PatronType.STUDENT, null, 14));

        assertEquals(14, repository.findFor(PatronType.STUDENT, MaterialType.BOOK).orElseThrow().loanDays());
    }

    @Test
    void findForReturnsEmptyWhenNoRow() {
        assertTrue(repository.findFor(PatronType.STAFF, MaterialType.BOOK).isEmpty());
    }

    @Test
    void savesUpdatesAndDeletes() {
        CircPolicy saved = repository.save(policy(PatronType.PUBLIC, null, 14));
        assertTrue(saved.id() != null);

        repository.save(new CircPolicy(saved.id(), PatronType.PUBLIC, null, 21, 5, 2, 5,
                new BigDecimal("0.50"), new BigDecimal("20.00"), 0));
        assertEquals(21, repository.findById(saved.id()).orElseThrow().loanDays());

        repository.deleteById(saved.id());
        assertTrue(repository.findById(saved.id()).isEmpty());
    }
}
