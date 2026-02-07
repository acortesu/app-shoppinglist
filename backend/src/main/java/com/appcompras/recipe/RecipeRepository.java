package com.appcompras.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<RecipeEntity, String> {

    List<RecipeEntity> findAllByOrderByCreatedAtDescIdAsc();

    List<RecipeEntity> findAllByTypeOrderByCreatedAtDescIdAsc(MealType type);
}
