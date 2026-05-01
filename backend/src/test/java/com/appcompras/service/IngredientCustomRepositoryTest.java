package com.appcompras.service;

import com.appcompras.domain.MeasurementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class IngredientCustomRepositoryTest {

    @Autowired
    private IngredientCustomRepository ingredientCustomRepository;

    private IngredientCustomEntity ingredientUserA;
    private IngredientCustomEntity ingredientUserB;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        ingredientUserA = new IngredientCustomEntity();
        ingredientUserA.setId("custom-tomato-user-a");
        ingredientUserA.setUserId("user-a");
        ingredientUserA.setName("Tomato");
        ingredientUserA.setNormalizedName("tomato");
        ingredientUserA.setMeasurementType(MeasurementType.WEIGHT);
        ingredientUserA.setCreatedAt(now);
        ingredientCustomRepository.save(ingredientUserA);

        ingredientUserB = new IngredientCustomEntity();
        ingredientUserB.setId("custom-pepper-user-b");
        ingredientUserB.setUserId("user-b");
        ingredientUserB.setName("Pepper");
        ingredientUserB.setNormalizedName("pepper");
        ingredientUserB.setMeasurementType(MeasurementType.WEIGHT);
        ingredientUserB.setCreatedAt(now);
        ingredientCustomRepository.save(ingredientUserB);
    }

    @Test
    void findByIdAndUserIdReturnsWhenMatch() {
        Optional<IngredientCustomEntity> result = ingredientCustomRepository.findByIdAndUserId("custom-tomato-user-a", "user-a");
        assertTrue(result.isPresent());
        assertEquals("custom-tomato-user-a", result.get().getId());
        assertEquals("user-a", result.get().getUserId());
    }

    @Test
    void findByIdAndUserIdReturnsEmptyWhenUserMismatch() {
        Optional<IngredientCustomEntity> result = ingredientCustomRepository.findByIdAndUserId("custom-tomato-user-a", "user-b");
        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndUserIdReturnsEmptyWhenIdNotFound() {
        Optional<IngredientCustomEntity> result = ingredientCustomRepository.findByIdAndUserId("nonexistent", "user-a");
        assertFalse(result.isPresent());
    }

    @Test
    void findByUserIdAndNormalizedNameReturnsWhenMatch() {
        Optional<IngredientCustomEntity> result = ingredientCustomRepository.findByUserIdAndNormalizedName("user-a", "tomato");
        assertTrue(result.isPresent());
        assertEquals("Tomato", result.get().getName());
        assertEquals("tomato", result.get().getNormalizedName());
    }

    @Test
    void findByUserIdAndNormalizedNameReturnsEmptyWhenUserMismatch() {
        Optional<IngredientCustomEntity> result = ingredientCustomRepository.findByUserIdAndNormalizedName("user-b", "tomato");
        assertFalse(result.isPresent());
    }

    @Test
    void findByUserIdAndNormalizedNameReturnsEmptyWhenNameNotFound() {
        Optional<IngredientCustomEntity> result = ingredientCustomRepository.findByUserIdAndNormalizedName("user-a", "nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void findAllByUserIdReturnsAllForUser() {
        Instant now = Instant.now();

        IngredientCustomEntity second = new IngredientCustomEntity();
        second.setId("custom-carrot-user-a");
        second.setUserId("user-a");
        second.setName("Carrot");
        second.setNormalizedName("carrot");
        second.setMeasurementType(MeasurementType.WEIGHT);
        second.setCreatedAt(now);
        ingredientCustomRepository.save(second);

        List<IngredientCustomEntity> results = ingredientCustomRepository.findAllByUserId("user-a");

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(i -> "tomato".equals(i.getNormalizedName())));
        assertTrue(results.stream().anyMatch(i -> "carrot".equals(i.getNormalizedName())));
    }

    @Test
    void findAllByUserIdFiltersPerUser() {
        List<IngredientCustomEntity> resultsUserA = ingredientCustomRepository.findAllByUserId("user-a");
        List<IngredientCustomEntity> resultsUserB = ingredientCustomRepository.findAllByUserId("user-b");

        assertEquals(1, resultsUserA.size());
        assertEquals(1, resultsUserB.size());
        assertEquals("Tomato", resultsUserA.get(0).getName());
        assertEquals("Pepper", resultsUserB.get(0).getName());
    }

    @Test
    void findAllByUserIdReturnsEmptyForUnknownUser() {
        List<IngredientCustomEntity> results = ingredientCustomRepository.findAllByUserId("unknown-user");
        assertEquals(0, results.size());
    }

    @Test
    void saveAndRetrievePersistsAllFields() {
        Instant created = Instant.parse("2026-05-01T10:00:00Z");

        IngredientCustomEntity ingredient = new IngredientCustomEntity();
        ingredient.setId("custom-full");
        ingredient.setUserId("user-full");
        ingredient.setName("Spinach");
        ingredient.setNormalizedName("spinach");
        ingredient.setMeasurementType(MeasurementType.WEIGHT);
        ingredient.setCreatedAt(created);

        ingredientCustomRepository.save(ingredient);
        Optional<IngredientCustomEntity> retrieved = ingredientCustomRepository.findByIdAndUserId("custom-full", "user-full");

        assertTrue(retrieved.isPresent());
        assertEquals("custom-full", retrieved.get().getId());
        assertEquals("user-full", retrieved.get().getUserId());
        assertEquals("Spinach", retrieved.get().getName());
        assertEquals("spinach", retrieved.get().getNormalizedName());
        assertEquals(MeasurementType.WEIGHT, retrieved.get().getMeasurementType());
        assertEquals(created, retrieved.get().getCreatedAt());
    }

    @Test
    void saveDifferentMeasurementTypes() {
        Instant now = Instant.now();

        IngredientCustomEntity volume = new IngredientCustomEntity();
        volume.setId("custom-milk");
        volume.setUserId("user-a");
        volume.setName("Milk");
        volume.setNormalizedName("milk");
        volume.setMeasurementType(MeasurementType.VOLUME);
        volume.setCreatedAt(now);
        ingredientCustomRepository.save(volume);

        IngredientCustomEntity unit = new IngredientCustomEntity();
        unit.setId("custom-eggs");
        unit.setUserId("user-a");
        unit.setName("Eggs");
        unit.setNormalizedName("eggs");
        unit.setMeasurementType(MeasurementType.UNIT);
        unit.setCreatedAt(now);
        ingredientCustomRepository.save(unit);

        List<IngredientCustomEntity> results = ingredientCustomRepository.findAllByUserId("user-a");
        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(i -> MeasurementType.WEIGHT == i.getMeasurementType()));
        assertTrue(results.stream().anyMatch(i -> MeasurementType.VOLUME == i.getMeasurementType()));
        assertTrue(results.stream().anyMatch(i -> MeasurementType.UNIT == i.getMeasurementType()));
    }

    @Test
    void deleteRemovesEntity() {
        ingredientCustomRepository.delete(ingredientUserA);
        Optional<IngredientCustomEntity> result = ingredientCustomRepository.findByIdAndUserId("custom-tomato-user-a", "user-a");
        assertFalse(result.isPresent());
    }

    @Test
    void deleteOnlyAffectsTargetEntity() {
        ingredientCustomRepository.delete(ingredientUserA);
        Optional<IngredientCustomEntity> ingredientAResult = ingredientCustomRepository.findByIdAndUserId("custom-tomato-user-a", "user-a");
        Optional<IngredientCustomEntity> ingredientBResult = ingredientCustomRepository.findByIdAndUserId("custom-pepper-user-b", "user-b");

        assertFalse(ingredientAResult.isPresent());
        assertTrue(ingredientBResult.isPresent());
    }

    @Test
    void findByUserIdAndNormalizedNameSeparatesPerUser() {
        Instant now = Instant.now();

        IngredientCustomEntity sameNameDifferentUser = new IngredientCustomEntity();
        sameNameDifferentUser.setId("custom-tomato-user-b");
        sameNameDifferentUser.setUserId("user-b");
        sameNameDifferentUser.setName("Tomato");
        sameNameDifferentUser.setNormalizedName("tomato");
        sameNameDifferentUser.setMeasurementType(MeasurementType.VOLUME);
        sameNameDifferentUser.setCreatedAt(now);
        ingredientCustomRepository.save(sameNameDifferentUser);

        Optional<IngredientCustomEntity> resultUserA = ingredientCustomRepository.findByUserIdAndNormalizedName("user-a", "tomato");
        Optional<IngredientCustomEntity> resultUserB = ingredientCustomRepository.findByUserIdAndNormalizedName("user-b", "tomato");

        assertTrue(resultUserA.isPresent());
        assertTrue(resultUserB.isPresent());
        assertEquals(MeasurementType.WEIGHT, resultUserA.get().getMeasurementType());
        assertEquals(MeasurementType.VOLUME, resultUserB.get().getMeasurementType());
    }

    @Test
    void updateModifiesExistingEntity() {
        ingredientUserA.setName("Cherry Tomato");
        ingredientUserA.setMeasurementType(MeasurementType.UNIT);
        ingredientCustomRepository.save(ingredientUserA);

        Optional<IngredientCustomEntity> retrieved = ingredientCustomRepository.findByIdAndUserId("custom-tomato-user-a", "user-a");
        assertTrue(retrieved.isPresent());
        assertEquals("Cherry Tomato", retrieved.get().getName());
        assertEquals(MeasurementType.UNIT, retrieved.get().getMeasurementType());
    }
}
