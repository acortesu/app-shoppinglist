package com.appcompras.planning;

import com.appcompras.recipe.RecipeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final RecipeService recipeService;

    public MealPlanService(MealPlanRepository mealPlanRepository, RecipeService recipeService) {
        this.mealPlanRepository = mealPlanRepository;
        this.recipeService = recipeService;
    }

    @Transactional
    public MealPlan create(CreateMealPlanRequest request) {
        Instant now = Instant.now();
        MealPlan plan = buildPlan(UUID.randomUUID().toString(), request, now, now);

        MealPlanEntity entity = new MealPlanEntity();
        entity.setId(plan.id());
        entity.setStartDate(plan.startDate());
        entity.setEndDate(plan.endDate());
        entity.setPeriod(plan.period());
        entity.setSlots(MealPlanEntityMapper.toEmbeddables(plan.slots()));
        entity.setCreatedAt(plan.createdAt());
        entity.setUpdatedAt(plan.updatedAt());

        MealPlanEntity saved = mealPlanRepository.save(entity);
        applyUsageDelta(List.of(), plan.slots());
        return MealPlanEntityMapper.toDomain(saved);
    }

    @Transactional
    public Optional<MealPlan> update(String id, CreateMealPlanRequest request) {
        Optional<MealPlanEntity> existingOpt = mealPlanRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        MealPlanEntity existing = existingOpt.get();
        List<PlannedMealSlot> previousSlots = MealPlanEntityMapper.toDomain(existing).slots();
        Instant now = Instant.now();
        MealPlan next = buildPlan(existing.getId(), request, existing.getCreatedAt(), now);

        existing.setStartDate(next.startDate());
        existing.setEndDate(next.endDate());
        existing.setPeriod(next.period());
        existing.setSlots(MealPlanEntityMapper.toEmbeddables(next.slots()));
        existing.setUpdatedAt(next.updatedAt());

        MealPlanEntity saved = mealPlanRepository.save(existing);
        applyUsageDelta(previousSlots, next.slots());
        return Optional.of(MealPlanEntityMapper.toDomain(saved));
    }

    @Transactional(readOnly = true)
    public List<MealPlan> findAll() {
        return mealPlanRepository.findAllByOrderByCreatedAtDescIdAsc().stream()
                .map(MealPlanEntityMapper::toDomain)
                .toList();
    }

    @Transactional
    public boolean deleteById(String id) {
        if (!mealPlanRepository.existsById(id)) {
            return false;
        }
        mealPlanRepository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<MealPlan> findById(String id) {
        return mealPlanRepository.findById(id)
                .map(MealPlanEntityMapper::toDomain);
    }

    private MealPlan buildPlan(String id, CreateMealPlanRequest request, Instant createdAt, Instant updatedAt) {
        LocalDate endDate = endDateFor(request.startDate(), request.period());
        List<CreateMealPlanRequest.SlotInput> inputSlots = request.slots() == null ? List.of() : request.slots();
        validateSlots(request.startDate(), endDate, inputSlots);
        validateRecipesExist(inputSlots);

        List<PlannedMealSlot> slots = inputSlots.stream()
                .map(slot -> new PlannedMealSlot(slot.date(), slot.mealType(), slot.recipeId()))
                .toList();

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
