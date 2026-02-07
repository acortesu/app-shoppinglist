package com.appcompras.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record CreateRecipeRequest(
        @NotBlank String name,
        @NotNull MealType type,
        @NotEmpty List<@Valid IngredientInput> ingredients,
        String preparation,
        String notes,
        Set<String> tags
) {

    public record IngredientInput(
            @NotBlank String ingredientId,
            @NotNull Double quantity,
            @NotNull Unit unit
    ) {
    }
}
