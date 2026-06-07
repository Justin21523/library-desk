package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.ELink;

import java.util.List;

public interface ELinkRepository extends Repository<ELink, Long> {

    List<ELink> findByBook(Long bookId);
}
