package com.appcompras.planning;

import com.appcompras.recipe.MealType;

import java.time.LocalDate;

public record PlannedMealSlot(
        LocalDate date,
        MealType mealType,
        String recipeId
) {
}
