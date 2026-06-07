package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAuthorityRepositoryIT extends AbstractRepositoryIT {

    private JdbcAuthorityRepository repository;
    private long authorId;
    private long subjectId;

    @BeforeEach
    void setUp() {
        repository = new JdbcAuthorityRepository(databaseManager);
        authorId = new JdbcAuthorRepository(databaseManager).save(new Author(null, "Twain, Mark")).id();
        subjectId = new JdbcSubjectRepository(databaseManager).save(new Subject(null, "Humor")).id();
    }

    @Test
    void authorVariantRoundTripsAndResolves() {
        repository.addAuthorVariant(authorId, "Clemens, Samuel");

        assertEquals(List.of("Clemens, Samuel"), repository.listAuthorVariants(authorId));
        // Case-insensitive resolution back to the authorized author.
        assertEquals(authorId, repository.findAuthorIdByVariant("clemens, samuel").orElseThrow());
        assertTrue(repository.findAuthorIdByVariant("unknown").isEmpty());
    }

    @Test
    void subjectVariantResolves() {
        repository.addSubjectVariant(subjectId, "Comedy");

        assertEquals(subjectId, repository.findSubjectIdByVariant("Comedy").orElseThrow());
    }
}
