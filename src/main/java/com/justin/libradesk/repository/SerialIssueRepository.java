package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.SerialIssue;

import java.time.LocalDate;
import java.util.List;

public interface SerialIssueRepository extends Repository<SerialIssue, Long> {

    List<SerialIssue> findBySubscription(Long subscriptionId);

    /** @return EXPECTED issues whose expected date is before the cutoff (claim candidates). */
    List<SerialIssue> findExpectedBefore(LocalDate cutoff);
}
