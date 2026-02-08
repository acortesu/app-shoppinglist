package com.appcompras.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresE2EFlowTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("appcompras_it")
            .withUsername("appcompras_user")
            .withPassword("appcompras_pass");

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void endToEndFlowPersistsInPostgres() throws Exception {
        String customIngredientId = createCustomIngredientAndGetId();
        String recipeId = createRecipeAndGetId();
        String planId = createPlanAndGetId(recipeId);
        String shoppingListId = generateShoppingListAndGetId(planId);

        mockMvc.perform(put("/api/shopping-lists/{id}", shoppingListId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "name": "Papel higienico",
                                      "quantity": 2,
                                      "unit": "pack",
                                      "manual": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Papel higienico"));

        Integer recipeCount = jdbcTemplate.queryForObject("select count(*) from recipes", Integer.class);
        Integer planCount = jdbcTemplate.queryForObject("select count(*) from meal_plans", Integer.class);
        Integer draftCount = jdbcTemplate.queryForObject("select count(*) from shopping_list_drafts", Integer.class);
        Integer customIngredientCount = jdbcTemplate.queryForObject("select count(*) from ingredient_custom", Integer.class);
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version in ('1','2','3','4','5','6') and success = true",
                Integer.class
        );

        assertThat(recipeCount).isEqualTo(1);
        assertThat(planCount).isEqualTo(1);
        assertThat(draftCount).isEqualTo(1);
        assertThat(customIngredientCount).isEqualTo(1);
        assertThat(customIngredientId).startsWith("custom-carne-de-conejo-");
        assertThat(migrationCount).isEqualTo(6);
    }

    private String createCustomIngredientAndGetId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ingredients/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Carne de conejo",
                                  "measurementType": "WEIGHT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createRecipeAndGetId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Receta IT",
                                  "type": "DINNER",
                                  "ingredients": [
                                    { "ingredientId": "rice", "quantity": 200, "unit": "GRAM" }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createPlanAndGetId(String recipeId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-02-09",
                                  "period": "WEEK",
                                  "slots": [
                                    { "date": "2026-02-10", "mealType": "DINNER", "recipeId": "%s" }
                                  ]
                                }
                                """.formatted(recipeId)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String generateShoppingListAndGetId(String planId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shopping-lists/generate")
                        .param("planId", planId))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
