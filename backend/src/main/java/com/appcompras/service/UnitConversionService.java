package com.appcompras.service;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UnitConversionService {

    private final IngredientCatalogService catalogService;

    private final Map<String, Map<Unit, Double>> ingredientSpecificToBaseFactors = Map.of(
            "rice", Map.of(Unit.CUP, 180.0),
            "salt", Map.of(Unit.PINCH, 0.3)
    );

    public UnitConversionService(IngredientCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public double toBaseAmount(String ingredientId, double quantity, Unit unit) {
        IngredientCatalogItem item = catalogService.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ingredient: " + ingredientId));

        if (!item.allowedUnits().contains(unit)) {
            throw new IllegalArgumentException("Unit " + unit + " is not allowed for ingredient " + ingredientId);
        }

        if (item.measurementType() == MeasurementType.TO_TASTE || unit == Unit.TO_TASTE) {
            return 0.0;
        }

        return switch (item.measurementType()) {
            case WEIGHT -> convertWeightToGrams(ingredientId, quantity, unit);
            case VOLUME -> convertVolumeToMilliliters(quantity, unit);
            case UNIT -> convertUnits(quantity, unit);
            case TO_TASTE -> 0.0;
        };
    }

    public Unit baseUnitFor(String ingredientId) {
        IngredientCatalogItem item = catalogService.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ingredient: " + ingredientId));

        return switch (item.measurementType()) {
            case WEIGHT -> Unit.GRAM;
            case VOLUME -> Unit.MILLILITER;
            case UNIT -> Unit.PIECE;
            case TO_TASTE -> Unit.TO_TASTE;
        };
    }

    private double convertWeightToGrams(String ingredientId, double quantity, Unit unit) {
        return switch (unit) {
            case GRAM -> quantity;
            case KILOGRAM -> quantity * 1000.0;
            case CUP, TABLESPOON, TEASPOON, PINCH -> fromSpecificRule(ingredientId, unit, quantity);
            default -> throw new IllegalArgumentException("Unsupported WEIGHT unit: " + unit);
        };
    }

    private double convertVolumeToMilliliters(double quantity, Unit unit) {
        return switch (unit) {
            case MILLILITER -> quantity;
            case LITER -> quantity * 1000.0;
            case TABLESPOON -> quantity * 15.0;
            case TEASPOON -> quantity * 5.0;
            case CUP -> quantity * 240.0;
            default -> throw new IllegalArgumentException("Unsupported VOLUME unit: " + unit);
        };
    }

    private double convertUnits(double quantity, Unit unit) {
        if (unit != Unit.PIECE) {
            throw new IllegalArgumentException("Unsupported UNIT type unit: " + unit);
        }
        return quantity;
    }

    private double fromSpecificRule(String ingredientId, Unit unit, double quantity) {
        Map<Unit, Double> rules = ingredientSpecificToBaseFactors.get(ingredientId);
        if (rules == null || !rules.containsKey(unit)) {
            throw new IllegalArgumentException(
                    "Missing ingredient specific conversion for ingredient " + ingredientId + " and unit " + unit);
        }
        return quantity * rules.get(unit);
    }
}
