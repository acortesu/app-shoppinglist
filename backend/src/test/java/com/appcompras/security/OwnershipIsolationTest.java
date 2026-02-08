package com.appcompras.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.require-auth=true",
        "app.security.google-client-id=test-client-id"
})
@AutoConfigureMockMvc
class OwnershipIsolationTest {

    private static final String USER_A = "owner-user-a";
    private static final String USER_B = "owner-user-b";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void resourcesAreIsolatedByAuthenticatedUser() throws Exception {
        String recipeId = createRecipeAs(USER_A);
        String planId = createPlanAs(USER_A, recipeId);
        String shoppingListId = generateShoppingListAs(USER_A, planId);

        mockMvc.perform(get("/api/recipes/{id}", recipeId).with(jwt().jwt(builder -> builder.subject(USER_B))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/plans/{id}", planId).with(jwt().jwt(builder -> builder.subject(USER_B))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/shopping-lists/{id}", shoppingListId).with(jwt().jwt(builder -> builder.subject(USER_B))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/recipes").with(jwt().jwt(builder -> builder.subject(USER_B))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));

        mockMvc.perform(get("/api/plans").with(jwt().jwt(builder -> builder.subject(USER_B))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));

        mockMvc.perform(get("/api/shopping-lists").with(jwt().jwt(builder -> builder.subject(USER_B))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    private String createRecipeAs(String userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recipes")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Recipe owner",
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

    private String createPlanAs(String userId, String recipeId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/plans")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
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

    private String generateShoppingListAs(String userId, String planId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shopping-lists/generate")
                        .with(jwt().jwt(builder -> builder.subject(userId)))
                        .param("planId", planId))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
