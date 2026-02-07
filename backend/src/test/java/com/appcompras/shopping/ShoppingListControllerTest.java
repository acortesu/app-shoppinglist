package com.appcompras.shopping;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShoppingListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generateShoppingListFromPlanAggregatesIngredients() throws Exception {
        String recipeRiceCup = createRecipeAndGetId("Rice cup", "LUNCH", "rice", 1, "CUP");
        String recipeRiceGrams = createRecipeAndGetId("Rice grams", "DINNER", "rice", 200, "GRAM");

        String planId = createPlanAndGetId(
                "2026-02-09",
                "WEEK",
                "2026-02-10", "LUNCH", recipeRiceCup,
                "2026-02-11", "DINNER", recipeRiceGrams
        );

        mockMvc.perform(post("/api/shopping-lists/generate")
                        .param("planId", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(planId))
                .andExpect(jsonPath("$.items[?(@.ingredientId=='rice')].requiredBaseAmount").value(org.hamcrest.Matchers.hasItem(380.0)))
                .andExpect(jsonPath("$.items[?(@.ingredientId=='rice')].baseUnit").value(org.hamcrest.Matchers.hasItem("GRAM")));
    }

    @Test
    void generateShoppingListReturnsNotFoundWhenPlanMissing() throws Exception {
        mockMvc.perform(post("/api/shopping-lists/generate")
                        .param("planId", "missing-plan"))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateShoppingListReturnsBadRequestWhenRecipeMissingInPlan() throws Exception {
        String recipeId = createRecipeAndGetId("Rice cup", "LUNCH", "rice", 1, "CUP");
        String planId = createPlanAndGetId(
                "2026-02-09",
                "WEEK",
                "2026-02-10", "LUNCH", recipeId,
                null, null, null
        );

        mockMvc.perform(delete("/api/recipes/{id}", recipeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/shopping-lists/generate")
                        .param("planId", planId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    private String createRecipeAndGetId(String name, String type, String ingredientId, double quantity, String unit)
            throws Exception {
        String payload = """
                {
                  "name": "%s",
                  "type": "%s",
                  "ingredients": [
                    { "ingredientId": "%s", "quantity": %s, "unit": "%s" }
                  ]
                }
                """.formatted(name, type, ingredientId, quantity, unit);

        MvcResult result = mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createPlanAndGetId(
            String startDate,
            String period,
            String date1,
            String mealType1,
            String recipeId1,
            String date2,
            String mealType2,
            String recipeId2
    ) throws Exception {
        String secondSlot = "";
        if (date2 != null && mealType2 != null && recipeId2 != null) {
            secondSlot = ",\n    { \"date\": \"%s\", \"mealType\": \"%s\", \"recipeId\": \"%s\" }"
                    .formatted(date2, mealType2, recipeId2);
        }

        String payload = """
                {
                  "startDate": "%s",
                  "period": "%s",
                  "slots": [
                    { "date": "%s", "mealType": "%s", "recipeId": "%s" }%s
                  ]
                }
                """.formatted(startDate, period, date1, mealType1, recipeId1, secondSlot);

        MvcResult result = mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
