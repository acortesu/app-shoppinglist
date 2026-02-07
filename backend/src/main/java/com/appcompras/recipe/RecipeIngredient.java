package com.appcompras.recipe;

public record RecipeIngredient(
        String ingredientId,
        double quantity,
        Unit unit
) {
}
