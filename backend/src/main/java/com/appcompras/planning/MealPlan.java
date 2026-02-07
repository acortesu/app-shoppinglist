package com.appcompras.planning;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MealPlan(
        String id,
        LocalDate startDate,
        LocalDate endDate,
        PlanPeriod period,
        List<PlannedMealSlot> slots,
        Instant createdAt,
        Instant updatedAt
) {
}
