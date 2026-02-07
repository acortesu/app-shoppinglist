package com.appcompras.recipe;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createRecipeReturnsCreated() throws Exception {
        String payload = """
                {
                  "name": "Arroz con tomate",
                  "type": "LUNCH",
                  "ingredients": [
                    { "ingredientId": "rice", "quantity": 1, "unit": "CUP" },
                    { "ingredientId": "tomato", "quantity": 2, "unit": "PIECE" }
                  ],
                  "preparation": "Mezclar y cocinar",
                  "notes": "MVP",
                  "tags": ["rapido"]
                }
                """;

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Arroz con tomate"))
                .andExpect(jsonPath("$.type").value("LUNCH"))
                .andExpect(jsonPath("$.usageCount").value(0));
    }

    @Test
    void createRecipeResolvesAliasToCanonicalIngredientId() throws Exception {
        String payload = """
                {
                  "name": "Arroz con tomate",
                  "type": "LUNCH",
                  "ingredients": [
                    { "ingredientId": "Arroz", "quantity": 1, "unit": "CUP" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value("rice"));
    }

    @Test
    void createRecipeRejectsUnknownIngredient() throws Exception {
        String payload = """
                {
                  "name": "Conejo al horno",
                  "type": "DINNER",
                  "ingredients": [
                    { "ingredientId": "conejo", "quantity": 1, "unit": "PIECE" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void getRecipeByIdReturnsRecipe() throws Exception {
        String id = createRecipeAndGetId();

        mockMvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Arroz con tomate"));
    }

    @Test
    void getRecipeByIdReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/recipes/{id}", "missing-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecipesReturnsList() throws Exception {
        createRecipeAndGetId();

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").isNotEmpty());
    }

    @Test
    void getRecipesCanFilterByTypeAndKeepDescOrder() throws Exception {
        createRecipeAndGetId("Desayuno 1", "BREAKFAST");
        String secondBreakfastId = createRecipeAndGetId("Desayuno 2", "BREAKFAST");
        createRecipeAndGetId("Almuerzo 1", "LUNCH");

        mockMvc.perform(get("/api/recipes?type=BREAKFAST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(secondBreakfastId))
                .andExpect(jsonPath("$[0].type").value("BREAKFAST"));
    }

    @Test
    void updateRecipeReturnsUpdatedRecipe() throws Exception {
        String id = createRecipeAndGetId();

        String payload = """
                {
                  "name": "Arroz con huevo",
                  "type": "DINNER",
                  "ingredients": [
                    { "ingredientId": "rice", "quantity": 200, "unit": "GRAM" },
                    { "ingredientId": "egg", "quantity": 2, "unit": "PIECE" }
                  ],
                  "notes": "actualizada"
                }
                """;

        mockMvc.perform(put("/api/recipes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Arroz con huevo"))
                .andExpect(jsonPath("$.type").value("DINNER"))
                .andExpect(jsonPath("$.ingredients[1].ingredientId").value("egg"))
                .andExpect(jsonPath("$.notes").value("actualizada"));
    }

    @Test
    void updateRecipeReturnsNotFoundWhenMissing() throws Exception {
        String payload = """
                {
                  "name": "Arroz con huevo",
                  "type": "DINNER",
                  "ingredients": [
                    { "ingredientId": "rice", "quantity": 200, "unit": "GRAM" }
                  ]
                }
                """;

        mockMvc.perform(put("/api/recipes/{id}", "missing-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRecipeRejectsInvalidQuantity() throws Exception {
        String id = createRecipeAndGetId();

        String payload = """
                {
                  "name": "Arroz con huevo",
                  "type": "DINNER",
                  "ingredients": [
                    { "ingredientId": "rice", "quantity": 0, "unit": "GRAM" }
                  ]
                }
                """;

        mockMvc.perform(put("/api/recipes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void deleteRecipeReturnsNoContent() throws Exception {
        String id = createRecipeAndGetId();

        mockMvc.perform(delete("/api/recipes/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRecipeReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(delete("/api/recipes/{id}", "missing-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRecipeRejectsInvalidBody() throws Exception {
        String payload = """
                {
                  "name": "",
                  "type": "LUNCH",
                  "ingredients": []
                }
                """;

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void createRecipeRejectsZeroQuantity() throws Exception {
        String payload = """
                {
                  "name": "Arroz sin cantidad",
                  "type": "LUNCH",
                  "ingredients": [
                    { "ingredientId": "rice", "quantity": 0, "unit": "GRAM" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    private String createRecipeAndGetId() throws Exception {
        return createRecipeAndGetId("Arroz con tomate", "LUNCH");
    }

    private String createRecipeAndGetId(String name, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "type": "%s",
                                  "ingredients": [
                                    { "ingredientId": "rice", "quantity": 1, "unit": "CUP" },
                                    { "ingredientId": "tomato", "quantity": 2, "unit": "PIECE" }
                                  ]
                                }
                                """.formatted(name, type)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
