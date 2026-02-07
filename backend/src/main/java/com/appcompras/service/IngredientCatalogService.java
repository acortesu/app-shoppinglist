package com.appcompras.service;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class IngredientCatalogService {

    private final Map<String, IngredientCatalogItem> catalog = Map.of(
            "rice", new IngredientCatalogItem("rice", "Rice", MeasurementType.WEIGHT,
                    Set.of(Unit.GRAM, Unit.KILOGRAM, Unit.CUP), 1.0, Unit.KILOGRAM),
            "oil", new IngredientCatalogItem("oil", "Oil", MeasurementType.VOLUME,
                    Set.of(Unit.MILLILITER, Unit.LITER, Unit.TABLESPOON, Unit.TEASPOON), 500.0, Unit.MILLILITER),
            "tomato", new IngredientCatalogItem("tomato", "Tomato", MeasurementType.UNIT,
                    Set.of(Unit.PIECE), 1.0, Unit.PIECE),
            "salt", new IngredientCatalogItem("salt", "Salt", MeasurementType.WEIGHT,
                    Set.of(Unit.GRAM, Unit.KILOGRAM, Unit.PINCH, Unit.TO_TASTE), 500.0, Unit.GRAM),
            "egg", new IngredientCatalogItem("egg", "Egg", MeasurementType.UNIT,
                    Set.of(Unit.PIECE), 12.0, Unit.PIECE)
    );

    public Optional<IngredientCatalogItem> findById(String ingredientId) {
        return Optional.ofNullable(catalog.get(ingredientId));
    }
}
