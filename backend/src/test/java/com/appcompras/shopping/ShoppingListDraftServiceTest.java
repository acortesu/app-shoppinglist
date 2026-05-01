package com.appcompras.shopping;

import com.appcompras.config.BusinessRuleException;
import com.appcompras.domain.ShoppingListItem;
import com.appcompras.domain.Unit;
import com.appcompras.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingListDraftServiceTest {

    @Mock
    private ShoppingListDraftRepository shoppingListDraftRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private ShoppingListDraftService shoppingListDraftService;

    @BeforeEach
    void setUp() {
        shoppingListDraftService = new ShoppingListDraftService(shoppingListDraftRepository, currentUserProvider);
        when(currentUserProvider.getCurrentUserId()).thenReturn("test-user-id");
    }

    @Test
    void validateItemsRejectsNonManualItemWithoutIngredientId() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                null,
                "Item Name",
                100.0,
                "GRAM",
                1,
                500.0,
                "KILOGRAM",
                false,
                false,
                null,
                0
        );

        assertThrows(BusinessRuleException.class, () ->
                shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)))
        );
    }

    @Test
    void validateItemsRejectsNonManualItemWithBlankIngredientId() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "   ",
                "Item Name",
                100.0,
                "GRAM",
                1,
                500.0,
                "KILOGRAM",
                false,
                false,
                null,
                0
        );

        assertThrows(BusinessRuleException.class, () ->
                shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)))
        );
    }

    @Test
    void validateItemsAcceptsManualItemWithoutIngredientId() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));
        when(shoppingListDraftRepository.save(any(ShoppingListDraftEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                null,
                "Manual Item",
                100.0,
                "GRAM",
                null,
                null,
                null,
                true,
                false,
                null,
                0
        );

        Optional<ShoppingListDraft> result = shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)));
        assertTrue(result.isPresent());
    }

    @Test
    void validateItemsRejectsPartialPackageFields() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "rice",
                "Rice",
                100.0,
                "GRAM",
                1,
                null,
                null,
                false,
                false,
                null,
                0
        );

        assertThrows(BusinessRuleException.class, () ->
                shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)))
        );
    }

    @Test
    void validateItemsRejectsZeroSuggestedPackages() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "rice",
                "Rice",
                100.0,
                "GRAM",
                0,
                500.0,
                "KILOGRAM",
                false,
                false,
                null,
                0
        );

        assertThrows(BusinessRuleException.class, () ->
                shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)))
        );
    }

    @Test
    void validateItemsRejectsNegativeSuggestedPackages() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "rice",
                "Rice",
                100.0,
                "GRAM",
                -1,
                500.0,
                "KILOGRAM",
                false,
                false,
                null,
                0
        );

        assertThrows(BusinessRuleException.class, () ->
                shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)))
        );
    }

    @Test
    void validateItemsRejectsZeroPackageAmount() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "rice",
                "Rice",
                100.0,
                "GRAM",
                1,
                0.0,
                "KILOGRAM",
                false,
                false,
                null,
                0
        );

        assertThrows(BusinessRuleException.class, () ->
                shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)))
        );
    }

    @Test
    void validateItemsRejectsNegativePackageAmount() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "rice",
                "Rice",
                100.0,
                "GRAM",
                1,
                -5.0,
                "KILOGRAM",
                false,
                false,
                null,
                0
        );

        assertThrows(BusinessRuleException.class, () ->
                shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)))
        );
    }

    @Test
    void validateItemsRejectsNoteTooLong() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));

        String longNote = "a".repeat(281);
        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "rice",
                "Rice",
                100.0,
                "GRAM",
                1,
                500.0,
                "KILOGRAM",
                false,
                false,
                longNote,
                0
        );

        assertThrows(BusinessRuleException.class, () ->
                shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)))
        );
    }

    @Test
    void validateItemsAcceptsNoteAtMaxLength() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));
        when(shoppingListDraftRepository.save(any(ShoppingListDraftEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String maxNote = "a".repeat(280);
        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "rice",
                "Rice",
                100.0,
                "GRAM",
                1,
                500.0,
                "KILOGRAM",
                false,
                false,
                maxNote,
                0
        );

        Optional<ShoppingListDraft> result = shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)));
        assertTrue(result.isPresent());
    }

    @Test
    void validateItemsRejectsNegativeSortOrder() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "rice",
                "Rice",
                100.0,
                "GRAM",
                1,
                500.0,
                "KILOGRAM",
                false,
                false,
                null,
                -1
        );

        assertThrows(BusinessRuleException.class, () ->
                shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)))
        );
    }

    @Test
    void validateItemsAcceptsZeroSortOrder() {
        ShoppingListDraftEntity existing = new ShoppingListDraftEntity();
        existing.setId("draft-id");
        existing.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(existing));
        when(shoppingListDraftRepository.save(any(ShoppingListDraftEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateShoppingListRequest.ItemInput item = new UpdateShoppingListRequest.ItemInput(
                "item-1",
                "rice",
                "Rice",
                100.0,
                "GRAM",
                1,
                500.0,
                "KILOGRAM",
                false,
                false,
                null,
                0
        );

        Optional<ShoppingListDraft> result = shoppingListDraftService.replaceItems("draft-id", new UpdateShoppingListRequest(List.of(item)));
        assertTrue(result.isPresent());
    }

    @Test
    void normalizeIdempotencyKeyReturnsNullForNull() {
        ShoppingListItem generatedItem = new ShoppingListItem("rice", "Rice", 180.0, Unit.GRAM, 1, 500.0, Unit.KILOGRAM);
        ShoppingListDraftEntity savedEntity = new ShoppingListDraftEntity();
        savedEntity.setId("draft-id");
        when(shoppingListDraftRepository.save(any(ShoppingListDraftEntity.class))).thenReturn(savedEntity);

        ShoppingListDraft result = shoppingListDraftService.createFromGenerated("plan-id", List.of(generatedItem), null);
        assertEquals("draft-id", result.id());
    }

    @Test
    void normalizeIdempotencyKeyReturnsNullForEmptyString() {
        ShoppingListItem generatedItem = new ShoppingListItem("rice", "Rice", 180.0, Unit.GRAM, 1, 500.0, Unit.KILOGRAM);
        ShoppingListDraftEntity savedEntity = new ShoppingListDraftEntity();
        savedEntity.setId("draft-id");
        when(shoppingListDraftRepository.save(any(ShoppingListDraftEntity.class))).thenReturn(savedEntity);

        ShoppingListDraft result = shoppingListDraftService.createFromGenerated("plan-id", List.of(generatedItem), "");
        assertEquals("draft-id", result.id());
    }

    @Test
    void normalizeIdempotencyKeyReturnsNullForWhitespace() {
        ShoppingListItem generatedItem = new ShoppingListItem("rice", "Rice", 180.0, Unit.GRAM, 1, 500.0, Unit.KILOGRAM);
        ShoppingListDraftEntity savedEntity = new ShoppingListDraftEntity();
        savedEntity.setId("draft-id");
        when(shoppingListDraftRepository.save(any(ShoppingListDraftEntity.class))).thenReturn(savedEntity);

        ShoppingListDraft result = shoppingListDraftService.createFromGenerated("plan-id", List.of(generatedItem), "   ");
        assertEquals("draft-id", result.id());
    }

    @Test
    void normalizeIdempotencyKeyReturnsTrimmedValue() {
        ShoppingListItem generatedItem = new ShoppingListItem("rice", "Rice", 180.0, Unit.GRAM, 1, 500.0, Unit.KILOGRAM);
        ShoppingListDraftEntity savedEntity = new ShoppingListDraftEntity();
        savedEntity.setId("draft-id");
        savedEntity.setIdempotencyKey("idempotent-key");
        when(shoppingListDraftRepository.save(any(ShoppingListDraftEntity.class))).thenReturn(savedEntity);

        ShoppingListDraft result = shoppingListDraftService.createFromGenerated("plan-id", List.of(generatedItem), "  idempotent-key  ");
        assertEquals("draft-id", result.id());
    }

    @Test
    void idempotencyKeyRetryOnConflict() {
        ShoppingListItem generatedItem = new ShoppingListItem("rice", "Rice", 180.0, Unit.GRAM, 1, 500.0, Unit.KILOGRAM);
        ShoppingListDraftEntity conflictEntity = new ShoppingListDraftEntity();
        conflictEntity.setId("existing-draft-id");
        conflictEntity.setIdempotencyKey("idempotent-key");

        when(shoppingListDraftRepository.findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc(
                "test-user-id", "plan-id", "idempotent-key"
        ))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(conflictEntity));
        when(shoppingListDraftRepository.save(any(ShoppingListDraftEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        ShoppingListDraft result = shoppingListDraftService.createFromGenerated(
                "plan-id",
                List.of(generatedItem),
                "idempotent-key"
        );

        assertEquals("existing-draft-id", result.id());
    }

    @Test
    void idempotencyKeyWithoutRetryThrowsDataIntegrityViolation() {
        ShoppingListItem generatedItem = new ShoppingListItem("rice", "Rice", 180.0, Unit.GRAM, 1, 500.0, Unit.KILOGRAM);

        when(shoppingListDraftRepository.save(any(ShoppingListDraftEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        assertThrows(DataIntegrityViolationException.class, () ->
                shoppingListDraftService.createFromGenerated(
                        "plan-id",
                        List.of(generatedItem),
                        null
                )
        );
    }

    @Test
    void findByIdReturnsWhenFound() {
        ShoppingListDraftEntity entity = new ShoppingListDraftEntity();
        entity.setId("draft-id");
        entity.setUserId("test-user-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(entity));

        Optional<ShoppingListDraft> result = shoppingListDraftService.findById("draft-id");
        assertTrue(result.isPresent());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(shoppingListDraftRepository.findByIdAndUserId("nonexistent-id", "test-user-id")).thenReturn(Optional.empty());

        Optional<ShoppingListDraft> result = shoppingListDraftService.findById("nonexistent-id");
        assertFalse(result.isPresent());
    }

    @Test
    void deleteByIdReturnsTrueWhenDeleted() {
        ShoppingListDraftEntity entity = new ShoppingListDraftEntity();
        entity.setId("draft-id");
        when(shoppingListDraftRepository.findByIdAndUserId("draft-id", "test-user-id")).thenReturn(Optional.of(entity));

        boolean result = shoppingListDraftService.deleteById("draft-id");
        assertTrue(result);
    }

    @Test
    void deleteByIdReturnsFalseWhenNotFound() {
        when(shoppingListDraftRepository.findByIdAndUserId("nonexistent-id", "test-user-id")).thenReturn(Optional.empty());

        boolean result = shoppingListDraftService.deleteById("nonexistent-id");
        assertFalse(result);
    }
}
