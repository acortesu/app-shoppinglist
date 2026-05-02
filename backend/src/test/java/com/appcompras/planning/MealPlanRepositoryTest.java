package com.appcompras.planning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class MealPlanRepositoryTest {

    @Autowired
    private MealPlanRepository mealPlanRepository;

    private MealPlanEntity planUserA;
    private MealPlanEntity planUserB;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        LocalDate startDate = LocalDate.of(2026, 5, 1);

        planUserA = new MealPlanEntity();
        planUserA.setId("plan-a-1");
        planUserA.setUserId("user-a");
        planUserA.setStartDate(startDate);
        planUserA.setEndDate(startDate.plusDays(6));
        planUserA.setPeriod(PlanPeriod.WEEK);
        planUserA.setCreatedAt(now);
        planUserA.setUpdatedAt(now);
        mealPlanRepository.save(planUserA);

        planUserB = new MealPlanEntity();
        planUserB.setId("plan-b-1");
        planUserB.setUserId("user-b");
        planUserB.setStartDate(startDate);
        planUserB.setEndDate(startDate.plusDays(6));
        planUserB.setPeriod(PlanPeriod.WEEK);
        planUserB.setCreatedAt(now);
        planUserB.setUpdatedAt(now);
        mealPlanRepository.save(planUserB);
    }

    @Test
    void findByIdAndUserIdReturnsWhenMatch() {
        Optional<MealPlanEntity> result = mealPlanRepository.findByIdAndUserId("plan-a-1", "user-a");
        assertTrue(result.isPresent());
        assertEquals("plan-a-1", result.get().getId());
        assertEquals("user-a", result.get().getUserId());
    }

    @Test
    void findByIdAndUserIdReturnsEmptyWhenUserMismatch() {
        Optional<MealPlanEntity> result = mealPlanRepository.findByIdAndUserId("plan-a-1", "user-b");
        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndUserIdReturnsEmptyWhenIdNotFound() {
        Optional<MealPlanEntity> result = mealPlanRepository.findByIdAndUserId("nonexistent", "user-a");
        assertFalse(result.isPresent());
    }

    @Test
    void existsByIdAndUserIdReturnsTrueWhenMatch() {
        boolean result = mealPlanRepository.existsByIdAndUserId("plan-a-1", "user-a");
        assertTrue(result);
    }

    @Test
    void existsByIdAndUserIdReturnsFalseWhenUserMismatch() {
        boolean result = mealPlanRepository.existsByIdAndUserId("plan-a-1", "user-b");
        assertFalse(result);
    }

    @Test
    void existsByIdAndUserIdReturnsFalseWhenIdNotFound() {
        boolean result = mealPlanRepository.existsByIdAndUserId("nonexistent", "user-a");
        assertFalse(result);
    }

    @Test
    void findAllByUserIdOrdersByCreatedAtDescThenIdAsc() {
        Instant now = Instant.now();
        LocalDate startDate = LocalDate.of(2026, 5, 1);

        MealPlanEntity older = new MealPlanEntity();
        older.setId("plan-a-older");
        older.setUserId("user-a");
        older.setStartDate(startDate);
        older.setEndDate(startDate.plusDays(6));
        older.setPeriod(PlanPeriod.WEEK);
        older.setCreatedAt(now.minusSeconds(100));
        older.setUpdatedAt(now.minusSeconds(100));
        mealPlanRepository.save(older);

        MealPlanEntity newer = new MealPlanEntity();
        newer.setId("plan-a-newer");
        newer.setUserId("user-a");
        newer.setStartDate(startDate);
        newer.setEndDate(startDate.plusDays(6));
        newer.setPeriod(PlanPeriod.WEEK);
        newer.setCreatedAt(now.plusSeconds(100));
        newer.setUpdatedAt(now.plusSeconds(100));
        mealPlanRepository.save(newer);

        List<MealPlanEntity> results = mealPlanRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a");

        assertEquals(3, results.size());
        assertEquals("plan-a-newer", results.get(0).getId());
        assertEquals("plan-a-1", results.get(1).getId());
        assertEquals("plan-a-older", results.get(2).getId());
    }

    @Test
    void findAllByUserIdFiltersPerUser() {
        List<MealPlanEntity> resultsUserA = mealPlanRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a");
        List<MealPlanEntity> resultsUserB = mealPlanRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-b");

        assertEquals(1, resultsUserA.size());
        assertEquals(1, resultsUserB.size());
        assertEquals("plan-a-1", resultsUserA.get(0).getId());
        assertEquals("plan-b-1", resultsUserB.get(0).getId());
    }

    @Test
    void findAllByUserIdReturnsEmptyForUnknownUser() {
        List<MealPlanEntity> results = mealPlanRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("unknown-user");
        assertEquals(0, results.size());
    }

    @Test
    void findAllByUserIdSecondSortKeyIdAscWhenCreatedAtSame() {
        Instant same = Instant.parse("2026-05-01T12:00:00Z");
        LocalDate startDate = LocalDate.of(2026, 5, 1);

        // Update planUserA to have same createdAt as the new plans
        planUserA.setCreatedAt(same);
        planUserA.setUpdatedAt(same);
        mealPlanRepository.save(planUserA);

        MealPlanEntity planZ = new MealPlanEntity();
        planZ.setId("plan-a-z");
        planZ.setUserId("user-a");
        planZ.setStartDate(startDate);
        planZ.setEndDate(startDate.plusDays(6));
        planZ.setPeriod(PlanPeriod.WEEK);
        planZ.setCreatedAt(same);
        planZ.setUpdatedAt(same);
        mealPlanRepository.save(planZ);

        MealPlanEntity planM = new MealPlanEntity();
        planM.setId("plan-a-m");
        planM.setUserId("user-a");
        planM.setStartDate(startDate);
        planM.setEndDate(startDate.plusDays(6));
        planM.setPeriod(PlanPeriod.WEEK);
        planM.setCreatedAt(same);
        planM.setUpdatedAt(same);
        mealPlanRepository.save(planM);

        List<MealPlanEntity> results = mealPlanRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a");

        assertEquals(3, results.size());
        // All have same createdAt, so ordered by id ascending: 1 < m < z
        assertEquals("plan-a-1", results.get(0).getId());
        assertEquals("plan-a-m", results.get(1).getId());
        assertEquals("plan-a-z", results.get(2).getId());
    }

    @Test
    void saveAndRetrievePersistsAllFields() {
        Instant created = Instant.parse("2026-05-01T10:00:00Z");
        Instant updated = Instant.parse("2026-05-01T15:00:00Z");
        LocalDate start = LocalDate.of(2026, 5, 5);
        LocalDate end = LocalDate.of(2026, 5, 12);

        MealPlanEntity plan = new MealPlanEntity();
        plan.setId("plan-full");
        plan.setUserId("user-full");
        plan.setStartDate(start);
        plan.setEndDate(end);
        plan.setPeriod(PlanPeriod.FORTNIGHT);
        plan.setCreatedAt(created);
        plan.setUpdatedAt(updated);

        MealPlanEntity saved = mealPlanRepository.save(plan);
        Optional<MealPlanEntity> retrieved = mealPlanRepository.findByIdAndUserId("plan-full", "user-full");

        assertTrue(retrieved.isPresent());
        assertEquals("plan-full", retrieved.get().getId());
        assertEquals("user-full", retrieved.get().getUserId());
        assertEquals(start, retrieved.get().getStartDate());
        assertEquals(end, retrieved.get().getEndDate());
        assertEquals(PlanPeriod.FORTNIGHT, retrieved.get().getPeriod());
        assertEquals(created, retrieved.get().getCreatedAt());
        assertEquals(updated, retrieved.get().getUpdatedAt());
    }

    @Test
    void deleteRemovesEntity() {
        mealPlanRepository.delete(planUserA);
        Optional<MealPlanEntity> result = mealPlanRepository.findByIdAndUserId("plan-a-1", "user-a");
        assertFalse(result.isPresent());
    }

    @Test
    void deleteOnlyAffectsTargetEntity() {
        mealPlanRepository.delete(planUserA);
        Optional<MealPlanEntity> planAResult = mealPlanRepository.findByIdAndUserId("plan-a-1", "user-a");
        Optional<MealPlanEntity> planBResult = mealPlanRepository.findByIdAndUserId("plan-b-1", "user-b");

        assertFalse(planAResult.isPresent());
        assertTrue(planBResult.isPresent());
    }
}
