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
class ShoppingListDraftNotNullConstraintTest {

    @Autowired
    private ShoppingListDraftRepository shoppingListDraftRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        shoppingListDraftRepository.deleteAll();
    }

    @Test
    void notNullConstraintOnUserId() {
        ShoppingListDraftEntity draft = new ShoppingListDraftEntity();
        draft.setId("draft-1");
        draft.setUserId(null);
        draft.setPlanId("plan-1");
        draft.setIdempotencyKey("key");
        draft.setCreatedAt(Instant.now());
        draft.setUpdatedAt(Instant.now());

        assertThrows(ConstraintViolationException.class, () -> {
            shoppingListDraftRepository.save(draft);
            entityManager.flush();
        });
    }

    @Test
    void notNullConstraintOnPlanId() {
        ShoppingListDraftEntity draft = new ShoppingListDraftEntity();
        draft.setId("draft-1");
        draft.setUserId("user-a");
        draft.setPlanId(null);
        draft.setIdempotencyKey("key");
        draft.setCreatedAt(Instant.now());
        draft.setUpdatedAt(Instant.now());

        assertThrows(ConstraintViolationException.class, () -> {
            shoppingListDraftRepository.save(draft);
            entityManager.flush();
        });
    }

    @Test
    void notNullConstraintOnCreatedAt() {
        ShoppingListDraftEntity draft = new ShoppingListDraftEntity();
        draft.setId("draft-1");
        draft.setUserId("user-a");
        draft.setPlanId("plan-1");
        draft.setIdempotencyKey("key");
        draft.setCreatedAt(null);
        draft.setUpdatedAt(Instant.now());

        assertThrows(ConstraintViolationException.class, () -> {
            shoppingListDraftRepository.save(draft);
            entityManager.flush();
        });
    }

    @Test
    void notNullConstraintOnUpdatedAt() {
        ShoppingListDraftEntity draft = new ShoppingListDraftEntity();
        draft.setId("draft-1");
        draft.setUserId("user-a");
        draft.setPlanId("plan-1");
        draft.setIdempotencyKey("key");
        draft.setCreatedAt(Instant.now());
        draft.setUpdatedAt(null);

        assertThrows(ConstraintViolationException.class, () -> {
            shoppingListDraftRepository.save(draft);
            entityManager.flush();
        });
    }
}
