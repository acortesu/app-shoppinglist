package com.appcompras.service;

import com.appcompras.domain.Unit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
