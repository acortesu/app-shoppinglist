package com.appcompras.domain;

import java.util.Map;
import java.util.Set;

public record IngredientCatalogItem(
        String ingredientId,
        String displayName,
        MeasurementType measurementType,
        Set<Unit> allowedUnits,
        double suggestedPurchaseAmount,
        Unit suggestedPurchaseUnit,
        Map<Unit, Double> densityRules
) {
    public IngredientCatalogItem {
        densityRules = densityRules == null ? Map.of() : Map.copyOf(densityRules);
    }
}
