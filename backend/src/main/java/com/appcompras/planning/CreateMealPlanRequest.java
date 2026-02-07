package com.appcompras.planning;

import com.appcompras.recipe.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateMealPlanRequest(
        @NotNull LocalDate startDate,
        @NotNull PlanPeriod period,
        List<@Valid SlotInput> slots
) {

    public record SlotInput(
            @NotNull LocalDate date,
            @NotNull MealType mealType,
            @NotBlank String recipeId
    ) {
    }
}
