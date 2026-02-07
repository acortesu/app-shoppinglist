package com.appcompras.planning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealPlanRepository extends JpaRepository<MealPlanEntity, String> {

    List<MealPlanEntity> findAllByOrderByCreatedAtDescIdAsc();
}
