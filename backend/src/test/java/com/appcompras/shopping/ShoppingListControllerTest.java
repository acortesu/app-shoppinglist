package com.appcompras.shopping;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShoppingListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generateShoppingListFromPlanCreatesEditableDraft() throws Exception {
        String recipeRiceCup = createRecipeAndGetId("Rice cup", "LUNCH", "rice", 1, "CUP");
        String recipeRiceGrams = createRecipeAndGetId("Rice grams", "DINNER", "rice", 200, "GRAM");

        String planId = createPlanAndGetId(
                "2026-02-09",
                "WEEK",
                "2026-02-10", "LUNCH", recipeRiceCup,
                "2026-02-11", "DINNER", recipeRiceGrams
        );

        MvcResult result = mockMvc.perform(post("/api/shopping-lists/generate")
                        .param("planId", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.planId").value(planId))
                .andExpect(jsonPath("$.items[?(@.ingredientId=='rice')].quantity").value(org.hamcrest.Matchers.hasItem(380.0)))
                .andExpect(jsonPath("$.items[?(@.ingredientId=='rice')].unit").value(org.hamcrest.Matchers.hasItem("GRAM")))
                .andReturn();

        String shoppingListId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/shopping-lists/{id}", shoppingListId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shoppingListId));
    }

    @Test
    void updateShoppingListCanModifyDeleteAndAddManualItems() throws Exception {
        String recipeRice = createRecipeAndGetId("Rice", "LUNCH", "rice", 1, "CUP");
        String planId = createPlanAndGetId(
                "2026-02-09",
                "WEEK",
                "2026-02-10", "LUNCH", recipeRice,
                null, null, null
        );

        MvcResult generated = mockMvc.perform(post("/api/shopping-lists/generate")
                        .param("planId", planId))
                .andExpect(status().isOk())
                .andReturn();

        String shoppingListId = JsonPath.read(generated.getResponse().getContentAsString(), "$.id");
        String firstItemId = JsonPath.read(generated.getResponse().getContentAsString(), "$.items[0].id");

        String updatePayload = """
                {
                  "items": [
                    {
                      "id": "%s",
                      "ingredientId": "rice",
                      "name": "Rice",
                      "quantity": 500,
                      "unit": "GRAM",
                      "suggestedPackages": 1,
                      "packageAmount": 1,
                      "packageUnit": "KILOGRAM",
                      "manual": false
                    },
                    {
                      "name": "Papel higienico",
                      "quantity": 2,
                      "unit": "pack",
                      "manual": true
                    }
                  ]
                }
                """.formatted(firstItemId);

        mockMvc.perform(put("/api/shopping-lists/{id}", shoppingListId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(500.0))
                .andExpect(jsonPath("$.items[1].name").value("Papel higienico"))
                .andExpect(jsonPath("$.items[1].manual").value(true));
    }

    @Test
    void updateShoppingListRejectsInvalidQuantity() throws Exception {
        String recipeRice = createRecipeAndGetId("Rice", "LUNCH", "rice", 1, "CUP");
        String planId = createPlanAndGetId(
                "2026-02-09",
                "WEEK",
                "2026-02-10", "LUNCH", recipeRice,
                null, null, null
        );

        MvcResult generated = mockMvc.perform(post("/api/shopping-lists/generate")
                        .param("planId", planId))
                .andExpect(status().isOk())
                .andReturn();

        String shoppingListId = JsonPath.read(generated.getResponse().getContentAsString(), "$.id");

        String payload = """
                {
                  "items": [
                    {
                      "name": "Rice",
                      "quantity": 0,
                      "unit": "GRAM",
                      "manual": false
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/shopping-lists/{id}", shoppingListId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void getShoppingListsReturnsOrderedDrafts() throws Exception {
        String recipeA = createRecipeAndGetId("Rice A", "LUNCH", "rice", 1, "CUP");
        String planA = createPlanAndGetId(
                "2026-02-09",
                "WEEK",
                "2026-02-10", "LUNCH", recipeA,
                null, null, null
        );
        String firstId = generateShoppingListAndGetId(planA);

        String recipeB = createRecipeAndGetId("Rice B", "DINNER", "rice", 200, "GRAM");
        String planB = createPlanAndGetId(
                "2026-02-16",
                "WEEK",
                "2026-02-17", "DINNER", recipeB,
                null, null, null
        );
        String secondId = generateShoppingListAndGetId(planB);

        MvcResult listResult = mockMvc.perform(get("/api/shopping-lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        java.util.List<String> ids = JsonPath.read(listResult.getResponse().getContentAsString(), "$[*].id");
        int firstIndex = ids.indexOf(firstId);
        int secondIndex = ids.indexOf(secondId);
        Assertions.assertTrue(secondIndex >= 0 && firstIndex >= 0 && secondIndex < firstIndex);
    }

    @Test
    void deleteShoppingListReturnsNoContentAndRemovesDraft() throws Exception {
        String recipeId = createRecipeAndGetId("Rice", "LUNCH", "rice", 1, "CUP");
        String planId = createPlanAndGetId(
                "2026-02-09",
                "WEEK",
                "2026-02-10", "LUNCH", recipeId,
                null, null, null
        );
        String shoppingListId = generateShoppingListAndGetId(planId);

        mockMvc.perform(delete("/api/shopping-lists/{id}", shoppingListId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/shopping-lists/{id}", shoppingListId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShoppingListReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(delete("/api/shopping-lists/{id}", "missing-id"))
                .andExpect(status().isNotFound());
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

    private String generateShoppingListAndGetId(String planId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shopping-lists/generate")
                        .param("planId", planId))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
