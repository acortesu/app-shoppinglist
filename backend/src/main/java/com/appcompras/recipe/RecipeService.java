package com.appcompras.recipe;

import com.appcompras.security.CurrentUserProvider;
import com.appcompras.service.IngredientCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientCatalogService ingredientCatalogService;
    private final CurrentUserProvider currentUserProvider;

    public RecipeService(
            RecipeRepository recipeRepository,
            IngredientCatalogService ingredientCatalogService,
            CurrentUserProvider currentUserProvider
    ) {
        this.recipeRepository = recipeRepository;
        this.ingredientCatalogService = ingredientCatalogService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public Recipe create(CreateRecipeRequest request) {
        String userId = currentUserProvider.getCurrentUserId();
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();

        List<RecipeIngredient> ingredients = request.ingredients().stream()
                .map(this::toValidatedIngredient)
                .toList();

        RecipeEntity entity = new RecipeEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setName(request.name().trim());
        entity.setType(request.type());
        entity.setIngredients(RecipeEntityMapper.toEmbeddables(ingredients));
        entity.setPreparation(request.preparation());
        entity.setNotes(request.notes());
        entity.setTags(RecipeEntityMapper.toTagSet(request.tags()));
        entity.setUsageCount(0);
        entity.setLastUsedAt(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        RecipeEntity saved = recipeRepository.save(entity);
        return RecipeEntityMapper.toDomain(saved);
    }

    @Transactional(readOnly = true)
    public Optional<Recipe> findById(String id) {
        String userId = currentUserProvider.getCurrentUserId();
        return recipeRepository.findByIdAndUserId(id, userId)
                .map(RecipeEntityMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public List<Recipe> findAll(MealType type) {
        String userId = currentUserProvider.getCurrentUserId();
        List<RecipeEntity> entities = type == null
                ? recipeRepository.findAllByUserIdOrderByCreatedAtDescIdAsc(userId)
                : recipeRepository.findAllByUserIdAndTypeOrderByCreatedAtDescIdAsc(userId, type);

        return entities.stream()
                .map(RecipeEntityMapper::toDomain)
                .toList();
    }

    @Transactional
    public Optional<Recipe> update(String id, CreateRecipeRequest request) {
        String userId = currentUserProvider.getCurrentUserId();
        Optional<RecipeEntity> existingOpt = recipeRepository.findByIdAndUserId(id, userId);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        List<RecipeIngredient> ingredients = request.ingredients().stream()
                .map(this::toValidatedIngredient)
                .toList();

        RecipeEntity existing = existingOpt.get();
        existing.setName(request.name().trim());
        existing.setType(request.type());
        existing.setIngredients(RecipeEntityMapper.toEmbeddables(ingredients));
        existing.setPreparation(request.preparation());
        existing.setNotes(request.notes());
        existing.setTags(RecipeEntityMapper.toTagSet(request.tags()));
        existing.setUpdatedAt(now);

        RecipeEntity saved = recipeRepository.save(existing);
        return Optional.of(RecipeEntityMapper.toDomain(saved));
    }

    @Transactional
    public boolean deleteById(String id) {
        String userId = currentUserProvider.getCurrentUserId();
        Optional<RecipeEntity> existingOpt = recipeRepository.findByIdAndUserId(id, userId);
        if (existingOpt.isEmpty()) {
            return false;
        }
        recipeRepository.delete(existingOpt.get());
        return true;
    }

    @Transactional
    public boolean incrementUsageCount(String id, Instant usedAt) {
        String userId = currentUserProvider.getCurrentUserId();
        Optional<RecipeEntity> existingOpt = recipeRepository.findByIdAndUserId(id, userId);
        if (existingOpt.isEmpty()) {
            return false;
        }

        RecipeEntity existing = existingOpt.get();
        existing.setUsageCount(existing.getUsageCount() + 1);
        existing.setLastUsedAt(usedAt);
        existing.setUpdatedAt(Instant.now());
        recipeRepository.save(existing);
        return true;
    }

    private RecipeIngredient toValidatedIngredient(CreateRecipeRequest.IngredientInput input) {
        String canonicalIngredientId = ingredientCatalogService.resolveIngredientId(input.ingredientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown ingredient: " + input.ingredientId() + ". Use /api/ingredients to discover options or create custom."
                ));

        com.appcompras.domain.Unit domainUnit = com.appcompras.domain.Unit.valueOf(input.unit().name());
        if (!ingredientCatalogService.isUnitAllowed(canonicalIngredientId, domainUnit)) {
            throw new IllegalArgumentException(
                    "Unit " + input.unit() + " is not allowed for ingredient " + canonicalIngredientId
            );
        }

        return new RecipeIngredient(canonicalIngredientId, input.quantity(), input.unit());
    }
}
