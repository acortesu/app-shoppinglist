package com.appcompras.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"app.security.require-auth=false",
		"app.cors.allowed-origins=https://www.acortesdev.xyz,http://localhost:5173"
})
@AutoConfigureMockMvc
class CorsTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allowedOriginPreflightSucceeds() throws Exception {
		mockMvc.perform(options("/api/recipes")
				.header("Origin", "https://www.acortesdev.xyz")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().exists("Access-Control-Allow-Origin"));
	}

	@Test
	void allowedLocalOriginPreflightSucceeds() throws Exception {
		mockMvc.perform(options("/api/recipes")
				.header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk())
				.andExpect(header().exists("Access-Control-Allow-Origin"));
	}

	@Test
	void rejectedOriginPreflightBlocked() throws Exception {
		mockMvc.perform(options("/api/recipes")
				.header("Origin", "https://evil.example.com")
				.header("Access-Control-Request-Method", "DELETE"))
				.andExpect(status().isForbidden());
	}

	@Test
	void unknownOriginPreflightBlocked() throws Exception {
		mockMvc.perform(options("/api/plans")
				.header("Origin", "https://attacker.com")
				.header("Access-Control-Request-Method", "PUT"))
				.andExpect(status().isForbidden());
	}
}
