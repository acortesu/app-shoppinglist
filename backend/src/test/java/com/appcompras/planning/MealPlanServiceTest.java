package com.appcompras.planning;

import com.appcompras.config.BusinessRuleException;
import com.appcompras.recipe.MealType;
import com.appcompras.recipe.Recipe;
import com.appcompras.recipe.RecipeService;
import com.appcompras.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealPlanServiceTest {

    @Mock
    private MealPlanRepository mealPlanRepository;

    @Mock
    private RecipeService recipeService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private MealPlanService mealPlanService;

    @BeforeEach
    void setUp() {
        mealPlanService = new MealPlanService(mealPlanRepository, recipeService, currentUserProvider);
        when(currentUserProvider.getCurrentUserId()).thenReturn("test-user-id");
    }

    @Test
    void createMealPlanValidatesDateRange() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate validDate = LocalDate.of(2026, 5, 5);
        LocalDate outOfRangeDate = LocalDate.of(2026, 5, 10);

        CreateMealPlanRequest.SlotInput validSlot = new CreateMealPlanRequest.SlotInput(validDate, MealType.LUNCH, "recipe-1");
        CreateMealPlanRequest.SlotInput outOfRangeSlot = new CreateMealPlanRequest.SlotInput(outOfRangeDate, MealType.LUNCH, "recipe-2");

        CreateMealPlanRequest request = new CreateMealPlanRequest(
            startDate,
            PlanPeriod.WEEK,
            List.of(outOfRangeSlot)
        );

        assertThrows(BusinessRuleException.class, () -> mealPlanService.create(request));
    }

    @Test
    void createMealPlanRejectsDuplicateSlots() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate date = LocalDate.of(2026, 5, 3);

        CreateMealPlanRequest.SlotInput slot1 = new CreateMealPlanRequest.SlotInput(date, MealType.LUNCH, "recipe-1");
        CreateMealPlanRequest.SlotInput slot2 = new CreateMealPlanRequest.SlotInput(date, MealType.LUNCH, "recipe-2");

        CreateMealPlanRequest request = new CreateMealPlanRequest(
            startDate,
            PlanPeriod.WEEK,
            List.of(slot1, slot2)
        );

        assertThrows(BusinessRuleException.class, () -> mealPlanService.create(request));
    }

    @Test
    void createMealPlanValidatesRecipesExist() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate date = LocalDate.of(2026, 5, 3);

        CreateMealPlanRequest.SlotInput slot = new CreateMealPlanRequest.SlotInput(date, MealType.LUNCH, "nonexistent-recipe");

        CreateMealPlanRequest request = new CreateMealPlanRequest(
            startDate,
            PlanPeriod.WEEK,
            List.of(slot)
        );

        when(recipeService.findById("nonexistent-recipe")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> mealPlanService.create(request));
    }

    @Test
    void createMealPlanWithWeekPeriodCalculatesEndDate() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate date = LocalDate.of(2026, 5, 3);

        CreateMealPlanRequest.SlotInput slot = new CreateMealPlanRequest.SlotInput(date, MealType.LUNCH, "recipe-1");
        CreateMealPlanRequest request = new CreateMealPlanRequest(
            startDate,
            PlanPeriod.WEEK,
            List.of(slot)
        );

        Recipe recipe = new Recipe("recipe-1", "Test Recipe", MealType.LUNCH, List.of(), null, null, null, 0, null, Instant.now(), Instant.now());
        when(recipeService.findById("recipe-1")).thenReturn(Optional.of(recipe));

        MealPlanEntity savedEntity = new MealPlanEntity();
        savedEntity.setId("plan-id");
        savedEntity.setStartDate(startDate);
        savedEntity.setEndDate(startDate.plusDays(6));
        savedEntity.setPeriod(PlanPeriod.WEEK);
        when(mealPlanRepository.save(any(MealPlanEntity.class))).thenReturn(savedEntity);

        MealPlan result = mealPlanService.create(request);

        assertEquals(startDate, result.startDate());
        assertEquals(startDate.plusDays(6), result.endDate());
        assertEquals(PlanPeriod.WEEK, result.period());
    }

    @Test
    void createMealPlanWithForthnightPeriodCalculatesEndDate() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate date = LocalDate.of(2026, 5, 5);

        CreateMealPlanRequest.SlotInput slot = new CreateMealPlanRequest.SlotInput(date, MealType.DINNER, "recipe-2");
        CreateMealPlanRequest request = new CreateMealPlanRequest(
            startDate,
            PlanPeriod.FORTNIGHT,
            List.of(slot)
        );

        Recipe recipe = new Recipe("recipe-2", "Dinner", MealType.DINNER, List.of(), null, null, null, 0, null, Instant.now(), Instant.now());
        when(recipeService.findById("recipe-2")).thenReturn(Optional.of(recipe));

        MealPlanEntity savedEntity = new MealPlanEntity();
        savedEntity.setId("plan-id");
        savedEntity.setStartDate(startDate);
        savedEntity.setEndDate(startDate.plusDays(13));
        savedEntity.setPeriod(PlanPeriod.FORTNIGHT);
        when(mealPlanRepository.save(any(MealPlanEntity.class))).thenReturn(savedEntity);

        MealPlan result = mealPlanService.create(request);

        assertEquals(startDate.plusDays(13), result.endDate());
        assertEquals(PlanPeriod.FORTNIGHT, result.period());
    }

    @Test
    void createMealPlanWithEmptySlots() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);

        CreateMealPlanRequest request = new CreateMealPlanRequest(
            startDate,
            PlanPeriod.WEEK,
            null
        );

        MealPlanEntity savedEntity = new MealPlanEntity();
        savedEntity.setId("plan-id");
        savedEntity.setStartDate(startDate);
        savedEntity.setEndDate(startDate.plusDays(6));
        when(mealPlanRepository.save(any(MealPlanEntity.class))).thenReturn(savedEntity);

        MealPlan result = mealPlanService.create(request);

        assertTrue(result.slots().isEmpty());
    }

    @Test
    void findByIdReturnsWhenFound() {
        MealPlanEntity entity = new MealPlanEntity();
        entity.setId("plan-id");
        entity.setUserId("test-user-id");

        when(mealPlanRepository.findByIdAndUserId("plan-id", "test-user-id")).thenReturn(Optional.of(entity));

        Optional<MealPlan> result = mealPlanService.findById("plan-id");

        assertTrue(result.isPresent());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(mealPlanRepository.findByIdAndUserId("nonexistent-id", "test-user-id")).thenReturn(Optional.empty());

        Optional<MealPlan> result = mealPlanService.findById("nonexistent-id");

        assertFalse(result.isPresent());
    }

    @Test
    void deleteByIdReturnsTrueWhenDeleted() {
        MealPlanEntity entity = new MealPlanEntity();
        entity.setId("plan-id");

        when(mealPlanRepository.findByIdAndUserId("plan-id", "test-user-id")).thenReturn(Optional.of(entity));

        boolean result = mealPlanService.deleteById("plan-id");

        assertTrue(result);
    }

    @Test
    void deleteByIdReturnsFalseWhenNotFound() {
        when(mealPlanRepository.findByIdAndUserId("nonexistent-id", "test-user-id")).thenReturn(Optional.empty());

        boolean result = mealPlanService.deleteById("nonexistent-id");

        assertFalse(result);
    }
}
