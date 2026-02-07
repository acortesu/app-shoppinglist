package com.appcompras.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class MealPlanEntityMapper {

    private MealPlanEntityMapper() {
    }

    public static MealPlan toDomain(MealPlanEntity entity) {
        List<PlannedMealSlot> slots = entity.getSlots().stream()
                .map(s -> new PlannedMealSlot(s.getDate(), s.getMealType(), s.getRecipeId()))
                .toList();

        return new MealPlan(
                entity.getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPeriod(),
                slots,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static List<MealPlanSlotEmbeddable> toEmbeddables(List<PlannedMealSlot> slots) {
        return slots.stream()
                .map(s -> new MealPlanSlotEmbeddable(s.date(), s.mealType(), s.recipeId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
