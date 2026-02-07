package com.appcompras.recipe;

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

    public Recipe create(CreateRecipeRequest request) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();

        List<RecipeIngredient> ingredients = request.ingredients().stream()
                .map(i -> new RecipeIngredient(i.ingredientId(), i.quantity(), i.unit()))
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
                    .map(i -> new RecipeIngredient(i.ingredientId(), i.quantity(), i.unit()))
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
}
