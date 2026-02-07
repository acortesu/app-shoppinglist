package com.appcompras.planning;

import com.appcompras.recipe.RecipeService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
        Instant now = Instant.now();
        MealPlan plan = buildPlan(UUID.randomUUID().toString(), request, now, now);
        plans.put(plan.id(), plan);
        applyUsageDelta(List.of(), plan.slots());
        return plan;
    }

    public Optional<MealPlan> update(String id, CreateMealPlanRequest request) {
        MealPlan updated = plans.computeIfPresent(id, (ignored, existing) -> {
            Instant now = Instant.now();
            MealPlan next = buildPlan(existing.id(), request, existing.createdAt(), now);
            applyUsageDelta(existing.slots(), next.slots());
            return next;
        });
        return Optional.ofNullable(updated);
    }

    public List<MealPlan> findAll() {
        return plans.values().stream()
                .sorted(Comparator.comparing(MealPlan::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(MealPlan::id))
                .toList();
    }

    public boolean deleteById(String id) {
        return plans.remove(id) != null;
    }

    public Optional<MealPlan> findById(String id) {
        return Optional.ofNullable(plans.get(id));
    }

    private MealPlan buildPlan(String id, CreateMealPlanRequest request, Instant createdAt, Instant updatedAt) {
        LocalDate endDate = endDateFor(request.startDate(), request.period());
        List<CreateMealPlanRequest.SlotInput> inputSlots = request.slots() == null ? List.of() : request.slots();
        validateSlots(request.startDate(), endDate, inputSlots);
        validateRecipesExist(inputSlots);

        List<PlannedMealSlot> slots = new ArrayList<>();
        for (CreateMealPlanRequest.SlotInput slot : inputSlots) {
            slots.add(new PlannedMealSlot(slot.date(), slot.mealType(), slot.recipeId()));
        }
        return new MealPlan(id, request.startDate(), endDate, request.period(), slots, createdAt, updatedAt);
    }

    private LocalDate endDateFor(LocalDate startDate, PlanPeriod period) {
        return switch (period) {
            case WEEK -> startDate.plusDays(6);
            case FORTNIGHT -> startDate.plusDays(13);
        };
    }

    private void validateRecipesExist(List<CreateMealPlanRequest.SlotInput> slots) {
        for (CreateMealPlanRequest.SlotInput slot : slots) {
            if (recipeService.findById(slot.recipeId()).isEmpty()) {
                throw new IllegalArgumentException("Recipe not found for slot: " + slot.recipeId());
            }
        }
    }

    private void applyUsageDelta(List<PlannedMealSlot> previousSlots, List<PlannedMealSlot> nextSlots) {
        Map<String, Integer> previousCountByRecipe = countByRecipe(previousSlots);
        Map<String, Integer> nextCountByRecipe = countByRecipe(nextSlots);
        Instant usageTime = Instant.now();

        for (Map.Entry<String, Integer> entry : nextCountByRecipe.entrySet()) {
            int oldCount = previousCountByRecipe.getOrDefault(entry.getKey(), 0);
            int delta = entry.getValue() - oldCount;
            if (delta <= 0) {
                continue;
            }

            for (int i = 0; i < delta; i++) {
                recipeService.incrementUsageCount(entry.getKey(), usageTime);
            }
        }
    }

    private Map<String, Integer> countByRecipe(List<PlannedMealSlot> slots) {
        Map<String, Integer> count = new HashMap<>();
        for (PlannedMealSlot slot : slots) {
            count.merge(slot.recipeId(), 1, Integer::sum);
        }
        return count;
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
