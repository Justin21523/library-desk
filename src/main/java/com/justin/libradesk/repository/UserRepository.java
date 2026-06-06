package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.User;

import java.util.Optional;

public interface UserRepository extends Repository<User, Long> {

    /** Looks up a staff account by its unique username (used for login). */
    Optional<User> findByUsername(String username);
}
