package com.appcompras.recipe;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class RecipeEntityMapper {

    private RecipeEntityMapper() {
    }

    public static Recipe toDomain(RecipeEntity entity) {
        List<RecipeIngredient> ingredients = entity.getIngredients().stream()
                .map(i -> new RecipeIngredient(i.getIngredientId(), i.getQuantity(), i.getUnit()))
                .toList();

        Set<String> tags = entity.getTags() == null ? Set.of() : Set.copyOf(entity.getTags());

        return new Recipe(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                ingredients,
                entity.getPreparation(),
                entity.getNotes(),
                tags,
                entity.getUsageCount(),
                entity.getLastUsedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static List<RecipeIngredientEmbeddable> toEmbeddables(List<RecipeIngredient> ingredients) {
        return ingredients.stream()
                .map(i -> new RecipeIngredientEmbeddable(i.ingredientId(), i.quantity(), i.unit()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static Set<String> toTagSet(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(tags);
    }
}
