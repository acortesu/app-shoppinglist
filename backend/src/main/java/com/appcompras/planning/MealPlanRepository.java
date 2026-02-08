package com.appcompras.planning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlanEntity, String> {

    Optional<MealPlanEntity> findByIdAndUserId(String id, String userId);

    boolean existsByIdAndUserId(String id, String userId);

    List<MealPlanEntity> findAllByUserIdOrderByCreatedAtDescIdAsc(String userId);
}
