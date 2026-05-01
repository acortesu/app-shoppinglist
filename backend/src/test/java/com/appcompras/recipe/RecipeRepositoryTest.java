package com.appcompras.recipe;

import com.appcompras.domain.Unit;
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

    @Test
    void findByIdAndUserIdEnforcesUserIdFiltering() {
        RecipeEntity recipeUserA = buildRecipe("r-a1", "Recipe A", MealType.LUNCH, Instant.now());
        recipeUserA.setUserId("user-a");
        RecipeEntity recipeUserB = buildRecipe("r-b1", "Recipe B", MealType.LUNCH, Instant.now());
        recipeUserB.setUserId("user-b");

        recipeRepository.saveAll(List.of(recipeUserA, recipeUserB));

        var foundByUserA = recipeRepository.findByIdAndUserId("r-a1", "user-a");
        var foundByUserB = recipeRepository.findByIdAndUserId("r-b1", "user-b");
        var notFound = recipeRepository.findByIdAndUserId("r-a1", "user-b");

        assertThat(foundByUserA).isPresent().get().extracting(RecipeEntity::getName).isEqualTo("Recipe A");
        assertThat(foundByUserB).isPresent().get().extracting(RecipeEntity::getName).isEqualTo("Recipe B");
        assertThat(notFound).isEmpty();
    }

    @Test
    void existsByIdAndUserIdEnforcesUserIdFiltering() {
        RecipeEntity recipeUserA = buildRecipe("r-a1", "Recipe A", MealType.LUNCH, Instant.now());
        recipeUserA.setUserId("user-a");
        recipeRepository.save(recipeUserA);

        assertThat(recipeRepository.existsByIdAndUserId("r-a1", "user-a")).isTrue();
        assertThat(recipeRepository.existsByIdAndUserId("r-a1", "user-b")).isFalse();
        assertThat(recipeRepository.existsByIdAndUserId("nonexistent", "user-a")).isFalse();
    }

    @Test
    void findAllByUserIdOrdersByCreatedAtDescThenIdAsc() {
        Instant same = Instant.parse("2026-02-01T10:00:00Z");

        RecipeEntity z = buildRecipe("recipe-z", "Z Recipe", MealType.LUNCH, same);
        z.setUserId("user-a");
        RecipeEntity m = buildRecipe("recipe-m", "M Recipe", MealType.LUNCH, same);
        m.setUserId("user-a");
        RecipeEntity older = buildRecipe("recipe-older", "Older", MealType.LUNCH, Instant.parse("2026-01-01T10:00:00Z"));
        older.setUserId("user-a");

        recipeRepository.saveAll(List.of(z, m, older));

        List<RecipeEntity> results = recipeRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a");

        assertThat(results)
                .extracting(RecipeEntity::getId)
                .containsExactly("recipe-m", "recipe-z", "recipe-older");
    }

    @Test
    void deletedUserRecipesAreNotAccessible() {
        RecipeEntity recipeUserA = buildRecipe("r-a1", "Recipe A", MealType.LUNCH, Instant.now());
        recipeUserA.setUserId("user-a");
        recipeRepository.save(recipeUserA);

        assertThat(recipeRepository.findByIdAndUserId("r-a1", "user-a")).isPresent();
        assertThat(recipeRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a")).hasSize(1);

        List<RecipeEntity> userARecipes = recipeRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a");
        recipeRepository.deleteAll(userARecipes);

        assertThat(recipeRepository.findByIdAndUserId("r-a1", "user-a")).isEmpty();
        assertThat(recipeRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a")).isEmpty();
    }

    @Test
    void caseInsensitiveIdOrderingInSecondarySort() {
        Instant same = Instant.parse("2026-02-01T10:00:00Z");

        RecipeEntity recipeA = buildRecipe("Recipe-A", "A Recipe", MealType.LUNCH, same);
        recipeA.setUserId("user-a");
        RecipeEntity recipeB = buildRecipe("Recipe-B", "B Recipe", MealType.LUNCH, same);
        recipeB.setUserId("user-a");
        RecipeEntity recipeC = buildRecipe("recipe-c", "C Recipe", MealType.LUNCH, same);
        recipeC.setUserId("user-a");

        recipeRepository.saveAll(List.of(recipeA, recipeB, recipeC));

        List<RecipeEntity> results = recipeRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a");

        assertThat(results)
                .extracting(RecipeEntity::getId)
                .containsExactly("Recipe-A", "Recipe-B", "recipe-c");
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
