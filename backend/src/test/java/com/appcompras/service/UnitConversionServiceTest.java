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
}
