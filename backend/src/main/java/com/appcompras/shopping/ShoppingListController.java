package com.appcompras.shopping;

import com.appcompras.domain.Recipe;
import com.appcompras.domain.RecipeIngredient;
import com.appcompras.planning.MealPlan;
import com.appcompras.planning.MealPlanService;
import com.appcompras.recipe.RecipeService;
import com.appcompras.service.ShoppingListService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-lists")
public class ShoppingListController {

    private final MealPlanService mealPlanService;
    private final RecipeService recipeService;
    private final ShoppingListService shoppingListService;

    public ShoppingListController(
            MealPlanService mealPlanService,
            RecipeService recipeService,
            ShoppingListService shoppingListService
    ) {
        this.mealPlanService = mealPlanService;
        this.recipeService = recipeService;
        this.shoppingListService = shoppingListService;
    }

    @PostMapping("/generate")
    public GeneratedShoppingListResponse generate(@RequestParam String planId) {
        MealPlan plan = mealPlanService.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        List<Recipe> recipes = plan.slots().stream()
                .map(slot -> recipeService.findById(slot.recipeId())
                        .map(this::toDomainRecipe)
                        .orElseThrow(() -> new IllegalArgumentException("Recipe not found for slot: " + slot.recipeId())))
                .toList();

        return new GeneratedShoppingListResponse(planId, shoppingListService.generateFromRecipes(recipes));
    }

    private Recipe toDomainRecipe(com.appcompras.recipe.Recipe recipe) {
        List<RecipeIngredient> ingredients = recipe.ingredients().stream()
                .map(i -> new RecipeIngredient(
                        i.ingredientId(),
                        i.quantity(),
                        com.appcompras.domain.Unit.valueOf(i.unit().name())
                ))
                .toList();

        return new Recipe(
                recipe.id(),
                recipe.name(),
                com.appcompras.domain.MealType.valueOf(recipe.type().name()),
                ingredients,
                recipe.preparation(),
                recipe.notes(),
                recipe.tags(),
                recipe.usageCount(),
                recipe.lastUsedAt(),
                recipe.createdAt(),
                recipe.updatedAt()
        );
    }
}
