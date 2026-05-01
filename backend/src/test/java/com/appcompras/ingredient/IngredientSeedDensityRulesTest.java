package com.appcompras.ingredient;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;
import com.appcompras.service.IngredientCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientSeedDensityRulesTest {

    private IngredientCatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new IngredientCatalogService(new ObjectMapper());
    }

    @Test
    void everyWeightIngredientWithCupHasDensityRule() {
        Optional<IngredientCatalogItem> rice = catalogService.findById("rice");
        assertTrue(rice.isPresent());
        assertTrue(rice.get().allowedUnits().contains(Unit.CUP));
        assertTrue(rice.get().densityRules().containsKey(Unit.CUP),
            "rice must have CUP density rule");
    }

    @Test
    void everyWeightIngredientWithTablespoonHasDensityRule() {
        Optional<IngredientCatalogItem> sugar = catalogService.findById("sugar");
        assertTrue(sugar.isPresent());
        assertTrue(sugar.get().allowedUnits().contains(Unit.TABLESPOON));
        assertTrue(sugar.get().densityRules().containsKey(Unit.TABLESPOON),
            "sugar must have TABLESPOON density rule");
    }

    @Test
    void everyWeightIngredientWithTeaspoonHasDensityRule() {
        Optional<IngredientCatalogItem> butter = catalogService.findById("butter");
        assertTrue(butter.isPresent());
        assertTrue(butter.get().allowedUnits().contains(Unit.TEASPOON));
        assertTrue(butter.get().densityRules().containsKey(Unit.TEASPOON),
            "butter must have TEASPOON density rule");
    }

    @Test
    void saltPinchDensityRule() {
        Optional<IngredientCatalogItem> salt = catalogService.findById("salt");
        assertTrue(salt.isPresent());
        assertTrue(salt.get().allowedUnits().contains(Unit.PINCH));
        assertTrue(salt.get().densityRules().containsKey(Unit.PINCH),
            "salt must have PINCH density rule");
        assertTrue(salt.get().densityRules().get(Unit.PINCH) > 0);
    }

    @Test
    void florDensityRulesComplete() {
        Optional<IngredientCatalogItem> wheatFlour = catalogService.findById("wheat-flour");
        assertTrue(wheatFlour.isPresent());

        Set<Unit> allowedUnits = wheatFlour.get().allowedUnits();
        assertTrue(allowedUnits.contains(Unit.CUP));
        assertTrue(allowedUnits.contains(Unit.TABLESPOON));
        assertTrue(allowedUnits.contains(Unit.TEASPOON));

        assertTrue(wheatFlour.get().densityRules().containsKey(Unit.CUP));
        assertTrue(wheatFlour.get().densityRules().containsKey(Unit.TABLESPOON));
        assertTrue(wheatFlour.get().densityRules().containsKey(Unit.TEASPOON));
    }

    @Test
    void honeyDensityRulesComplete() {
        Optional<IngredientCatalogItem> honey = catalogService.findById("honey");
        assertTrue(honey.isPresent());

        Set<Unit> allowedUnits = honey.get().allowedUnits();
        assertTrue(allowedUnits.contains(Unit.CUP));
        assertTrue(allowedUnits.contains(Unit.TABLESPOON));
        assertTrue(allowedUnits.contains(Unit.TEASPOON));

        assertTrue(honey.get().densityRules().containsKey(Unit.CUP));
        assertTrue(honey.get().densityRules().containsKey(Unit.TABLESPOON));
        assertTrue(honey.get().densityRules().containsKey(Unit.TEASPOON));

        assertTrue(honey.get().densityRules().get(Unit.CUP) > 100,
            "honey density (CUP) should be substantial (> 100g)");
    }

    @Test
    void ingredientsWithoutConversionUnitsHaveEmptyDensityRules() {
        Optional<IngredientCatalogItem> chicken = catalogService.findById("chicken");
        assertTrue(chicken.isPresent());
        assertFalse(chicken.get().allowedUnits().contains(Unit.CUP));
        assertFalse(chicken.get().allowedUnits().contains(Unit.TABLESPOON));
        assertTrue(chicken.get().densityRules().isEmpty(),
            "chicken has no conversion units, so no density rules needed");
    }

    @Test
    void volumeIngredientsHaveNoDensityRulesForWeightUnits() {
        Optional<IngredientCatalogItem> oil = catalogService.findById("oil");
        assertTrue(oil.isPresent());
        assertTrue(oil.get().measurementType() == MeasurementType.VOLUME);
        assertFalse(oil.get().densityRules().containsKey(Unit.GRAM),
            "oil is VOLUME, not WEIGHT, so no GRAM/KILOGRAM rules");
        assertFalse(oil.get().densityRules().containsKey(Unit.KILOGRAM));
    }

    @Test
    void toTasteIngredientsHaveNoDensityRules() {
        Optional<IngredientCatalogItem> pepper = catalogService.findById("black-pepper");
        assertTrue(pepper.isPresent());
        assertTrue(pepper.get().measurementType() == MeasurementType.TO_TASTE);
        assertTrue(pepper.get().densityRules().isEmpty(),
            "TO_TASTE ingredients never need conversion rules");
    }

    @Test
    void seedLoadsSuccessfully() {
        assertTrue(catalogService.catalogVersion() >= 1,
            "seed must load with valid catalogVersion >= 1");
    }
}
