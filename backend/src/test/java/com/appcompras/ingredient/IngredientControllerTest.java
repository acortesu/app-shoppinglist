package com.appcompras.ingredient;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listIngredientsReturnsCatalog() throws Exception {
        mockMvc.perform(get("/api/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    void listIngredientsCanSearchByAlias() throws Exception {
        mockMvc.perform(get("/api/ingredients").param("q", "arroz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("rice"))
                .andExpect(jsonPath("$[0].preferredLabel").value("Arroz"))
                .andExpect(jsonPath("$[0].aliases").isArray());
    }

    @Test
    void aliasBackwardCompatibilitySearchStillResolvesLegacyNames() throws Exception {
        mockMvc.perform(get("/api/ingredients").param("q", "frijoles negros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("beans"));

        mockMvc.perform(get("/api/ingredients").param("q", "Pechuga de Pollo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("chicken"));

        mockMvc.perform(get("/api/ingredients").param("q", "ARROZ INTEGRÁL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("rice"));
    }

    @Test
    void recipeCreateStillAcceptsLegacyAliases() throws Exception {
        String payload = """
                {
                  "name": "Frijoles con pollo",
                  "type": "LUNCH",
                  "ingredients": [
                    { "ingredientId": "frijoles negros", "quantity": 300, "unit": "GRAM" },
                    { "ingredientId": "pechuga de pollo", "quantity": 250, "unit": "GRAM" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value("beans"))
                .andExpect(jsonPath("$.ingredients[1].ingredientId").value("chicken"));
    }

    @Test
    void createCustomIngredientAndUseItInRecipe() throws Exception {
        MvcResult createCustom = mockMvc.perform(post("/api/ingredients/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Carne de conejo",
                                  "measurementType": "WEIGHT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.startsWith("custom-carne-de-conejo-")))
                .andExpect(jsonPath("$.custom").value(true))
                .andReturn();

        String customId = JsonPath.read(createCustom.getResponse().getContentAsString(), "$.id");

        String recipePayload = """
                {
                  "name": "Conejo guisado",
                  "type": "DINNER",
                  "ingredients": [
                    { "ingredientId": "%s", "quantity": 500, "unit": "GRAM" }
                  ]
                }
                """.formatted(customId);

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recipePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value(customId));
    }
}
