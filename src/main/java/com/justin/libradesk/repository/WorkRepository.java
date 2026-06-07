package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Work;

import java.util.Optional;

public interface WorkRepository extends Repository<Work, Long> {

    /** @return the work with the given normalized key, if it exists. */
    Optional<Work> findByKey(String workKey);
}
