package com.appcompras.shopping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShoppingListDraftRepository extends JpaRepository<ShoppingListDraftEntity, String> {

    Optional<ShoppingListDraftEntity> findByIdAndUserId(String id, String userId);

    boolean existsByIdAndUserId(String id, String userId);

    List<ShoppingListDraftEntity> findAllByUserIdOrderByCreatedAtDescIdAsc(String userId);

    Optional<ShoppingListDraftEntity> findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc(
            String userId,
            String planId,
            String idempotencyKey
    );
}
