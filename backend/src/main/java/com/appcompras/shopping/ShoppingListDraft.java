package com.appcompras.shopping;

import java.time.Instant;
import java.util.List;

public record ShoppingListDraft(
        String id,
        String planId,
        List<ShoppingListDraftItem> items,
        Instant createdAt,
        Instant updatedAt
) {
}
