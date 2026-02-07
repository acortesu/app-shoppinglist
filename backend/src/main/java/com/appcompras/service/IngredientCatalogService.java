package com.appcompras.service;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class IngredientCatalogService {

    private static final String SEED_FILE = "seed/ingredients-catalog-cr.json";

    private final ConcurrentMap<String, IngredientCatalogItem> catalog = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> aliasToIngredientId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public IngredientCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        loadSeedFromResource(SEED_FILE);
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
        addAlias(id, normalizedName);
        addAlias(id, id);
        addAlias(id, trimmedName);
        return item;
    }

    private void loadSeedFromResource(String resourcePath) {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode ingredientsNode = root.path("ingredients");
            if (!ingredientsNode.isArray()) {
                throw new IllegalStateException("Invalid ingredient seed format: missing ingredients array");
            }

            for (JsonNode ingredientNode : ingredientsNode) {
                registerSeedIngredient(ingredientNode);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ingredient seed file: " + resourcePath, e);
        }
    }

    private void registerSeedIngredient(JsonNode node) {
        String id = requiredText(node, "id");
        String displayName = requiredText(node, "displayName");
        MeasurementType measurementType = MeasurementType.valueOf(requiredText(node, "measurementType"));
        double suggestedPurchaseAmount = node.path("suggestedPurchaseAmount").asDouble();
        Unit suggestedPurchaseUnit = Unit.valueOf(requiredText(node, "suggestedPurchaseUnit"));

        Set<Unit> allowedUnits = new HashSet<>();
        JsonNode allowedUnitsNode = node.path("allowedUnits");
        if (!allowedUnitsNode.isArray() || allowedUnitsNode.isEmpty()) {
            throw new IllegalStateException("Ingredient seed requires non-empty allowedUnits for id: " + id);
        }
        for (JsonNode unitNode : allowedUnitsNode) {
            allowedUnits.add(Unit.valueOf(unitNode.asText()));
        }

        IngredientCatalogItem item = new IngredientCatalogItem(
                id,
                displayName,
                measurementType,
                Set.copyOf(allowedUnits),
                suggestedPurchaseAmount,
                suggestedPurchaseUnit
        );

        catalog.put(id, item);
        addAlias(id, id);
        addAlias(id, displayName);

        JsonNode aliasesNode = node.path("aliases");
        if (aliasesNode.isArray()) {
            for (JsonNode aliasNode : aliasesNode) {
                addAlias(id, aliasNode.asText());
            }
        }
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalStateException("Ingredient seed missing required field: " + field);
        }
        return value.asText();
    }

    private void addAlias(String ingredientId, String alias) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        String normalizedAlias = normalizeAlias(alias);
        if (normalizedAlias.isBlank()) {
            return;
        }

        String existingIngredientId = aliasToIngredientId.putIfAbsent(normalizedAlias, ingredientId);
        if (existingIngredientId != null && !existingIngredientId.equals(ingredientId)) {
            throw new IllegalStateException(
                    "Ambiguous alias '" + alias + "' maps to both '" + existingIngredientId + "' and '" + ingredientId + "'"
            );
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
