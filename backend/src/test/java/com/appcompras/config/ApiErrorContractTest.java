package com.appcompras.config;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownIngredientReturnsIngredientNotFoundCode() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Conejo",
                                  "type": "DINNER",
                                  "ingredients": [
                                    { "ingredientId": "conejo", "quantity": 1, "unit": "PIECE" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
    }

    @Test
    void invalidIngredientUnitReturnsSpecificCode() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Huevo invalido",
                                  "type": "DINNER",
                                  "ingredients": [
                                    { "ingredientId": "egg", "quantity": 1, "unit": "LITER" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INGREDIENT_UNIT"));
    }

    @Test
    void duplicatePlanSlotReturnsSpecificCode() throws Exception {
        String recipeId = createRecipeAndGetId();

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-02-09",
                                  "period": "WEEK",
                                  "slots": [
                                    { "date": "2026-02-10", "mealType": "LUNCH", "recipeId": "%s" },
                                    { "date": "2026-02-10", "mealType": "LUNCH", "recipeId": "%s" }
                                  ]
                                }
                                """.formatted(recipeId, recipeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PLAN_DUPLICATE_SLOT"));
    }

    @Test
    void slotOutOfRangeReturnsSpecificCode() throws Exception {
        String recipeId = createRecipeAndGetId();

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-02-09",
                                  "period": "WEEK",
                                  "slots": [
                                    { "date": "2026-02-20", "mealType": "LUNCH", "recipeId": "%s" }
                                  ]
                                }
                                """.formatted(recipeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PLAN_SLOT_OUT_OF_RANGE"));
    }

    @Test
    void shoppingGenerationWhenPlanRecipeMissingReturnsSpecificCode() throws Exception {
        String recipeId = createRecipeAndGetId();
        String planId = createPlanAndGetId(recipeId);

        mockMvc.perform(delete("/api/recipes/{id}", recipeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/shopping-lists/generate")
                        .param("planId", planId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PLAN_RECIPE_NOT_FOUND"));
    }

    @Test
    void shoppingUpdateWithoutIngredientInNonManualItemReturnsSpecificCode() throws Exception {
        String recipeId = createRecipeAndGetId();
        String planId = createPlanAndGetId(recipeId);
        String draftId = generateShoppingListAndGetId(planId);

        mockMvc.perform(put("/api/shopping-lists/{id}", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "name": "Rice",
                                      "quantity": 1,
                                      "unit": "GRAM",
                                      "manual": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SHOPPING_ITEM_INGREDIENT_REQUIRED"));
    }

    private String createRecipeAndGetId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Receta contrato",
                                  "type": "LUNCH",
                                  "ingredients": [
                                    { "ingredientId": "rice", "quantity": 1, "unit": "CUP" }
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
                                    { "date": "2026-02-10", "mealType": "LUNCH", "recipeId": "%s" }
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
