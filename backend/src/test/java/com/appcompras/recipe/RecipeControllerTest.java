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

    private String createRecipeAndGetId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Arroz con tomate",
                                  "type": "LUNCH",
                                  "ingredients": [
                                    { "ingredientId": "rice", "quantity": 1, "unit": "CUP" },
                                    { "ingredientId": "tomato", "quantity": 2, "unit": "PIECE" }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
