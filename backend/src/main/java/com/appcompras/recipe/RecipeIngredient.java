package com.appcompras.recipe;

import com.appcompras.domain.Unit;

public record RecipeIngredient(
        String ingredientId,
        double quantity,
        Unit unit
) {
}
