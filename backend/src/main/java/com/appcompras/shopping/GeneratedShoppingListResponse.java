package com.appcompras.shopping;

import com.appcompras.domain.ShoppingListItem;

import java.util.List;

public record GeneratedShoppingListResponse(
        String planId,
        List<ShoppingListItem> items
) {
}
