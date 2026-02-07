package com.appcompras.shopping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ShoppingListDraftEntityMapper {

    private ShoppingListDraftEntityMapper() {
    }

    public static ShoppingListDraft toDomain(ShoppingListDraftEntity entity) {
        List<ShoppingListDraftItem> items = entity.getItems().stream()
                .map(i -> new ShoppingListDraftItem(
                        i.getId(),
                        i.getIngredientId(),
                        i.getName(),
                        i.getQuantity(),
                        i.getUnit(),
                        i.getSuggestedPackages(),
                        i.getPackageAmount(),
                        i.getPackageUnit(),
                        i.isManual()
                ))
                .toList();

        return new ShoppingListDraft(
                entity.getId(),
                entity.getPlanId(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static List<ShoppingListDraftItemEmbeddable> toEmbeddables(List<ShoppingListDraftItem> items) {
        return items.stream()
                .map(i -> new ShoppingListDraftItemEmbeddable(
                        i.id(),
                        i.ingredientId(),
                        i.name(),
                        i.quantity(),
                        i.unit(),
                        i.suggestedPackages(),
                        i.packageAmount(),
                        i.packageUnit(),
                        i.manual()
                ))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
