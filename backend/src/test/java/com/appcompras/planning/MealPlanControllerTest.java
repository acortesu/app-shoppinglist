package com.appcompras.planning;

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
class MealPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createPlanReturnsCreatedAndCanBeFetched() throws Exception {
        String recipeId = createRecipeAndGetId("LUNCH");

        String payload = """
                {
                  "startDate": "2026-02-09",
                  "period": "WEEK",
                  "slots": [
                    { "date": "2026-02-10", "mealType": "LUNCH", "recipeId": "%s" }
                  ]
                }
                """.formatted(recipeId);

        MvcResult createResult = mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.startDate").value("2026-02-09"))
                .andExpect(jsonPath("$.endDate").value("2026-02-15"))
                .andExpect(jsonPath("$.slots[0].recipeId").value(recipeId))
                .andReturn();

        String planId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/plans/{id}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId));

        mockMvc.perform(get("/api/recipes/{id}", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageCount").value(1))
                .andExpect(jsonPath("$.lastUsedAt").isNotEmpty());
    }

    @Test
    void getPlanByIdReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/plans/{id}", "missing-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPlanRejectsSlotOutOfRange() throws Exception {
        String recipeId = createRecipeAndGetId("LUNCH");

        String payload = """
                {
                  "startDate": "2026-02-09",
                  "period": "WEEK",
                  "slots": [
                    { "date": "2026-02-20", "mealType": "LUNCH", "recipeId": "%s" }
                  ]
                }
                """.formatted(recipeId);

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void createPlanRejectsDuplicateSlot() throws Exception {
        String recipeId = createRecipeAndGetId("LUNCH");

        String payload = """
                {
                  "startDate": "2026-02-09",
                  "period": "WEEK",
                  "slots": [
                    { "date": "2026-02-10", "mealType": "LUNCH", "recipeId": "%s" },
                    { "date": "2026-02-10", "mealType": "LUNCH", "recipeId": "%s" }
                  ]
                }
                """.formatted(recipeId, recipeId);

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void createPlanRejectsUnknownRecipe() throws Exception {
        String payload = """
                {
                  "startDate": "2026-02-09",
                  "period": "WEEK",
                  "slots": [
                    { "date": "2026-02-10", "mealType": "LUNCH", "recipeId": "unknown" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    private String createRecipeAndGetId(String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Receta base",
                                  "type": "%s",
                                  "ingredients": [
                                    { "ingredientId": "rice", "quantity": 1, "unit": "CUP" }
                                  ]
                                }
                                """.formatted(type)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
