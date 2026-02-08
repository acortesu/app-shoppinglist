package com.appcompras.shopping;

import com.appcompras.config.BusinessRuleException;
import com.appcompras.domain.ShoppingListItem;
import com.appcompras.security.CurrentUserProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class ShoppingListDraftService {

    private final ShoppingListDraftRepository shoppingListDraftRepository;
    private final CurrentUserProvider currentUserProvider;

    public ShoppingListDraftService(
            ShoppingListDraftRepository shoppingListDraftRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.shoppingListDraftRepository = shoppingListDraftRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ShoppingListDraft createFromGenerated(String planId, List<ShoppingListItem> generatedItems, String idempotencyKey) {
        String userId = currentUserProvider.getCurrentUserId();
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedIdempotencyKey != null) {
            Optional<ShoppingListDraftEntity> existing = shoppingListDraftRepository
                    .findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc(
                            userId, planId, normalizedIdempotencyKey);
            if (existing.isPresent()) {
                return ShoppingListDraftEntityMapper.toDomain(existing.get());
            }
        }

        Instant now = Instant.now();

        ShoppingListDraftEntity entity = new ShoppingListDraftEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setPlanId(planId);
        entity.setIdempotencyKey(normalizedIdempotencyKey);
        List<ShoppingListDraftItem> generatedDraftItems = new ArrayList<>();
        for (int i = 0; i < generatedItems.size(); i++) {
            generatedDraftItems.add(toDraftItem(generatedItems.get(i), i));
        }
        entity.setItems(ShoppingListDraftEntityMapper.toEmbeddables(generatedDraftItems));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        try {
            ShoppingListDraftEntity saved = shoppingListDraftRepository.save(entity);
            return ShoppingListDraftEntityMapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            if (normalizedIdempotencyKey == null) {
                throw ex;
            }
            return shoppingListDraftRepository
                    .findTopByUserIdAndPlanIdAndIdempotencyKeyOrderByCreatedAtDesc(
                            userId,
                            planId,
                            normalizedIdempotencyKey
                    )
                    .map(ShoppingListDraftEntityMapper::toDomain)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ShoppingListDraft> findById(String id) {
        String userId = currentUserProvider.getCurrentUserId();
        return shoppingListDraftRepository.findByIdAndUserId(id, userId)
                .map(ShoppingListDraftEntityMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public List<ShoppingListDraft> findAll() {
        String userId = currentUserProvider.getCurrentUserId();
        return shoppingListDraftRepository.findAllByUserIdOrderByCreatedAtDescIdAsc(userId).stream()
                .map(ShoppingListDraftEntityMapper::toDomain)
                .toList();
    }

    @Transactional
    public Optional<ShoppingListDraft> replaceItems(String id, UpdateShoppingListRequest request) {
        String userId = currentUserProvider.getCurrentUserId();
        Optional<ShoppingListDraftEntity> existingOpt = shoppingListDraftRepository.findByIdAndUserId(id, userId);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        ShoppingListDraftEntity existing = existingOpt.get();
        validateItems(request.items());
        existing.setItems(ShoppingListDraftEntityMapper.toEmbeddables(
                IntStream.range(0, request.items().size())
                        .mapToObj(i -> fromRequestItem(request.items().get(i), i))
                        .toList()
        ));
        existing.setUpdatedAt(Instant.now());

        ShoppingListDraftEntity saved = shoppingListDraftRepository.save(existing);
        return Optional.of(ShoppingListDraftEntityMapper.toDomain(saved));
    }

    @Transactional
    public boolean deleteById(String id) {
        String userId = currentUserProvider.getCurrentUserId();
        Optional<ShoppingListDraftEntity> existingOpt = shoppingListDraftRepository.findByIdAndUserId(id, userId);
        if (existingOpt.isEmpty()) {
            return false;
        }
        shoppingListDraftRepository.delete(existingOpt.get());
        return true;
    }

    private ShoppingListDraftItem toDraftItem(ShoppingListItem item, int index) {
        return new ShoppingListDraftItem(
                UUID.randomUUID().toString(),
                item.ingredientId(),
                item.name(),
                item.requiredBaseAmount(),
                item.baseUnit().name(),
                item.suggestedPackages(),
                item.packageAmount(),
                item.packageUnit().name(),
                false,
                false,
                null,
                index
        );
    }

    private ShoppingListDraftItem fromRequestItem(UpdateShoppingListRequest.ItemInput item, int index) {
        return new ShoppingListDraftItem(
                item.id() == null || item.id().isBlank() ? UUID.randomUUID().toString() : item.id(),
                item.ingredientId(),
                item.name().trim(),
                item.quantity(),
                item.unit().trim(),
                item.suggestedPackages(),
                item.packageAmount(),
                item.packageUnit(),
                item.manual(),
                item.bought() != null && item.bought(),
                item.note() == null ? null : item.note().trim(),
                item.sortOrder() == null ? index : item.sortOrder()
        );
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateItems(List<UpdateShoppingListRequest.ItemInput> items) {
        for (int i = 0; i < items.size(); i++) {
            UpdateShoppingListRequest.ItemInput item = items.get(i);
            String context = "items[" + i + "]";

            if (!item.manual() && (item.ingredientId() == null || item.ingredientId().isBlank())) {
                throw new BusinessRuleException(
                        "SHOPPING_ITEM_INGREDIENT_REQUIRED",
                        context + ".ingredientId is required when manual=false"
                );
            }

            boolean hasSuggestedPackages = item.suggestedPackages() != null;
            boolean hasPackageAmount = item.packageAmount() != null;
            boolean hasPackageUnit = item.packageUnit() != null && !item.packageUnit().isBlank();
            boolean hasPackagingData = hasSuggestedPackages || hasPackageAmount || hasPackageUnit;
            boolean hasCompletePackagingData = hasSuggestedPackages && hasPackageAmount && hasPackageUnit;

            if (hasPackagingData && !hasCompletePackagingData) {
                throw new BusinessRuleException(
                        "SHOPPING_ITEM_PACKAGE_FIELDS_INCOMPLETE",
                        context + " package fields must be sent together: suggestedPackages, packageAmount, packageUnit"
                );
            }

            if (hasSuggestedPackages && item.suggestedPackages() <= 0) {
                throw new BusinessRuleException("SHOPPING_ITEM_INVALID_SUGGESTED_PACKAGES", context + ".suggestedPackages must be > 0");
            }

            if (hasPackageAmount && item.packageAmount() <= 0) {
                throw new BusinessRuleException("SHOPPING_ITEM_INVALID_PACKAGE_AMOUNT", context + ".packageAmount must be > 0");
            }

            if (item.note() != null && item.note().length() > 280) {
                throw new BusinessRuleException("SHOPPING_ITEM_NOTE_TOO_LONG", context + ".note max length is 280");
            }

            if (item.sortOrder() != null && item.sortOrder() < 0) {
                throw new BusinessRuleException("SHOPPING_ITEM_INVALID_SORT_ORDER", context + ".sortOrder must be >= 0");
            }
        }
    }
}
