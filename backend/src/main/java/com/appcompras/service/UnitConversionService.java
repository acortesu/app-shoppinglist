package com.appcompras.service;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UnitConversionService {

    private final IngredientCatalogService catalogService;

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
            case WEIGHT -> convertWeightToGrams(item, quantity, unit);
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

    public double packageBaseAmount(MeasurementType type, double amount, Unit unit) {
        validatePackageUnit(type, unit);
        return switch (type) {
            case WEIGHT -> convertWeightPackageToGrams(amount, unit);
            case VOLUME -> convertVolumePackageToMilliliters(amount, unit);
            case UNIT -> amount;
            case TO_TASTE -> 0.0;
        };
    }

    private void validatePackageUnit(MeasurementType type, Unit unit) {
        switch (type) {
            case WEIGHT -> {
                if (unit != Unit.GRAM && unit != Unit.KILOGRAM) {
                    throw new IllegalArgumentException("Package unit for WEIGHT must be GRAM or KILOGRAM, got: " + unit);
                }
            }
            case VOLUME -> {
                if (unit != Unit.MILLILITER && unit != Unit.LITER) {
                    throw new IllegalArgumentException("Package unit for VOLUME must be MILLILITER or LITER, got: " + unit);
                }
            }
            case UNIT -> {
                if (unit != Unit.PIECE) {
                    throw new IllegalArgumentException("Package unit for UNIT must be PIECE, got: " + unit);
                }
            }
            case TO_TASTE -> {
                // TO_TASTE packages always yield 0
            }
        }
    }

    private double convertWeightPackageToGrams(double amount, Unit unit) {
        return switch (unit) {
            case GRAM -> amount;
            case KILOGRAM -> amount * 1000.0;
            default -> throw new IllegalArgumentException("Unsupported package WEIGHT unit: " + unit);
        };
    }

    private double convertVolumePackageToMilliliters(double amount, Unit unit) {
        return switch (unit) {
            case MILLILITER -> amount;
            case LITER -> amount * 1000.0;
            default -> throw new IllegalArgumentException("Unsupported package VOLUME unit: " + unit);
        };
    }

    private double convertWeightToGrams(IngredientCatalogItem item, double quantity, Unit unit) {
        return switch (unit) {
            case GRAM -> quantity;
            case KILOGRAM -> quantity * 1000.0;
            case CUP, TABLESPOON, TEASPOON, PINCH -> fromSpecificRule(item, unit, quantity);
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

    private double fromSpecificRule(IngredientCatalogItem item, Unit unit, double quantity) {
        Map<Unit, Double> rules = item.densityRules();
        if (rules == null || !rules.containsKey(unit)) {
            throw new IllegalArgumentException(
                    "Missing ingredient specific conversion for ingredient " + item.ingredientId() + " and unit " + unit);
        }
        return quantity * rules.get(unit);
    }
}
