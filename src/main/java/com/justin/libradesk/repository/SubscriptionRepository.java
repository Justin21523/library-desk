package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Subscription;

import java.util.List;

public interface SubscriptionRepository extends Repository<Subscription, Long> {

    /** @return all ACTIVE subscriptions (used by issue prediction and the claim job). */
    List<Subscription> findActive();
}
