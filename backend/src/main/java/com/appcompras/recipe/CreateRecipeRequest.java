package com.appcompras.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Set;

public record CreateRecipeRequest(
        @Schema(example = "Arroz con huevo")
        @NotBlank String name,
        @Schema(example = "DINNER")
        @NotNull MealType type,
        @NotEmpty List<@Valid IngredientInput> ingredients,
        @Schema(example = "Mezclar y cocinar")
        String preparation,
        @Schema(example = "Cena rápida")
        String notes,
        @Schema(example = "[\"rapido\",\"batch\"]")
        Set<String> tags
) {

    public record IngredientInput(
            @Schema(example = "rice")
            @NotBlank String ingredientId,
            @Schema(example = "200")
            @NotNull @Positive Double quantity,
            @Schema(example = "GRAM")
            @NotNull Unit unit
    ) {
    }
}
