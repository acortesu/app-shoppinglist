package com.appcompras.planning;

import com.appcompras.recipe.RecipeService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MealPlanService {

    private final Map<String, MealPlan> plans = new ConcurrentHashMap<>();
    private final RecipeService recipeService;

    public MealPlanService(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    public MealPlan create(CreateMealPlanRequest request) {
        LocalDate endDate = endDateFor(request.startDate(), request.period());
        List<CreateMealPlanRequest.SlotInput> inputSlots = request.slots() == null ? List.of() : request.slots();

        validateSlots(request.startDate(), endDate, inputSlots);

        List<PlannedMealSlot> slots = new ArrayList<>();
        Instant usageTime = Instant.now();
        for (CreateMealPlanRequest.SlotInput slot : inputSlots) {
            boolean used = recipeService.incrementUsageCount(slot.recipeId(), usageTime);
            if (!used) {
                throw new IllegalArgumentException("Recipe not found for slot: " + slot.recipeId());
            }
            slots.add(new PlannedMealSlot(slot.date(), slot.mealType(), slot.recipeId()));
        }

        Instant now = Instant.now();
        MealPlan plan = new MealPlan(
                UUID.randomUUID().toString(),
                request.startDate(),
                endDate,
                request.period(),
                slots,
                now,
                now
        );

        plans.put(plan.id(), plan);
        return plan;
    }

    public Optional<MealPlan> findById(String id) {
        return Optional.ofNullable(plans.get(id));
    }

    private LocalDate endDateFor(LocalDate startDate, PlanPeriod period) {
        return switch (period) {
            case WEEK -> startDate.plusDays(6);
            case FORTNIGHT -> startDate.plusDays(13);
        };
    }

    private void validateSlots(LocalDate startDate, LocalDate endDate, List<CreateMealPlanRequest.SlotInput> slots) {
        Map<String, Boolean> uniqueSlots = new HashMap<>();
        for (CreateMealPlanRequest.SlotInput slot : slots) {
            if (slot.date().isBefore(startDate) || slot.date().isAfter(endDate)) {
                throw new IllegalArgumentException("Slot date out of plan range: " + slot.date());
            }

            String key = slot.date() + "|" + slot.mealType();
            if (uniqueSlots.put(key, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("Duplicate slot for date and mealType: " + key);
            }
        }
    }
}
