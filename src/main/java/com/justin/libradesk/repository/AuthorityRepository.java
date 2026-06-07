package com.justin.libradesk.repository;

import java.util.List;
import java.util.Optional;

/**
 * Stores see-from variant headings (authority cross-references) pointing at the
 * authorized author/subject records. A variant form resolves to the authorized
 * record's id so headings can be normalised during cataloging.
 */
public interface AuthorityRepository {

    void addAuthorVariant(long authorId, String variantForm);

    Optional<Long> findAuthorIdByVariant(String variantForm);

    List<String> listAuthorVariants(long authorId);

    void addSubjectVariant(long subjectId, String variantForm);

    Optional<Long> findSubjectIdByVariant(String variantForm);

    List<String> listSubjectVariants(long subjectId);

    /** Repoints all links/variants from one author onto another and deletes the source. */
    void mergeAuthor(long fromId, long intoId);

    /** Repoints all links/variants from one subject onto another and deletes the source. */
    void mergeSubject(long fromId, long intoId);
}
