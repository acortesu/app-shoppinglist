package com.appcompras.ingredient;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.service.IngredientCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientCatalogServiceTest {

    private IngredientCatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new IngredientCatalogService(new ObjectMapper());
    }

    @Test
    void seedItemTakesPrecedenceOverCustom() {
        Optional<IngredientCatalogItem> rice = catalogService.findById("rice");
        assertTrue(rice.isPresent());
        assertEquals("Rice", rice.get().displayName());
    }

    @Test
    void customIngredientCreated() {
        IngredientCatalogItem created = catalogService.createCustomIngredient("Cucumber", MeasurementType.WEIGHT);
        assertEquals(MeasurementType.WEIGHT, created.measurementType());
        assertTrue(created.ingredientId().startsWith("custom-"));
    }

    @Test
    void customIngredientResolvedById() {
        IngredientCatalogItem created = catalogService.createCustomIngredient("Zucchini", MeasurementType.WEIGHT);
        Optional<IngredientCatalogItem> found = catalogService.findById(created.ingredientId());
        assertTrue(found.isPresent());
        assertEquals("Zucchini", found.get().displayName());
    }

    @Test
    void duplicateNameThrowsForSeedIngredient() {
        assertThrows(IllegalArgumentException.class, () ->
                catalogService.createCustomIngredient("Rice", MeasurementType.WEIGHT)
        );
    }

    @Test
    void duplicateNameThrowsForCustomIngredient() {
        catalogService.createCustomIngredient("Broccoli", MeasurementType.WEIGHT);
        assertThrows(IllegalArgumentException.class, () ->
                catalogService.createCustomIngredient("Broccoli", MeasurementType.WEIGHT)
        );
    }

    @Test
    void aliasNormalizationIgnoresCase() {
        Optional<String> lower = catalogService.resolveIngredientId("rice");
        Optional<String> upper = catalogService.resolveIngredientId("RICE");
        Optional<String> mixed = catalogService.resolveIngredientId("RiCe");
        assertTrue(lower.isPresent());
        assertTrue(upper.isPresent());
        assertTrue(mixed.isPresent());
        assertEquals(lower.get(), upper.get());
        assertEquals(lower.get(), mixed.get());
    }

    @Test
    void aliasNormalizationIgnoresAccents() {
        IngredientCatalogItem created = catalogService.createCustomIngredient("Açaí", MeasurementType.VOLUME);
        Optional<String> withAccent = catalogService.resolveIngredientId("açaí");
        Optional<String> withoutAccent = catalogService.resolveIngredientId("acai");
        assertTrue(withAccent.isPresent());
        assertTrue(withoutAccent.isPresent());
        assertEquals(withAccent.get(), withoutAccent.get());
        assertEquals(created.ingredientId(), withAccent.get());
    }

    @Test
    void aliasNormalizationIgnoresWhitespace() {
        Optional<String> single = catalogService.resolveIngredientId("wheat flour");
        Optional<String> multiple = catalogService.resolveIngredientId("wheat    flour");
        assertTrue(single.isPresent());
        assertTrue(multiple.isPresent());
        assertEquals(single.get(), multiple.get());
    }

    @Test
    void listWithoutQueryReturnsSorted() {
        List<IngredientCatalogItem> items = catalogService.list(null);
        assertTrue(items.size() > 0);
        assertTrue(items.stream().map(i -> i.displayName()).allMatch(name ->
                name.equals(name.trim())
        ));
    }

    @Test
    void listWithQueryFiltersResults() {
        List<IngredientCatalogItem> items = catalogService.list("rice");
        assertTrue(items.size() > 0);
        assertTrue(items.stream().anyMatch(item -> item.ingredientId().equals("rice")));
    }

    @Test
    void listWithQueryCaseInsensitive() {
        List<IngredientCatalogItem> lowercase = catalogService.list("rice");
        List<IngredientCatalogItem> uppercase = catalogService.list("RICE");
        assertEquals(lowercase.size(), uppercase.size());
    }

    @Test
    void listWithPartialQueryMatches() {
        List<IngredientCatalogItem> items = catalogService.list("ric");
        assertTrue(items.stream().anyMatch(item -> item.ingredientId().contains("ric")), "Should find rice");
    }

    @Test
    void listWithEmptyQueryReturnsFull() {
        List<IngredientCatalogItem> all = catalogService.list("");
        List<IngredientCatalogItem> none = catalogService.list(null);
        assertEquals(all.size(), none.size());
    }

    @Test
    void searchIncludesCustomIngredients() {
        catalogService.createCustomIngredient("CustomVegetable", MeasurementType.WEIGHT);
        List<IngredientCatalogItem> results = catalogService.list("custom");
        assertTrue(results.stream().anyMatch(item -> item.ingredientId().startsWith("custom-")));
    }

    @Test
    void resolveIngredientIdReturnsSeedItemDirectly() {
        Optional<String> result = catalogService.resolveIngredientId("rice");
        assertTrue(result.isPresent());
        assertEquals("rice", result.get());
    }

    @Test
    void resolveIngredientIdReturnsCustomItemDirectly() {
        IngredientCatalogItem created = catalogService.createCustomIngredient("Kale", MeasurementType.WEIGHT);
        Optional<String> result = catalogService.resolveIngredientId(created.ingredientId());
        assertTrue(result.isPresent());
        assertEquals(created.ingredientId(), result.get());
    }

    @Test
    void resolveIngredientIdReturnsSeedAlias() {
        Optional<String> result = catalogService.resolveIngredientId("arroz");
        assertTrue(result.isPresent());
        assertEquals("rice", result.get());
    }

    @Test
    void resolveIngredientIdReturnsEmptyForUnknown() {
        Optional<String> result = catalogService.resolveIngredientId("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void isUnitAllowedForValidUnit() {
        assertTrue(catalogService.isUnitAllowed("rice", com.appcompras.domain.Unit.GRAM));
    }

    @Test
    void isUnitAllowedForInvalidUnit() {
        assertFalse(catalogService.isUnitAllowed("rice", com.appcompras.domain.Unit.LITER));
    }

    @Test
    void isUnitAllowedForUnknownIngredient() {
        assertFalse(catalogService.isUnitAllowed("unknown", com.appcompras.domain.Unit.GRAM));
    }

    @Test
    void createCustomIngredientWithBlankNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                catalogService.createCustomIngredient("   ", MeasurementType.WEIGHT)
        );
    }

    @Test
    void createCustomIngredientWithNullNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                catalogService.createCustomIngredient(null, MeasurementType.WEIGHT)
        );
    }

    @Test
    void aliasesForSeedIngredient() {
        Optional<IngredientCatalogItem> rice = catalogService.findById("rice");
        assertTrue(rice.isPresent());
        List<String> aliases = catalogService.aliasesForItem(rice.get());
        assertTrue(aliases.size() > 0);
        assertTrue(aliases.stream().anyMatch(alias -> alias.toLowerCase().contains("rice")));
    }

    @Test
    void aliasesForCustomIngredient() {
        IngredientCatalogItem created = catalogService.createCustomIngredient("Radish", MeasurementType.WEIGHT);
        List<String> aliases = catalogService.aliasesForItem(created);
        assertTrue(aliases.size() > 0);
        assertTrue(aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase("Radish")));
    }

    @Test
    void preferredLabelForItemWithQuery() {
        Optional<IngredientCatalogItem> rice = catalogService.findById("rice");
        assertTrue(rice.isPresent());
        String label = catalogService.preferredLabelForItem(rice.get(), "arr");
        assertFalse(label.isEmpty());
    }

    @Test
    void preferredLabelForItemWithoutQuery() {
        Optional<IngredientCatalogItem> rice = catalogService.findById("rice");
        assertTrue(rice.isPresent());
        String label = catalogService.preferredLabelForItem(rice.get(), null);
        assertFalse(label.isEmpty());
    }

    @Test
    void preferredLabelForNull() {
        String label = catalogService.preferredLabelForItem(null, "query");
        assertEquals("", label);
    }

    @Test
    void catalogVersionLoadsSuccessfully() {
        assertTrue(catalogService.catalogVersion() >= 1);
    }
}
