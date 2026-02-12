package com.appcompras.service;

import com.appcompras.domain.IngredientCatalogItem;
import com.appcompras.domain.MeasurementType;
import com.appcompras.domain.Unit;
import com.appcompras.security.CurrentUserProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final int MIN_SUPPORTED_CATALOG_VERSION = 1;

    private final ConcurrentMap<String, IngredientCatalogItem> seedCatalog = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> seedAliasToIngredientId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<String>> seedAliasesByIngredientId = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, IngredientCatalogItem> localCustomCatalog = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> localCustomAliasToIngredientId = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final IngredientCustomRepository ingredientCustomRepository;
    private final CurrentUserProvider currentUserProvider;
    private final int catalogVersion;

    @Autowired
    public IngredientCatalogService(
            ObjectMapper objectMapper,
            IngredientCustomRepository ingredientCustomRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.objectMapper = objectMapper;
        this.ingredientCustomRepository = ingredientCustomRepository;
        this.currentUserProvider = currentUserProvider;
        this.catalogVersion = loadSeedFromResource(SEED_FILE);
    }

    public IngredientCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.ingredientCustomRepository = null;
        this.currentUserProvider = null;
        this.catalogVersion = loadSeedFromResource(SEED_FILE);
    }

    public int catalogVersion() {
        return catalogVersion;
    }

    public Optional<IngredientCatalogItem> findById(String ingredientId) {
        IngredientCatalogItem seedItem = seedCatalog.get(ingredientId);
        if (seedItem != null) {
            return Optional.of(seedItem);
        }

        if (ingredientCustomRepository != null) {
            return ingredientCustomRepository.findByIdAndUserId(ingredientId, currentUserId())
                    .map(this::toCustomCatalogItem);
        }

        return Optional.ofNullable(localCustomCatalog.get(ingredientId));
    }

    public Optional<String> resolveIngredientId(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return Optional.empty();
        }

        String direct = rawInput.trim();
        if (seedCatalog.containsKey(direct)) {
            return Optional.of(direct);
        }

        String lowered = direct.toLowerCase(Locale.ROOT);
        if (seedCatalog.containsKey(lowered)) {
            return Optional.of(lowered);
        }

        String normalized = normalizeAlias(rawInput);
        if (seedCatalog.containsKey(normalized)) {
            return Optional.of(normalized);
        }

        String seedAliasMatch = seedAliasToIngredientId.get(normalized);
        if (seedAliasMatch != null) {
            return Optional.of(seedAliasMatch);
        }

        if (ingredientCustomRepository != null) {
            Optional<IngredientCustomEntity> match = ingredientCustomRepository.findByUserIdAndNormalizedName(
                    currentUserId(), normalized);
            if (match.isPresent()) {
                return Optional.of(match.get().getId());
            }

            return ingredientCustomRepository.findByIdAndUserId(direct, currentUserId())
                    .map(IngredientCustomEntity::getId);
        }

        String customAliasMatch = localCustomAliasToIngredientId.get(normalized);
        if (customAliasMatch != null) {
            return Optional.of(customAliasMatch);
        }

        if (localCustomCatalog.containsKey(direct)) {
            return Optional.of(direct);
        }

        return Optional.empty();
    }

    public boolean isUnitAllowed(String ingredientId, Unit unit) {
        return findById(ingredientId)
                .map(item -> item.allowedUnits().contains(unit))
                .orElse(false);
    }

    public List<IngredientCatalogItem> list(String query) {
        List<IngredientCatalogItem> customItems = customItemsForCurrentUser();

        if (query == null || query.isBlank()) {
            return java.util.stream.Stream.concat(seedCatalog.values().stream(), customItems.stream())
                    .sorted(Comparator.comparing(IngredientCatalogItem::displayName))
                    .toList();
        }

        String normalizedQuery = normalizeAlias(query);
        return java.util.stream.Stream.concat(seedCatalog.values().stream(), customItems.stream())
                .filter(item -> item.ingredientId().contains(normalizedQuery)
                        || normalizeAlias(item.displayName()).contains(normalizedQuery)
                        || hasMatchingAlias(item.ingredientId(), normalizedQuery))
                .sorted(Comparator.comparing(IngredientCatalogItem::displayName))
                .toList();
    }

    public List<String> aliasesForItem(IngredientCatalogItem item) {
        if (item == null) {
            return List.of();
        }
        List<String> aliases = seedAliasesByIngredientId.get(item.ingredientId());
        if (aliases != null && !aliases.isEmpty()) {
            return aliases.stream()
                    .map(this::toDisplayCase)
                    .distinct()
                    .toList();
        }
        return List.of(toDisplayCase(item.displayName()));
    }

    public String preferredLabelForItem(IngredientCatalogItem item, String query) {
        if (item == null) {
            return "";
        }
        List<String> aliases = seedAliasesByIngredientId.get(item.ingredientId());
        if (aliases == null || aliases.isEmpty()) {
            return item.displayName();
        }
        if (query != null && !query.isBlank()) {
            String normalizedQuery = normalizeAlias(query);
            Optional<String> startsWithMatch = aliases.stream()
                    .filter(alias -> normalizeAlias(alias).startsWith(normalizedQuery))
                    .findFirst();
            if (startsWithMatch.isPresent()) {
                return toDisplayCase(startsWithMatch.get());
            }

            Optional<String> containsMatch = aliases.stream()
                    .filter(alias -> normalizeAlias(alias).contains(normalizedQuery))
                    .findFirst();
            if (containsMatch.isPresent()) {
                return toDisplayCase(containsMatch.get());
            }
        }
        return toDisplayCase(aliases.get(0));
    }

    public IngredientCatalogItem createCustomIngredient(String name, MeasurementType measurementType) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ingredient name is required");
        }

        String trimmedName = name.trim();
        String canonicalName = toDisplayCase(trimmedName);
        String normalizedName = normalizeAlias(canonicalName);
        Optional<String> existing = resolveIngredientId(trimmedName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Ingredient already exists: " + existing.get());
        }

        String id = "custom-" + normalizedName + "-" + UUID.randomUUID().toString().substring(0, 8);

        if (ingredientCustomRepository != null) {
            IngredientCustomEntity entity = new IngredientCustomEntity();
            entity.setId(id);
            entity.setUserId(currentUserId());
            entity.setName(canonicalName);
            entity.setNormalizedName(normalizedName);
            entity.setMeasurementType(measurementType);
            entity.setCreatedAt(Instant.now());

            try {
                IngredientCustomEntity saved = ingredientCustomRepository.save(entity);
                return toCustomCatalogItem(saved);
            } catch (DataIntegrityViolationException ex) {
                throw new IllegalArgumentException("Ingredient already exists: " + trimmedName);
            }
        }

        IngredientCatalogItem localItem = buildCustomItem(id, canonicalName, measurementType);
        localCustomCatalog.put(id, localItem);
        addLocalCustomAlias(id, normalizedName);
        addLocalCustomAlias(id, id);
        addLocalCustomAlias(id, canonicalName);
        return localItem;
    }

    private List<IngredientCatalogItem> customItemsForCurrentUser() {
        if (ingredientCustomRepository != null) {
            return ingredientCustomRepository.findAllByUserId(currentUserId()).stream()
                    .map(this::toCustomCatalogItem)
                    .toList();
        }
        return localCustomCatalog.values().stream().toList();
    }

    private IngredientCatalogItem toCustomCatalogItem(IngredientCustomEntity entity) {
        return buildCustomItem(entity.getId(), entity.getName(), entity.getMeasurementType());
    }

    private IngredientCatalogItem buildCustomItem(String id, String name, MeasurementType measurementType) {
        return switch (measurementType) {
            case WEIGHT -> new IngredientCatalogItem(
                    id,
                    name,
                    MeasurementType.WEIGHT,
                    Set.of(Unit.GRAM, Unit.KILOGRAM),
                    1.0,
                    Unit.KILOGRAM
            );
            case VOLUME -> new IngredientCatalogItem(
                    id,
                    name,
                    MeasurementType.VOLUME,
                    Set.of(Unit.MILLILITER, Unit.LITER, Unit.CUP, Unit.TABLESPOON, Unit.TEASPOON),
                    1.0,
                    Unit.LITER
            );
            case UNIT -> new IngredientCatalogItem(
                    id,
                    name,
                    MeasurementType.UNIT,
                    Set.of(Unit.PIECE),
                    1.0,
                    Unit.PIECE
            );
            case TO_TASTE -> new IngredientCatalogItem(
                    id,
                    name,
                    MeasurementType.TO_TASTE,
                    Set.of(Unit.PINCH, Unit.TO_TASTE),
                    0.0,
                    Unit.TO_TASTE
            );
        };
    }

    private int loadSeedFromResource(String resourcePath) {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            int version = root.path("catalogVersion").asInt(-1);
            if (version < MIN_SUPPORTED_CATALOG_VERSION) {
                throw new IllegalStateException("Invalid or missing catalogVersion in seed file: " + resourcePath);
            }

            JsonNode ingredientsNode = root.path("ingredients");
            if (!ingredientsNode.isArray()) {
                throw new IllegalStateException("Invalid ingredient seed format: missing ingredients array");
            }

            for (JsonNode ingredientNode : ingredientsNode) {
                registerSeedIngredient(ingredientNode);
            }
            return version;
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

        seedCatalog.put(id, item);
        addSeedAlias(id, id);
        addSeedAlias(id, displayName);

        List<String> aliases = new ArrayList<>();
        JsonNode aliasesNode = node.path("aliases");
        if (aliasesNode.isArray()) {
            for (JsonNode aliasNode : aliasesNode) {
                String alias = aliasNode.asText();
                addSeedAlias(id, alias);
                if (!alias.isBlank()) {
                    aliases.add(alias.trim());
                }
            }
        }
        seedAliasesByIngredientId.put(id, List.copyOf(aliases));
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalStateException("Ingredient seed missing required field: " + field);
        }
        return value.asText();
    }

    private void addSeedAlias(String ingredientId, String alias) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        String normalizedAlias = normalizeAlias(alias);
        if (normalizedAlias.isBlank()) {
            return;
        }

        String existingIngredientId = seedAliasToIngredientId.putIfAbsent(normalizedAlias, ingredientId);
        if (existingIngredientId != null && !existingIngredientId.equals(ingredientId)) {
            throw new IllegalStateException(
                    "Ambiguous alias '" + alias + "' maps to both '" + existingIngredientId + "' and '" + ingredientId + "'"
            );
        }
    }

    private void addLocalCustomAlias(String ingredientId, String alias) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        String normalizedAlias = normalizeAlias(alias);
        if (normalizedAlias.isBlank()) {
            return;
        }
        localCustomAliasToIngredientId.putIfAbsent(normalizedAlias, ingredientId);
    }

    private boolean hasMatchingAlias(String ingredientId, String query) {
        boolean seedMatch = seedAliasToIngredientId.entrySet().stream()
                .anyMatch(entry -> entry.getValue().equals(ingredientId) && entry.getKey().contains(query));
        if (seedMatch) {
            return true;
        }

        return localCustomAliasToIngredientId.entrySet().stream()
                .anyMatch(entry -> entry.getValue().equals(ingredientId) && entry.getKey().contains(query));
    }

    private String currentUserId() {
        if (currentUserProvider == null) {
            return "local-dev-user";
        }
        return currentUserProvider.getCurrentUserId();
    }

    private String normalizeAlias(String value) {
        String withoutAccent = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String normalized = withoutAccent.replaceAll("[^a-z0-9]+", "-");
        return normalized.replaceAll("^-+|-+$", "");
    }

    private String toDisplayCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Arrays.stream(value.trim().toLowerCase(Locale.forLanguageTag("es-CR")).split("\\s+"))
                .filter(token -> !token.isBlank())
                .map(token -> Character.toUpperCase(token.charAt(0)) + token.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }
}
