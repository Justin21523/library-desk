package com.justin.libradesk.repository;

import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.CircPolicy;

import java.util.Optional;

public interface CircPolicyRepository extends Repository<CircPolicy, Long> {

    /**
     * Resolves the policy for a patron/material combination: an exact
     * (patronType, materialType) row if present, otherwise the patron type's
     * default row (material_type IS NULL).
     *
     * @return the matching policy, or empty if neither exists
     */
    Optional<CircPolicy> findFor(PatronType patronType, MaterialType materialType);
}
