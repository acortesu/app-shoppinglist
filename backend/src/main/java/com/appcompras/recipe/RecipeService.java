package com.appcompras.recipe;

import com.appcompras.service.IngredientCatalogService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@Service
public class RecipeService {

    private final Map<String, Recipe> recipes = new ConcurrentHashMap<>();
    private final IngredientCatalogService ingredientCatalogService;

    public RecipeService(IngredientCatalogService ingredientCatalogService) {
        this.ingredientCatalogService = ingredientCatalogService;
    }

    public Recipe create(CreateRecipeRequest request) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();

        List<RecipeIngredient> ingredients = request.ingredients().stream()
                .map(this::toValidatedIngredient)
                .toList();

        Recipe recipe = new Recipe(
                id,
                request.name().trim(),
                request.type(),
                ingredients,
                request.preparation(),
                request.notes(),
                request.tags() == null ? Set.of() : request.tags(),
                0,
                null,
                now,
                now
        );

        recipes.put(id, recipe);
        return recipe;
    }

    public Optional<Recipe> findById(String id) {
        return Optional.ofNullable(recipes.get(id));
    }

    public List<Recipe> findAll(MealType type) {
        return recipes.values().stream()
                .filter(recipe -> type == null || recipe.type() == type)
                .sorted(Comparator.comparing(Recipe::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(Recipe::id))
                .toList();
    }

    public Optional<Recipe> update(String id, CreateRecipeRequest request) {
        BiFunction<String, Recipe, Recipe> updater = (ignored, existing) -> {
            Instant now = Instant.now();

            List<RecipeIngredient> ingredients = request.ingredients().stream()
                    .map(this::toValidatedIngredient)
                    .toList();

            return new Recipe(
                    existing.id(),
                    request.name().trim(),
                    request.type(),
                    ingredients,
                    request.preparation(),
                    request.notes(),
                    request.tags() == null ? Set.of() : request.tags(),
                    existing.usageCount(),
                    existing.lastUsedAt(),
                    existing.createdAt(),
                    now
            );
        };

        Recipe updated = recipes.computeIfPresent(id, updater);
        return Optional.ofNullable(updated);
    }

    public boolean deleteById(String id) {
        return recipes.remove(id) != null;
    }

    public boolean incrementUsageCount(String id, Instant usedAt) {
        Recipe updated = recipes.computeIfPresent(id, (ignored, existing) -> new Recipe(
                existing.id(),
                existing.name(),
                existing.type(),
                existing.ingredients(),
                existing.preparation(),
                existing.notes(),
                existing.tags(),
                existing.usageCount() + 1,
                usedAt,
                existing.createdAt(),
                Instant.now()
        ));
        return updated != null;
    }

    private RecipeIngredient toValidatedIngredient(CreateRecipeRequest.IngredientInput input) {
        String canonicalIngredientId = ingredientCatalogService.resolveIngredientId(input.ingredientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown ingredient: " + input.ingredientId() + ". Use /api/ingredients to discover options or create custom."
                ));

        com.appcompras.domain.Unit domainUnit = com.appcompras.domain.Unit.valueOf(input.unit().name());
        if (!ingredientCatalogService.isUnitAllowed(canonicalIngredientId, domainUnit)) {
            throw new IllegalArgumentException(
                    "Unit " + input.unit() + " is not allowed for ingredient " + canonicalIngredientId
            );
        }

        return new RecipeIngredient(canonicalIngredientId, input.quantity(), input.unit());
    }
}
