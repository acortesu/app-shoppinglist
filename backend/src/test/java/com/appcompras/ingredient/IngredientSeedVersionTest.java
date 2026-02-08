package com.appcompras.ingredient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientSeedVersionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ingredientSeedIncludesCatalogVersionAndIngredientsArray() throws Exception {
        try (InputStream inputStream = new ClassPathResource("seed/ingredients-catalog-cr.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            assertThat(root.path("catalogVersion").asInt(-1)).isGreaterThanOrEqualTo(1);
            assertThat(root.path("ingredients").isArray()).isTrue();
            assertThat(root.path("ingredients").size()).isGreaterThan(0);
        }
    }
}
