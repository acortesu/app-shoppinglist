package com.appcompras.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record Recipe(
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
}
