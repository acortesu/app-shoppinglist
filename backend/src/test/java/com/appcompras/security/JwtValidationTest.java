package com.appcompras.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"app.security.require-auth=true",
		"app.security.google-client-id=test-client-id"
})
@AutoConfigureMockMvc
class JwtValidationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void missingAuthorizationHeaderReturns401() throws Exception {
		mockMvc.perform(get("/api/recipes"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void invalidAuthorizationSchemeReturns401() throws Exception {
		mockMvc.perform(get("/api/recipes")
				.header("Authorization", "Basic invalid"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void emptyBearerTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/recipes")
				.header("Authorization", "Bearer "))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointsRequireAuth() throws Exception {
		mockMvc.perform(get("/api/plans"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/shopping-lists"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/ingredients"))
				.andExpect(status().isUnauthorized());
	}
}
