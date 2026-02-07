package com.appcompras.shopping;

import java.time.Instant;
import java.util.List;

public record ShoppingListResponse(
        String id,
        String planId,
        List<ShoppingListDraftItem> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static ShoppingListResponse from(ShoppingListDraft draft) {
        return new ShoppingListResponse(
                draft.id(),
                draft.planId(),
                draft.items(),
                draft.createdAt(),
                draft.updatedAt()
        );
    }
}
