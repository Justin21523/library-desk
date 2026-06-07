package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Holding;

import java.util.List;

public interface HoldingRepository extends Repository<Holding, Long> {

    List<Holding> findByBook(Long bookId);
}
