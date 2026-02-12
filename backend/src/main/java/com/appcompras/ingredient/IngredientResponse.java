package com.appcompras.ingredient;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;

import java.util.List;
import java.util.Set;

public record IngredientResponse(
        String id,
        String name,
        String preferredLabel,
        List<String> aliases,
        MeasurementType measurementType,
        Set<Unit> allowedUnits,
        boolean custom
) {
    public static IngredientResponse from(IngredientCatalogItem item) {
        return new IngredientResponse(
                item.ingredientId(),
                item.displayName(),
                item.displayName(),
                List.of(item.displayName()),
                item.measurementType(),
                item.allowedUnits(),
                item.ingredientId().startsWith("custom-")
        );
    }

    public static IngredientResponse from(IngredientCatalogItem item, String preferredLabel, List<String> aliases) {
        return new IngredientResponse(
                item.ingredientId(),
                item.displayName(),
                preferredLabel,
                aliases,
                item.measurementType(),
                item.allowedUnits(),
                item.ingredientId().startsWith("custom-")
        );
    }
}
