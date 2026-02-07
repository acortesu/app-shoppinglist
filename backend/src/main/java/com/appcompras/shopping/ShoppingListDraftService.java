package com.appcompras.shopping;

import com.appcompras.domain.ShoppingListItem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ShoppingListDraftService {

    private final Map<String, ShoppingListDraft> drafts = new ConcurrentHashMap<>();

    public ShoppingListDraft createFromGenerated(String planId, List<ShoppingListItem> generatedItems) {
        Instant now = Instant.now();
        ShoppingListDraft draft = new ShoppingListDraft(
                UUID.randomUUID().toString(),
                planId,
                generatedItems.stream().map(this::toDraftItem).toList(),
                now,
                now
        );
        drafts.put(draft.id(), draft);
        return draft;
    }

    public Optional<ShoppingListDraft> findById(String id) {
        return Optional.ofNullable(drafts.get(id));
    }

    public Optional<ShoppingListDraft> replaceItems(String id, UpdateShoppingListRequest request) {
        ShoppingListDraft updated = drafts.computeIfPresent(id, (ignored, existing) -> new ShoppingListDraft(
                existing.id(),
                existing.planId(),
                request.items().stream().map(this::fromRequestItem).collect(Collectors.toList()),
                existing.createdAt(),
                Instant.now()
        ));

        return Optional.ofNullable(updated);
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
