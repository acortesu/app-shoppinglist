package com.appcompras.service;

import com.appcompras.domain.MealType;
import com.appcompras.domain.Recipe;
import com.appcompras.domain.RecipeIngredient;
import com.appcompras.domain.ShoppingListItem;
import com.appcompras.domain.Unit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShoppingListServiceTest {

    private ShoppingListService shoppingListService;

    @BeforeEach
    void setUp() {
        IngredientCatalogService catalogService = new IngredientCatalogService(new ObjectMapper());
        UnitConversionService conversionService = new UnitConversionService(catalogService);
        shoppingListService = new ShoppingListService(catalogService, conversionService);
    }

    @Test
    void aggregatesIngredientsFromMultipleRecipes() {
        Recipe riceLunch = new Recipe(
                "r1",
                "Rice bowl",
                MealType.LUNCH,
                List.of(new RecipeIngredient("rice", 1, Unit.CUP)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        Recipe riceDinner = new Recipe(
                "r2",
                "Rice side",
                MealType.DINNER,
                List.of(new RecipeIngredient("rice", 200, Unit.GRAM)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(List.of(riceLunch, riceDinner));

        assertEquals(1, list.size());
        ShoppingListItem rice = list.get(0);
        assertEquals("rice", rice.ingredientId());
        assertEquals(380.0, rice.requiredBaseAmount(), 0.001);
        assertEquals(1, rice.suggestedPackages());
    }

    @Test
    void roundsUpSuggestedPackages() {
        Recipe oilRecipe = new Recipe(
                "r3",
                "Big salad",
                MealType.DINNER,
                List.of(new RecipeIngredient("oil", 600, Unit.MILLILITER)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(List.of(oilRecipe));

        assertEquals(1, list.size());
        ShoppingListItem oil = list.get(0);
        assertEquals(2, oil.suggestedPackages());
    }

    @Test
    void filtersOutToTasteIngredients() {
        Recipe seasonedDish = new Recipe(
                "r4",
                "Seasoned dish",
                MealType.DINNER,
                List.of(
                    new RecipeIngredient("rice", 1, Unit.CUP),
                    new RecipeIngredient("black-pepper", 1, Unit.TO_TASTE)
                ),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(List.of(seasonedDish));

        assertEquals(1, list.size());
        ShoppingListItem item = list.get(0);
        assertEquals("rice", item.ingredientId());
    }

    @Test
    void handlesZeroPackageBaseAmountGracefully() {
        Recipe recipe = new Recipe(
                "r5",
                "Test recipe",
                MealType.LUNCH,
                List.of(new RecipeIngredient("black-pepper", 1, Unit.TO_TASTE)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(List.of(recipe));

        assertEquals(0, list.size());
    }

    @Test
    void roundsAggregatedBaseAmountsToOneDecimal() {
        Recipe saltyDish1 = new Recipe(
                "r6a",
                "Salty dish 1",
                MealType.LUNCH,
                List.of(new RecipeIngredient("salt", 0.3, Unit.PINCH)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        Recipe saltyDish2 = new Recipe(
                "r6b",
                "Salty dish 2",
                MealType.LUNCH,
                List.of(new RecipeIngredient("salt", 0.3, Unit.PINCH)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        Recipe saltyDish3 = new Recipe(
                "r6c",
                "Salty dish 3",
                MealType.LUNCH,
                List.of(new RecipeIngredient("salt", 0.3, Unit.PINCH)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(
            List.of(saltyDish1, saltyDish2, saltyDish3)
        );

        assertEquals(1, list.size());
        ShoppingListItem salt = list.get(0);
        assertEquals("salt", salt.ingredientId());
        double requiredAmount = salt.requiredBaseAmount();
        assertEquals(0.3, requiredAmount, 0.001);
    }

    @Test
    void emptyRecipesList() {
        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(List.of());
        assertEquals(0, list.size());
    }

    @Test
    void singleRecipeWithSingleIngredient() {
        Recipe recipe = new Recipe(
                "r7",
                "Simple salad",
                MealType.LUNCH,
                List.of(new RecipeIngredient("oil", 100, Unit.MILLILITER)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(List.of(recipe));

        assertEquals(1, list.size());
        ShoppingListItem oil = list.get(0);
        assertEquals("oil", oil.ingredientId());
        assertEquals(100.0, oil.requiredBaseAmount(), 0.001);
        assertEquals(1, oil.suggestedPackages());
    }

    @Test
    void mixedUnitAggregation() {
        Recipe flourBread = new Recipe(
                "r8a",
                "Bread 1",
                MealType.BREAKFAST,
                List.of(new RecipeIngredient("wheat-flour", 2, Unit.CUP)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        Recipe flourCake = new Recipe(
                "r8b",
                "Cake",
                MealType.DINNER,
                List.of(new RecipeIngredient("wheat-flour", 250, Unit.GRAM)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(List.of(flourBread, flourCake));

        assertEquals(1, list.size());
        ShoppingListItem flour = list.get(0);
        assertEquals("wheat-flour", flour.ingredientId());
        assertEquals(500.0, flour.requiredBaseAmount(), 0.001);
    }

    @Test
    void suggestedPackagesCeiling() {
        Recipe recipe = new Recipe(
                "r9",
                "Butter cake",
                MealType.DINNER,
                List.of(new RecipeIngredient("butter", 300, Unit.GRAM)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(List.of(recipe));

        assertEquals(1, list.size());
        ShoppingListItem butter = list.get(0);
        assertEquals(2, butter.suggestedPackages());
    }

    @Test
    void unknownIngredientThrows() {
        Recipe recipe = new Recipe(
                "r10",
                "Mystery dish",
                MealType.LUNCH,
                List.of(new RecipeIngredient("nonexistent-ingredient", 100, Unit.GRAM)),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        assertThrows(IllegalArgumentException.class, () ->
            shoppingListService.generateFromRecipes(List.of(recipe))
        );
    }

    @Test
    void multipleIngredientsInShoppingList() {
        Recipe complexRecipe = new Recipe(
                "r11",
                "Complex dish",
                MealType.DINNER,
                List.of(
                    new RecipeIngredient("rice", 1, Unit.CUP),
                    new RecipeIngredient("oil", 2, Unit.TABLESPOON),
                    new RecipeIngredient("salt", 0.5, Unit.PINCH)
                ),
                null,
                null,
                Set.of(),
                0,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ShoppingListItem> list = shoppingListService.generateFromRecipes(List.of(complexRecipe));

        assertEquals(3, list.size());
    }
}
