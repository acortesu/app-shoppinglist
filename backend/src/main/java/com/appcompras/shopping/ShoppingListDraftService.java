package com.appcompras.shopping;

import com.appcompras.domain.ShoppingListItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShoppingListDraftService {

    private final ShoppingListDraftRepository shoppingListDraftRepository;

    public ShoppingListDraftService(ShoppingListDraftRepository shoppingListDraftRepository) {
        this.shoppingListDraftRepository = shoppingListDraftRepository;
    }

    @Transactional
    public ShoppingListDraft createFromGenerated(String planId, List<ShoppingListItem> generatedItems) {
        Instant now = Instant.now();

        ShoppingListDraftEntity entity = new ShoppingListDraftEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPlanId(planId);
        entity.setItems(ShoppingListDraftEntityMapper.toEmbeddables(
                generatedItems.stream().map(this::toDraftItem).toList()
        ));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        ShoppingListDraftEntity saved = shoppingListDraftRepository.save(entity);
        return ShoppingListDraftEntityMapper.toDomain(saved);
    }

    @Transactional(readOnly = true)
    public Optional<ShoppingListDraft> findById(String id) {
        return shoppingListDraftRepository.findById(id)
                .map(ShoppingListDraftEntityMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public List<ShoppingListDraft> findAll() {
        return shoppingListDraftRepository.findAllByOrderByCreatedAtDescIdAsc().stream()
                .map(ShoppingListDraftEntityMapper::toDomain)
                .toList();
    }

    @Transactional
    public Optional<ShoppingListDraft> replaceItems(String id, UpdateShoppingListRequest request) {
        Optional<ShoppingListDraftEntity> existingOpt = shoppingListDraftRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        ShoppingListDraftEntity existing = existingOpt.get();
        existing.setItems(ShoppingListDraftEntityMapper.toEmbeddables(
                request.items().stream().map(this::fromRequestItem).collect(Collectors.toList())
        ));
        existing.setUpdatedAt(Instant.now());

        ShoppingListDraftEntity saved = shoppingListDraftRepository.save(existing);
        return Optional.of(ShoppingListDraftEntityMapper.toDomain(saved));
    }

    @Transactional
    public boolean deleteById(String id) {
        if (!shoppingListDraftRepository.existsById(id)) {
            return false;
        }
        shoppingListDraftRepository.deleteById(id);
        return true;
    }

    private ShoppingListDraftItem toDraftItem(ShoppingListItem item) {
        return new ShoppingListDraftItem(
                UUID.randomUUID().toString(),
                item.ingredientId(),
                item.name(),
                item.requiredBaseAmount(),
                item.baseUnit().name(),
                item.suggestedPackages(),
                item.packageAmount(),
                item.packageUnit().name(),
                false
        );
    }

    private ShoppingListDraftItem fromRequestItem(UpdateShoppingListRequest.ItemInput item) {
        return new ShoppingListDraftItem(
                item.id() == null || item.id().isBlank() ? UUID.randomUUID().toString() : item.id(),
                item.ingredientId(),
                item.name().trim(),
                item.quantity(),
                item.unit().trim(),
                item.suggestedPackages(),
                item.packageAmount(),
                item.packageUnit(),
                item.manual()
        );
    }
}
