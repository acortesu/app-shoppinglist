package com.appcompras.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<RecipeEntity, String> {

    Optional<RecipeEntity> findByIdAndUserId(String id, String userId);

    boolean existsByIdAndUserId(String id, String userId);

    List<RecipeEntity> findAllByUserIdOrderByCreatedAtDescIdAsc(String userId);

    List<RecipeEntity> findAllByUserIdAndTypeOrderByCreatedAtDescIdAsc(String userId, MealType type);
}
