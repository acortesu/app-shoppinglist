package com.appcompras.ingredient;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;

import java.util.Set;

public record IngredientResponse(
        String id,
        String name,
        MeasurementType measurementType,
        Set<Unit> allowedUnits,
        boolean custom
) {
    public static IngredientResponse from(IngredientCatalogItem item) {
        return new IngredientResponse(
                item.ingredientId(),
                item.displayName(),
                item.measurementType(),
                item.allowedUnits(),
                item.ingredientId().startsWith("custom-")
        );
    }
}
