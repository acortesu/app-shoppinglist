package com.appcompras.recipe;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public List<Recipe> findAll() {
        return recipes.values().stream().toList();
    }
}
