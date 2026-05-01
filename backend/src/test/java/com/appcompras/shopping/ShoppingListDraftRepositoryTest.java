package com.appcompras.shopping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class ShoppingListDraftRepositoryTest {

    @Autowired
    private ShoppingListDraftRepository shoppingListDraftRepository;

    private ShoppingListDraftEntity draftUserA;
    private ShoppingListDraftEntity draftUserB;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        draftUserA = new ShoppingListDraftEntity();
        draftUserA.setId("draft-a-1");
        draftUserA.setUserId("user-a");
        draftUserA.setPlanId("plan-1");
        draftUserA.setIdempotencyKey("key-a-1");
        draftUserA.setCreatedAt(now);
        draftUserA.setUpdatedAt(now);
        shoppingListDraftRepository.save(draftUserA);

        draftUserB = new ShoppingListDraftEntity();
        draftUserB.setId("draft-b-1");
        draftUserB.setUserId("user-b");
        draftUserB.setPlanId("plan-2");
        draftUserB.setIdempotencyKey("key-b-1");
        draftUserB.setCreatedAt(now);
        draftUserB.setUpdatedAt(now);
        shoppingListDraftRepository.save(draftUserB);
    }

    @Test
    void findByIdAndUserIdReturnsWhenMatch() {
        Optional<ShoppingListDraftEntity> result = shoppingListDraftRepository.findByIdAndUserId("draft-a-1", "user-a");
        assertTrue(result.isPresent());
        assertEquals("draft-a-1", result.get().getId());
        assertEquals("user-a", result.get().getUserId());
    }

    @Test
    void findByIdAndUserIdReturnsEmptyWhenUserMismatch() {
        Optional<ShoppingListDraftEntity> result = shoppingListDraftRepository.findByIdAndUserId("draft-a-1", "user-b");
        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndUserIdReturnsEmptyWhenIdNotFound() {
        Optional<ShoppingListDraftEntity> result = shoppingListDraftRepository.findByIdAndUserId("nonexistent", "user-a");
        assertFalse(result.isPresent());
    }

    @Test
    void existsByIdAndUserIdReturnsTrueWhenMatch() {
        boolean result = shoppingListDraftRepository.existsByIdAndUserId("draft-a-1", "user-a");
        assertTrue(result);
    }

    @Test
    void existsByIdAndUserIdReturnsFalseWhenUserMismatch() {
        boolean result = shoppingListDraftRepository.existsByIdAndUserId("draft-a-1", "user-b");
        assertFalse(result);
    }

    @Test
    void existsByIdAndUserIdReturnsFalseWhenIdNotFound() {
        boolean result = shoppingListDraftRepository.existsByIdAndUserId("nonexistent", "user-a");
        assertFalse(result);
    }

    @Test
    void findAllByUserIdOrdersByCreatedAtDescThenIdAsc() {
        Instant now = Instant.now();

        ShoppingListDraftEntity older = new ShoppingListDraftEntity();
        older.setId("draft-a-older");
        older.setUserId("user-a");
        older.setPlanId("plan-1");
        older.setIdempotencyKey("key-a-older");
        older.setCreatedAt(now.minusSeconds(100));
        older.setUpdatedAt(now.minusSeconds(100));
        shoppingListDraftRepository.save(older);

        ShoppingListDraftEntity newer = new ShoppingListDraftEntity();
        newer.setId("draft-a-newer");
        newer.setUserId("user-a");
        newer.setPlanId("plan-1");
        newer.setIdempotencyKey("key-a-newer");
        newer.setCreatedAt(now.plusSeconds(100));
        newer.setUpdatedAt(now.plusSeconds(100));
        shoppingListDraftRepository.save(newer);

        List<ShoppingListDraftEntity> results = shoppingListDraftRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a");

        assertEquals(3, results.size());
        assertEquals("draft-a-newer", results.get(0).getId());
        assertEquals("draft-a-1", results.get(1).getId());
        assertEquals("draft-a-older", results.get(2).getId());
    }

    @Test
    void findAllByUserIdFiltersPerUser() {
        List<ShoppingListDraftEntity> resultsUserA = shoppingListDraftRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-a");
        List<ShoppingListDraftEntity> resultsUserB = shoppingListDraftRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("user-b");

        assertEquals(1, resultsUserA.size());
        assertEquals(1, resultsUserB.size());
        assertEquals("draft-a-1", resultsUserA.get(0).getId());
        assertEquals("draft-b-1", resultsUserB.get(0).getId());
    }

    @Test
    void findAllByUserIdReturnsEmptyForUnknownUser() {
        List<ShoppingListDraftEntity> results = shoppingListDraftRepository.findAllByUserIdOrderByCreatedAtDescIdAsc("unknown-user");
        assertEquals(0, results.size());
    }

    @Test
    void findTopByUserIdAndPlanIdAndIdempotencyKeyReturnsWhenExists() {
        Instant now = Instant.now();

        ShoppingListDraftEntity withKey = new ShoppingListDraftEntity();
        withKey.setId("draft-with-key");
        withKey.setUserId("user-a");
        withKey.setPlanId("plan-1");
        withKey.setIdempotencyKey("idempotent-key");
        withKey.setCreatedAt(now);
        withKey.setUpdatedAt(now);
        shoppingListDraftRepository.save(withKey);

        Optional<ShoppingListDraftEntity> result = shoppingListDraftRepository
                .findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc("user-a", "plan-1", "idempotent-key");

        assertTrue(result.isPresent());
        assertEquals("draft-with-key", result.get().getId());
    }

    @Test
    void findTopByUserIdAndPlanIdAndIdempotencyKeyFiltersPerUserId() {
        Optional<ShoppingListDraftEntity> resultUserA = shoppingListDraftRepository
                .findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc("user-a", "plan-1", "key-a-1");
        Optional<ShoppingListDraftEntity> resultUserB = shoppingListDraftRepository
                .findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc("user-b", "plan-2", "key-b-1");

        assertTrue(resultUserA.isPresent());
        assertTrue(resultUserB.isPresent());
        assertEquals("draft-a-1", resultUserA.get().getId());
        assertEquals("draft-b-1", resultUserB.get().getId());
    }

    @Test
    void findTopByUserIdAndPlanIdAndIdempotencyKeyFiltersPerPlanId() {
        Instant now = Instant.now();

        ShoppingListDraftEntity differentPlan = new ShoppingListDraftEntity();
        differentPlan.setId("draft-a-2");
        differentPlan.setUserId("user-a");
        differentPlan.setPlanId("plan-2");
        differentPlan.setIdempotencyKey("key-a-1");
        differentPlan.setCreatedAt(now);
        differentPlan.setUpdatedAt(now);
        shoppingListDraftRepository.save(differentPlan);

        Optional<ShoppingListDraftEntity> result = shoppingListDraftRepository
                .findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc("user-a", "plan-2", "key-a-1");

        assertTrue(result.isPresent());
        assertEquals("draft-a-2", result.get().getId());
    }

    @Test
    void findTopByUserIdAndPlanIdAndIdempotencyKeyFiltersPerIdempotencyKey() {
        Instant now = Instant.now();

        ShoppingListDraftEntity differentKey = new ShoppingListDraftEntity();
        differentKey.setId("draft-a-3");
        differentKey.setUserId("user-a");
        differentKey.setPlanId("plan-1");
        differentKey.setIdempotencyKey("different-key");
        differentKey.setCreatedAt(now);
        differentKey.setUpdatedAt(now);
        shoppingListDraftRepository.save(differentKey);

        Optional<ShoppingListDraftEntity> result = shoppingListDraftRepository
                .findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc("user-a", "plan-1", "different-key");

        assertTrue(result.isPresent());
        assertEquals("draft-a-3", result.get().getId());
    }

    @Test
    void findTopByUserIdAndPlanIdAndIdempotencyKeyReturnsEmptyWhenNoMatch() {
        Optional<ShoppingListDraftEntity> result = shoppingListDraftRepository
                .findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc("user-a", "plan-1", "nonexistent-key");

        assertFalse(result.isPresent());
    }

    @Test
    void findTopByUserIdAndPlanIdAndIdempotencyKeyReturnsEmptyWhenUserMismatch() {
        Optional<ShoppingListDraftEntity> result = shoppingListDraftRepository
                .findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc("user-c", "plan-1", "key-a-1");

        assertFalse(result.isPresent());
    }

    @Test
    void saveAndRetrievePersistsAllFields() {
        Instant created = Instant.parse("2026-05-01T10:00:00Z");
        Instant updated = Instant.parse("2026-05-01T15:00:00Z");

        ShoppingListDraftEntity draft = new ShoppingListDraftEntity();
        draft.setId("draft-full");
        draft.setUserId("user-full");
        draft.setPlanId("plan-full");
        draft.setIdempotencyKey("key-full");
        draft.setCreatedAt(created);
        draft.setUpdatedAt(updated);

        shoppingListDraftRepository.save(draft);
        Optional<ShoppingListDraftEntity> retrieved = shoppingListDraftRepository.findByIdAndUserId("draft-full", "user-full");

        assertTrue(retrieved.isPresent());
        assertEquals("draft-full", retrieved.get().getId());
        assertEquals("user-full", retrieved.get().getUserId());
        assertEquals("plan-full", retrieved.get().getPlanId());
        assertEquals("key-full", retrieved.get().getIdempotencyKey());
        assertEquals(created, retrieved.get().getCreatedAt());
        assertEquals(updated, retrieved.get().getUpdatedAt());
    }

    @Test
    void deleteRemovesEntity() {
        shoppingListDraftRepository.delete(draftUserA);
        Optional<ShoppingListDraftEntity> result = shoppingListDraftRepository.findByIdAndUserId("draft-a-1", "user-a");
        assertFalse(result.isPresent());
    }

    @Test
    void deleteOnlyAffectsTargetEntity() {
        shoppingListDraftRepository.delete(draftUserA);
        Optional<ShoppingListDraftEntity> draftAResult = shoppingListDraftRepository.findByIdAndUserId("draft-a-1", "user-a");
        Optional<ShoppingListDraftEntity> draftBResult = shoppingListDraftRepository.findByIdAndUserId("draft-b-1", "user-b");

        assertFalse(draftAResult.isPresent());
        assertTrue(draftBResult.isPresent());
    }
}
