package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.ShelfLocation;

import java.util.List;

public interface LocationRepository extends Repository<ShelfLocation, Long> {

    /** @return the shelving locations belonging to a branch, by name. */
    List<ShelfLocation> findByBranch(Long branchId);
}
