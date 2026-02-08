package com.appcompras.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

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
class ApiSecurityAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void requestWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void requestWithJwtCanAccessProtectedEndpoints() throws Exception {
        String payload = """
                {
                  "name": "Auth recipe",
                  "type": "LUNCH",
                  "ingredients": [
                    { "ingredientId": "rice", "quantity": 1, "unit": "CUP" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/recipes")
                        .with(jwt().jwt(builder -> builder.subject("user-auth-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Auth recipe"));
    }
}
