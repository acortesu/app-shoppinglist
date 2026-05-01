package com.appcompras.shopping;

import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class ShoppingListDraftConstraintTest {

    @Autowired
    private ShoppingListDraftRepository shoppingListDraftRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        shoppingListDraftRepository.deleteAll();
    }

    @Test
    void uniqueConstraintOnUserIdPlanIdIdempotencyKey() {
        Instant now = Instant.now();

        ShoppingListDraftEntity first = new ShoppingListDraftEntity();
        first.setId("draft-1");
        first.setUserId("user-a");
        first.setPlanId("plan-1");
        first.setIdempotencyKey("idempotent-key");
        first.setCreatedAt(now);
        first.setUpdatedAt(now);
        shoppingListDraftRepository.save(first);
        entityManager.flush();

        ShoppingListDraftEntity duplicate = new ShoppingListDraftEntity();
        duplicate.setId("draft-2");
        duplicate.setUserId("user-a");
        duplicate.setPlanId("plan-1");
        duplicate.setIdempotencyKey("idempotent-key");
        duplicate.setCreatedAt(now);
        duplicate.setUpdatedAt(now);

        assertThrows(ConstraintViolationException.class, () -> {
            shoppingListDraftRepository.save(duplicate);
            entityManager.flush();
        });
    }

    @Test
    void uniqueConstraintAllowsDifferentUsers() {
        Instant now = Instant.now();

        ShoppingListDraftEntity userA = new ShoppingListDraftEntity();
        userA.setId("draft-a");
        userA.setUserId("user-a");
        userA.setPlanId("plan-1");
        userA.setIdempotencyKey("same-key");
        userA.setCreatedAt(now);
        userA.setUpdatedAt(now);
        shoppingListDraftRepository.save(userA);

        ShoppingListDraftEntity userB = new ShoppingListDraftEntity();
        userB.setId("draft-b");
        userB.setUserId("user-b");
        userB.setPlanId("plan-1");
        userB.setIdempotencyKey("same-key");
        userB.setCreatedAt(now);
        userB.setUpdatedAt(now);

        shoppingListDraftRepository.save(userB);
    }

    @Test
    void uniqueConstraintAllowsDifferentPlans() {
        Instant now = Instant.now();

        ShoppingListDraftEntity plan1 = new ShoppingListDraftEntity();
        plan1.setId("draft-1");
        plan1.setUserId("user-a");
        plan1.setPlanId("plan-1");
        plan1.setIdempotencyKey("same-key");
        plan1.setCreatedAt(now);
        plan1.setUpdatedAt(now);
        shoppingListDraftRepository.save(plan1);

        ShoppingListDraftEntity plan2 = new ShoppingListDraftEntity();
        plan2.setId("draft-2");
        plan2.setUserId("user-a");
        plan2.setPlanId("plan-2");
        plan2.setIdempotencyKey("same-key");
        plan2.setCreatedAt(now);
        plan2.setUpdatedAt(now);

        shoppingListDraftRepository.save(plan2);
    }

    @Test
    void uniqueConstraintAllowsNullIdempotencyKey() {
        Instant now = Instant.now();

        ShoppingListDraftEntity first = new ShoppingListDraftEntity();
        first.setId("draft-1");
        first.setUserId("user-a");
        first.setPlanId("plan-1");
        first.setIdempotencyKey(null);
        first.setCreatedAt(now);
        first.setUpdatedAt(now);
        shoppingListDraftRepository.save(first);

        ShoppingListDraftEntity second = new ShoppingListDraftEntity();
        second.setId("draft-2");
        second.setUserId("user-a");
        second.setPlanId("plan-1");
        second.setIdempotencyKey(null);
        second.setCreatedAt(now);
        second.setUpdatedAt(now);

        shoppingListDraftRepository.save(second);
    }
}
