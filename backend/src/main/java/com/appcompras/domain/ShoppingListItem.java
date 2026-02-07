package com.appcompras.domain;

public record ShoppingListItem(
        String ingredientId,
        String name,
        double requiredBaseAmount,
        Unit baseUnit,
        int suggestedPackages,
        double packageAmount,
        Unit packageUnit
) {
}
