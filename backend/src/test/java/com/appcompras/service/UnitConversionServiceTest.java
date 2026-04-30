package com.appcompras.service;

import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitConversionServiceTest {

    private UnitConversionService conversionService;

    @BeforeEach
    void setUp() {
        IngredientCatalogService catalogService = new IngredientCatalogService(new ObjectMapper());
        conversionService = new UnitConversionService(catalogService);
    }

    @Test
    void convertsRiceCupToGrams() {
        double base = conversionService.toBaseAmount("rice", 1.0, Unit.CUP);
        assertEquals(180.0, base, 0.001);
    }

    @Test
    void convertsOilTablespoonToMilliliters() {
        double base = conversionService.toBaseAmount("oil", 2.0, Unit.TABLESPOON);
        assertEquals(30.0, base, 0.001);
    }

    @Test
    void convertsSaltPinchToGrams() {
        double base = conversionService.toBaseAmount("salt", 1.0, Unit.PINCH);
        assertEquals(0.3, base, 0.001);
    }

    @Test
    void packageBaseAmountWeightGram() {
        double base = conversionService.packageBaseAmount(MeasurementType.WEIGHT, 500.0, Unit.GRAM);
        assertEquals(500.0, base, 0.001);
    }

    @Test
    void packageBaseAmountWeightKilogram() {
        double base = conversionService.packageBaseAmount(MeasurementType.WEIGHT, 1.0, Unit.KILOGRAM);
        assertEquals(1000.0, base, 0.001);
    }

    @Test
    void packageBaseAmountVolumeMilliliter() {
        double base = conversionService.packageBaseAmount(MeasurementType.VOLUME, 250.0, Unit.MILLILITER);
        assertEquals(250.0, base, 0.001);
    }

    @Test
    void packageBaseAmountVolumeLiter() {
        double base = conversionService.packageBaseAmount(MeasurementType.VOLUME, 1.0, Unit.LITER);
        assertEquals(1000.0, base, 0.001);
    }

    @Test
    void packageBaseAmountUnitPiece() {
        double base = conversionService.packageBaseAmount(MeasurementType.UNIT, 12.0, Unit.PIECE);
        assertEquals(12.0, base, 0.001);
    }

    @Test
    void packageBaseAmountToTaste() {
        double base = conversionService.packageBaseAmount(MeasurementType.TO_TASTE, 0.0, Unit.TO_TASTE);
        assertEquals(0.0, base, 0.001);
    }

    @Test
    void packageBaseAmountWeightRejectsCup() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.packageBaseAmount(MeasurementType.WEIGHT, 1.0, Unit.CUP)
        );
    }

    @Test
    void packageBaseAmountWeightRejectsTablespoon() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.packageBaseAmount(MeasurementType.WEIGHT, 1.0, Unit.TABLESPOON)
        );
    }

    @Test
    void packageBaseAmountWeightRejectsTeaspoon() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.packageBaseAmount(MeasurementType.WEIGHT, 1.0, Unit.TEASPOON)
        );
    }

    @Test
    void packageBaseAmountWeightRejectsPinch() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.packageBaseAmount(MeasurementType.WEIGHT, 1.0, Unit.PINCH)
        );
    }

    @Test
    void packageBaseAmountVolumeRejectsCup() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.packageBaseAmount(MeasurementType.VOLUME, 1.0, Unit.CUP)
        );
    }

    @Test
    void packageBaseAmountVolumeRejectsTablespoon() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.packageBaseAmount(MeasurementType.VOLUME, 1.0, Unit.TABLESPOON)
        );
    }

    @Test
    void packageBaseAmountUnitRejectsNonPiece() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.packageBaseAmount(MeasurementType.UNIT, 1.0, Unit.GRAM)
        );
    }

    @Test
    void toBaseAmountZeroQuantity() {
        double base = conversionService.toBaseAmount("rice", 0.0, Unit.CUP);
        assertEquals(0.0, base, 0.001);
    }

    @Test
    void toBaseAmountToTasteShortCircuit() {
        double base = conversionService.toBaseAmount("salt", 5.0, Unit.TO_TASTE);
        assertEquals(0.0, base, 0.001);
    }

    @Test
    void toBaseAmountDisallowedUnit() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.toBaseAmount("rice", 1.0, Unit.TABLESPOON)
        );
    }

    @Test
    void toBaseAmountMissingDensityRule() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.toBaseAmount("sugar", 1.0, Unit.PINCH)
        );
    }

    @Test
    void toBaseAmountWeightGramConversion() {
        double base = conversionService.toBaseAmount("salt", 500.0, Unit.GRAM);
        assertEquals(500.0, base, 0.001);
    }

    @Test
    void toBaseAmountWeightKilogramConversion() {
        double base = conversionService.toBaseAmount("sugar", 2.0, Unit.KILOGRAM);
        assertEquals(2000.0, base, 0.001);
    }

    @Test
    void toBaseAmountWeightCupConversion() {
        double base = conversionService.toBaseAmount("wheat-flour", 2.0, Unit.CUP);
        assertEquals(250.0, base, 0.001);
    }

    @Test
    void toBaseAmountWeightTablespoonConversion() {
        double base = conversionService.toBaseAmount("sugar", 2.0, Unit.TABLESPOON);
        assertEquals(25.0, base, 0.001);
    }

    @Test
    void toBaseAmountWeightTeaspoonConversion() {
        double base = conversionService.toBaseAmount("butter", 3.0, Unit.TEASPOON);
        assertEquals(14.1, base, 0.01);
    }

    @Test
    void toBaseAmountVolumeMilliliterConversion() {
        double base = conversionService.toBaseAmount("oil", 250.0, Unit.MILLILITER);
        assertEquals(250.0, base, 0.001);
    }

    @Test
    void toBaseAmountVolumeLiterConversion() {
        double base = conversionService.toBaseAmount("oil", 1.5, Unit.LITER);
        assertEquals(1500.0, base, 0.001);
    }

    @Test
    void toBaseAmountVolumeCupConversion() {
        double base = conversionService.toBaseAmount("milk", 2.0, Unit.CUP);
        assertEquals(480.0, base, 0.001);
    }

    @Test
    void toBaseAmountVolumeTablespoonConversion() {
        double base = conversionService.toBaseAmount("natilla", 3.0, Unit.TABLESPOON);
        assertEquals(45.0, base, 0.001);
    }

    @Test
    void toBaseAmountVolumeTeaspoonConversion() {
        double base = conversionService.toBaseAmount("oil", 6.0, Unit.TEASPOON);
        assertEquals(30.0, base, 0.001);
    }

    @Test
    void toBaseAmountUnitConversion() {
        double base = conversionService.toBaseAmount("egg", 12.0, Unit.PIECE);
        assertEquals(12.0, base, 0.001);
    }

    @Test
    void toBaseAmountUnknownIngredient() {
        assertThrows(IllegalArgumentException.class, () ->
            conversionService.toBaseAmount("non-existent-ingredient", 1.0, Unit.GRAM)
        );
    }

    @Test
    void baseUnitForWeight() {
        Unit baseUnit = conversionService.baseUnitFor("rice");
        assertEquals(Unit.GRAM, baseUnit);
    }

    @Test
    void baseUnitForVolume() {
        Unit baseUnit = conversionService.baseUnitFor("oil");
        assertEquals(Unit.MILLILITER, baseUnit);
    }

    @Test
    void baseUnitForUnit() {
        Unit baseUnit = conversionService.baseUnitFor("egg");
        assertEquals(Unit.PIECE, baseUnit);
    }

    @Test
    void baseUnitForToTaste() {
        Unit baseUnit = conversionService.baseUnitFor("black-pepper");
        assertEquals(Unit.TO_TASTE, baseUnit);
    }
}
