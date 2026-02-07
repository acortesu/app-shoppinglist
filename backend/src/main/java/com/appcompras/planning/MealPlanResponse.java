package com.appcompras.planning;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MealPlanResponse(
        String id,
        LocalDate startDate,
        LocalDate endDate,
        PlanPeriod period,
        List<PlannedMealSlot> slots,
        Instant createdAt,
        Instant updatedAt
) {
    public static MealPlanResponse from(MealPlan plan) {
        return new MealPlanResponse(
                plan.id(),
                plan.startDate(),
                plan.endDate(),
                plan.period(),
                plan.slots(),
                plan.createdAt(),
                plan.updatedAt()
        );
    }
}
