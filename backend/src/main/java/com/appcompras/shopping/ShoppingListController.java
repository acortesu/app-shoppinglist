package com.appcompras.shopping;

import com.appcompras.config.BusinessRuleException;
import com.appcompras.domain.Recipe;
import com.appcompras.domain.RecipeIngredient;
import com.appcompras.planning.MealPlan;
import com.appcompras.planning.MealPlanService;
import com.appcompras.recipe.RecipeService;
import com.appcompras.service.ShoppingListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-lists")
@Tag(name = "Shopping Lists")
public class ShoppingListController {

    private final MealPlanService mealPlanService;
    private final RecipeService recipeService;
    private final ShoppingListService shoppingListService;
    private final ShoppingListDraftService shoppingListDraftService;

    public ShoppingListController(
            MealPlanService mealPlanService,
            RecipeService recipeService,
            ShoppingListService shoppingListService,
            ShoppingListDraftService shoppingListDraftService
    ) {
        this.mealPlanService = mealPlanService;
        this.recipeService = recipeService;
        this.shoppingListService = shoppingListService;
        this.shoppingListDraftService = shoppingListDraftService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate shopping list draft from plan")
    public ShoppingListResponse generate(
            @RequestParam String planId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        MealPlan plan = mealPlanService.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        List<Recipe> recipes = plan.slots().stream()
                .map(slot -> recipeService.findById(slot.recipeId())
                        .map(this::toDomainRecipe)
                        .orElseThrow(() -> new BusinessRuleException(
                                "PLAN_RECIPE_NOT_FOUND",
                                "Recipe not found for slot: " + slot.recipeId()
                        )))
                .toList();

        ShoppingListDraft draft = shoppingListDraftService.createFromGenerated(
                planId,
                shoppingListService.generateFromRecipes(recipes),
                idempotencyKey
        );

        return ShoppingListResponse.from(draft);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shopping list draft by id")
    public ShoppingListResponse getById(@PathVariable String id) {
        ShoppingListDraft draft = shoppingListDraftService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shopping list not found"));
        return ShoppingListResponse.from(draft);
    }

    @GetMapping
    @Operation(summary = "List shopping list drafts")
    public List<ShoppingListResponse> getAll() {
        return shoppingListDraftService.findAll().stream()
                .map(ShoppingListResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace shopping list draft items")
    public ShoppingListResponse update(@PathVariable String id, @Valid @RequestBody UpdateShoppingListRequest request) {
        ShoppingListDraft updated = shoppingListDraftService.replaceItems(id, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shopping list not found"));
        return ShoppingListResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete shopping list draft")
    public void delete(@PathVariable String id) {
        boolean deleted = shoppingListDraftService.deleteById(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shopping list not found");
        }
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
