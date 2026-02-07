package com.appcompras.service;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class IngredientCatalogService {

    private final ConcurrentMap<String, IngredientCatalogItem> catalog = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> aliasToIngredientId = new ConcurrentHashMap<>();

    public IngredientCatalogService() {
        addBaseIngredient(
                "rice",
                "Rice",
                MeasurementType.WEIGHT,
                Set.of(Unit.GRAM, Unit.KILOGRAM, Unit.CUP),
                1.0,
                Unit.KILOGRAM,
                Set.of("arroz", "white rice")
        );
        addBaseIngredient(
                "oil",
                "Oil",
                MeasurementType.VOLUME,
                Set.of(Unit.MILLILITER, Unit.LITER, Unit.TABLESPOON, Unit.TEASPOON),
                500.0,
                Unit.MILLILITER,
                Set.of("aceite")
        );
        addBaseIngredient(
                "tomato",
                "Tomato",
                MeasurementType.UNIT,
                Set.of(Unit.PIECE),
                1.0,
                Unit.PIECE,
                Set.of("tomate")
        );
        addBaseIngredient(
                "salt",
                "Salt",
                MeasurementType.WEIGHT,
                Set.of(Unit.GRAM, Unit.KILOGRAM, Unit.PINCH, Unit.TO_TASTE),
                500.0,
                Unit.GRAM,
                Set.of("sal")
        );
        addBaseIngredient(
                "egg",
                "Egg",
                MeasurementType.UNIT,
                Set.of(Unit.PIECE),
                12.0,
                Unit.PIECE,
                Set.of("huevo", "huevos")
        );
    }

    public Optional<IngredientCatalogItem> findById(String ingredientId) {
        return Optional.ofNullable(catalog.get(ingredientId));
    }

    public Optional<String> resolveIngredientId(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return Optional.empty();
        }

        String direct = rawInput.trim();
        if (catalog.containsKey(direct)) {
            return Optional.of(direct);
        }

        String lowered = direct.toLowerCase(Locale.ROOT);
        if (catalog.containsKey(lowered)) {
            return Optional.of(lowered);
        }

        String normalized = normalizeAlias(rawInput);
        if (catalog.containsKey(normalized)) {
            return Optional.of(normalized);
        }

        return Optional.ofNullable(aliasToIngredientId.get(normalized));
    }

    public boolean isUnitAllowed(String ingredientId, Unit unit) {
        IngredientCatalogItem item = catalog.get(ingredientId);
        return item != null && item.allowedUnits().contains(unit);
    }

    public List<IngredientCatalogItem> list(String query) {
        if (query == null || query.isBlank()) {
            return catalog.values().stream()
                    .sorted(Comparator.comparing(IngredientCatalogItem::displayName))
                    .toList();
        }

        String normalizedQuery = normalizeAlias(query);
        return catalog.values().stream()
                .filter(item -> item.ingredientId().contains(normalizedQuery)
                        || normalizeAlias(item.displayName()).contains(normalizedQuery)
                        || hasMatchingAlias(item.ingredientId(), normalizedQuery))
                .sorted(Comparator.comparing(IngredientCatalogItem::displayName))
                .toList();
    }

    public IngredientCatalogItem createCustomIngredient(String name, MeasurementType measurementType) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ingredient name is required");
        }

        String trimmedName = name.trim();
        String normalizedName = normalizeAlias(trimmedName);
        Optional<String> existing = resolveIngredientId(trimmedName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Ingredient already exists: " + existing.get());
        }

        String id = "custom-" + normalizedName + "-" + UUID.randomUUID().toString().substring(0, 8);
        IngredientCatalogItem item = switch (measurementType) {
            case WEIGHT -> new IngredientCatalogItem(
                    id,
                    trimmedName,
                    MeasurementType.WEIGHT,
                    Set.of(Unit.GRAM, Unit.KILOGRAM),
                    1.0,
                    Unit.KILOGRAM
            );
            case VOLUME -> new IngredientCatalogItem(
                    id,
                    trimmedName,
                    MeasurementType.VOLUME,
                    Set.of(Unit.MILLILITER, Unit.LITER),
                    1.0,
                    Unit.LITER
            );
            case UNIT -> new IngredientCatalogItem(
                    id,
                    trimmedName,
                    MeasurementType.UNIT,
                    Set.of(Unit.PIECE),
                    1.0,
                    Unit.PIECE
            );
            case TO_TASTE -> new IngredientCatalogItem(
                    id,
                    trimmedName,
                    MeasurementType.TO_TASTE,
                    Set.of(Unit.TO_TASTE),
                    0.0,
                    Unit.TO_TASTE
            );
        };

        catalog.put(id, item);
        aliasToIngredientId.put(normalizedName, id);
        aliasToIngredientId.put(normalizeAlias(id), id);
        return item;
    }

    private void addBaseIngredient(
            String id,
            String displayName,
            MeasurementType measurementType,
            Set<Unit> allowedUnits,
            double suggestedPurchaseAmount,
            Unit suggestedPurchaseUnit,
            Set<String> aliases
    ) {
        IngredientCatalogItem item = new IngredientCatalogItem(
                id,
                displayName,
                measurementType,
                allowedUnits,
                suggestedPurchaseAmount,
                suggestedPurchaseUnit
        );
        catalog.put(id, item);
        aliasToIngredientId.put(normalizeAlias(id), id);
        aliasToIngredientId.put(normalizeAlias(displayName), id);
        for (String alias : aliases) {
            aliasToIngredientId.put(normalizeAlias(alias), id);
        }
    }

    private boolean hasMatchingAlias(String ingredientId, String query) {
        return aliasToIngredientId.entrySet().stream()
                .anyMatch(entry -> entry.getValue().equals(ingredientId) && entry.getKey().contains(query));
    }

    private String normalizeAlias(String value) {
        String withoutAccent = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String normalized = withoutAccent.replaceAll("[^a-z0-9]+", "-");
        return normalized.replaceAll("^-+|-+$", "");
    }
}
