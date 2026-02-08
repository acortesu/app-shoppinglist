package com.appcompras.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientCustomRepository extends JpaRepository<IngredientCustomEntity, String> {

    Optional<IngredientCustomEntity> findByIdAndUserId(String id, String userId);

    Optional<IngredientCustomEntity> findByUserIdAndNormalizedName(String userId, String normalizedName);

    List<IngredientCustomEntity> findAllByUserId(String userId);
}
