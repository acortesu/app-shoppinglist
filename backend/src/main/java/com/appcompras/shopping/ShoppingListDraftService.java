package com.appcompras.shopping;

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
}
