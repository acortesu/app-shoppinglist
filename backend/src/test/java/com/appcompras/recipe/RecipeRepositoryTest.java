package com.appcompras.recipe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecipeRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @BeforeEach
    void clean() {
        recipeRepository.deleteAll();
    }

    @Test
    void saveAndLoadRecipeWithIngredientsAndTags() {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId("r-1");
        recipe.setUserId("local-dev-user");
        recipe.setName("Arroz con tomate");
        recipe.setType(MealType.LUNCH);
        recipe.setIngredients(List.of(
                new RecipeIngredientEmbeddable("rice", 1.0, Unit.CUP),
                new RecipeIngredientEmbeddable("tomato", 2.0, Unit.PIECE)
        ));
        recipe.setPreparation("Mezclar y cocinar");
        recipe.setNotes("MVP");
        recipe.setTags(Set.of("rapido"));
        recipe.setUsageCount(0);
        recipe.setCreatedAt(Instant.parse("2026-02-07T12:00:00Z"));
        recipe.setUpdatedAt(Instant.parse("2026-02-07T12:00:00Z"));

        recipeRepository.save(recipe);

        RecipeEntity loaded = recipeRepository.findById("r-1").orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Arroz con tomate");
        assertThat(loaded.getIngredients()).hasSize(2);
        assertThat(loaded.getIngredients().get(0).getIngredientId()).isEqualTo("rice");
        assertThat(loaded.getIngredients().get(1).getIngredientId()).isEqualTo("tomato");
        assertThat(loaded.getTags()).containsExactly("rapido");
    }

    @Test
    void findAllByTypeKeepsCreatedAtDescOrder() {
        RecipeEntity olderBreakfast = buildRecipe("r-1", "Desayuno 1", MealType.BREAKFAST, Instant.parse("2026-02-01T10:00:00Z"));
        RecipeEntity newerBreakfast = buildRecipe("r-2", "Desayuno 2", MealType.BREAKFAST, Instant.parse("2026-02-02T10:00:00Z"));
        RecipeEntity lunch = buildRecipe("r-3", "Almuerzo", MealType.LUNCH, Instant.parse("2026-02-03T10:00:00Z"));

        recipeRepository.saveAll(List.of(olderBreakfast, newerBreakfast, lunch));

        List<RecipeEntity> breakfasts = recipeRepository.findAllByUserIdAndTypeOrderByCreatedAtDescIdAsc(
                "local-dev-user", MealType.BREAKFAST);
        assertThat(breakfasts).extracting(RecipeEntity::getId).containsExactly("r-2", "r-1");
    }

    private RecipeEntity buildRecipe(String id, String name, MealType type, Instant createdAt) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setUserId("local-dev-user");
        recipe.setName(name);
        recipe.setType(type);
        recipe.setIngredients(List.of(new RecipeIngredientEmbeddable("rice", 1.0, Unit.CUP)));
        recipe.setTags(Set.of());
        recipe.setUsageCount(0);
        recipe.setCreatedAt(createdAt);
        recipe.setUpdatedAt(createdAt);
        return recipe;
    }
}
