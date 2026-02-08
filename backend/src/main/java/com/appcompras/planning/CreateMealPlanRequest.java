package com.appcompras.planning;

import com.appcompras.recipe.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateMealPlanRequest(
        @Schema(example = "2026-02-09")
        @NotNull LocalDate startDate,
        @Schema(example = "WEEK")
        @NotNull PlanPeriod period,
        List<@Valid SlotInput> slots
) {

    public record SlotInput(
            @Schema(example = "2026-02-10")
            @NotNull LocalDate date,
            @Schema(example = "LUNCH")
            @NotNull MealType mealType,
            @Schema(example = "3f84c4ce-62bb-4cf5-a42e-19a5df1709ce")
            @NotBlank String recipeId
    ) {
    }
}
