package com.appcompras.service;

import com.appcompras.domain.MealType;
import com.appcompras.domain.Recipe;
import com.appcompras.domain.RecipeIngredient;
import com.appcompras.domain.ShoppingListItem;
import com.appcompras.domain.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoppingListServiceTest {

    private ShoppingListService shoppingListService;

    @BeforeEach
    void setUp() {
        IngredientCatalogService catalogService = new IngredientCatalogService();
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
}
