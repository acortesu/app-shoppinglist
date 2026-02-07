package com.appcompras.recipe;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record RecipeResponse(
        String id,
        String name,
        MealType type,
        List<RecipeIngredient> ingredients,
        String preparation,
        String notes,
        Set<String> tags,
        int usageCount,
        Instant lastUsedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(
                recipe.id(),
                recipe.name(),
                recipe.type(),
                recipe.ingredients(),
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
