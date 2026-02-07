package com.appcompras.shopping;

public record ShoppingListDraftItem(
        String id,
        String ingredientId,
        String name,
        double quantity,
        String unit,
        Integer suggestedPackages,
        Double packageAmount,
        String packageUnit,
        boolean manual
) {
}
