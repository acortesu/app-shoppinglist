package com.appcompras.domain;

public record RecipeIngredient(
        String ingredientId,
        double quantity,
        Unit unit
) {
}
