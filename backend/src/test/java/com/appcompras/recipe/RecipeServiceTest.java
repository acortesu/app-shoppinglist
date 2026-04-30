package com.appcompras.recipe;

import com.appcompras.config.BusinessRuleException;
import com.appcompras.domain.Unit;
import com.appcompras.security.CurrentUserProvider;
import com.appcompras.service.IngredientCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientCatalogService ingredientCatalogService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipeRepository, ingredientCatalogService, currentUserProvider);
        when(currentUserProvider.getCurrentUserId()).thenReturn("test-user-id");
    }

    @Test
    void createRecipeWithValidIngredient() {
        when(ingredientCatalogService.resolveIngredientId("rice")).thenReturn(Optional.of("rice"));
        when(ingredientCatalogService.isUnitAllowed("rice", Unit.CUP)).thenReturn(true);

        RecipeEntity savedEntity = new RecipeEntity();
        savedEntity.setId("recipe-id");
        savedEntity.setUserId("test-user-id");
        savedEntity.setName("Rice Bowl");
        when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(savedEntity);

        CreateRecipeRequest.IngredientInput rice = new CreateRecipeRequest.IngredientInput("rice", 1.0, Unit.CUP);
        CreateRecipeRequest request = new CreateRecipeRequest("Rice Bowl", MealType.LUNCH, List.of(rice), null, null, null);

        Recipe result = recipeService.create(request);

        assertEquals("Rice Bowl", result.name());
    }

    @Test
    void createRecipeWithUnknownIngredientThrows() {
        when(ingredientCatalogService.resolveIngredientId("unknown")).thenReturn(Optional.empty());

        CreateRecipeRequest.IngredientInput input = new CreateRecipeRequest.IngredientInput("unknown", 1.0, Unit.GRAM);
        CreateRecipeRequest request = new CreateRecipeRequest("Bad Recipe", MealType.LUNCH, List.of(input), null, null, null);

        assertThrows(BusinessRuleException.class, () -> recipeService.create(request));
    }

    @Test
    void createRecipeWithDisallowedUnitThrows() {
        when(ingredientCatalogService.resolveIngredientId("rice")).thenReturn(Optional.of("rice"));
        when(ingredientCatalogService.isUnitAllowed("rice", Unit.MILLILITER)).thenReturn(false);

        CreateRecipeRequest.IngredientInput input = new CreateRecipeRequest.IngredientInput("rice", 1.0, Unit.MILLILITER);
        CreateRecipeRequest request = new CreateRecipeRequest("Bad Recipe", MealType.LUNCH, List.of(input), null, null, null);

        assertThrows(BusinessRuleException.class, () -> recipeService.create(request));
    }

    @Test
    void createRecipeWithAliasResolution() {
        when(ingredientCatalogService.resolveIngredientId("arroz")).thenReturn(Optional.of("rice"));
        when(ingredientCatalogService.isUnitAllowed("rice", Unit.GRAM)).thenReturn(true);

        RecipeEntity savedEntity = new RecipeEntity();
        savedEntity.setId("recipe-id");
        savedEntity.setUserId("test-user-id");
        savedEntity.setName("Arroz Recipe");
        when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(savedEntity);

        CreateRecipeRequest.IngredientInput input = new CreateRecipeRequest.IngredientInput("arroz", 500.0, Unit.GRAM);
        CreateRecipeRequest request = new CreateRecipeRequest("Arroz Recipe", MealType.LUNCH, List.of(input), null, null, null);

        Recipe result = recipeService.create(request);

        assertEquals("Arroz Recipe", result.name());
    }

    @Test
    void incrementUsageCountIncrementsAndSetsLastUsed() {
        RecipeEntity entity = new RecipeEntity();
        entity.setId("recipe-id");
        entity.setUserId("test-user-id");
        entity.setUsageCount(5);
        entity.setLastUsedAt(null);

        when(recipeRepository.findByIdAndUserId("recipe-id", "test-user-id")).thenReturn(Optional.of(entity));
        when(recipeRepository.save(any(RecipeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant usedAt = Instant.now();
        boolean result = recipeService.incrementUsageCount("recipe-id", usedAt);

        assertTrue(result);
        assertEquals(6, entity.getUsageCount());
        assertEquals(usedAt, entity.getLastUsedAt());
        verify(recipeRepository).save(entity);
    }

    @Test
    void incrementUsageCountReturnsFalseWhenRecipeNotFound() {
        when(recipeRepository.findByIdAndUserId("nonexistent-id", "test-user-id")).thenReturn(Optional.empty());

        boolean result = recipeService.incrementUsageCount("nonexistent-id", Instant.now());

        assertFalse(result);
    }

    @Test
    void createRecipeValidatesAllIngredients() {
        when(ingredientCatalogService.resolveIngredientId("rice")).thenReturn(Optional.of("rice"));
        when(ingredientCatalogService.isUnitAllowed("rice", Unit.CUP)).thenReturn(true);
        when(ingredientCatalogService.resolveIngredientId("oil")).thenReturn(Optional.of("oil"));
        when(ingredientCatalogService.isUnitAllowed("oil", Unit.TABLESPOON)).thenReturn(true);

        RecipeEntity savedEntity = new RecipeEntity();
        savedEntity.setId("recipe-id");
        savedEntity.setUserId("test-user-id");
        savedEntity.setName("Test Recipe");
        when(recipeRepository.save(any(RecipeEntity.class))).thenReturn(savedEntity);

        CreateRecipeRequest.IngredientInput rice = new CreateRecipeRequest.IngredientInput("rice", 1.0, Unit.CUP);
        CreateRecipeRequest.IngredientInput oil = new CreateRecipeRequest.IngredientInput("oil", 2.0, Unit.TABLESPOON);
        CreateRecipeRequest request = new CreateRecipeRequest("Test Recipe", MealType.LUNCH, List.of(rice, oil), null, null, null);

        Recipe result = recipeService.create(request);

        assertEquals("Test Recipe", result.name());
        verify(recipeRepository).save(any(RecipeEntity.class));
    }

    @Test
    void findByIdReturnsRecipeWhenFound() {
        RecipeEntity entity = new RecipeEntity();
        entity.setId("recipe-id");
        entity.setUserId("test-user-id");
        entity.setName("My Recipe");

        when(recipeRepository.findByIdAndUserId("recipe-id", "test-user-id")).thenReturn(Optional.of(entity));

        Optional<Recipe> result = recipeService.findById("recipe-id");

        assertTrue(result.isPresent());
        assertEquals("My Recipe", result.get().name());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(recipeRepository.findByIdAndUserId("nonexistent-id", "test-user-id")).thenReturn(Optional.empty());

        Optional<Recipe> result = recipeService.findById("nonexistent-id");

        assertFalse(result.isPresent());
    }

    @Test
    void deleteByIdReturnsTrueWhenDeleted() {
        RecipeEntity entity = new RecipeEntity();
        entity.setId("recipe-id");

        when(recipeRepository.findByIdAndUserId("recipe-id", "test-user-id")).thenReturn(Optional.of(entity));

        boolean result = recipeService.deleteById("recipe-id");

        assertTrue(result);
        verify(recipeRepository).delete(entity);
    }

    @Test
    void deleteByIdReturnsFalseWhenNotFound() {
        when(recipeRepository.findByIdAndUserId("nonexistent-id", "test-user-id")).thenReturn(Optional.empty());

        boolean result = recipeService.deleteById("nonexistent-id");

        assertFalse(result);
    }
}
