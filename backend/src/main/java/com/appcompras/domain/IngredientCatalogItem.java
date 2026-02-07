package com.appcompras.domain;

import java.util.Set;

public record IngredientCatalogItem(
        String ingredientId,
        String displayName,
        MeasurementType measurementType,
        Set<Unit> allowedUnits,
        double suggestedPurchaseAmount,
        Unit suggestedPurchaseUnit
) {
}
