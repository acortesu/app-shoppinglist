package com.appcompras.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"app.security.require-auth=false"
})
@AutoConfigureMockMvc
class ApiVersionFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void emptyVersionHeaderIsAllowed() throws Exception {
		mockMvc.perform(get("/api/recipes")
				.header("X-API-Version", ""))
				.andExpect(status().isOk());
	}

	@Test
	void missingVersionHeaderIsAllowed() throws Exception {
		mockMvc.perform(get("/api/recipes"))
				.andExpect(status().isOk());
	}

	@Test
	void versionOneIsSupported() throws Exception {
		mockMvc.perform(get("/api/recipes")
				.header("X-API-Version", "1"))
				.andExpect(status().isOk());
	}

	@Test
	void versionV1IsSupported() throws Exception {
		mockMvc.perform(get("/api/recipes")
				.header("X-API-Version", "v1"))
				.andExpect(status().isOk());
	}

	@Test
	void versionCaseInsensitive() throws Exception {
		mockMvc.perform(get("/api/recipes")
				.header("X-API-Version", "V1"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/recipes")
				.header("X-API-Version", "Version1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_API_VERSION"));
	}

	@Test
	void versionTwoIsNotSupported() throws Exception {
		mockMvc.perform(get("/api/recipes")
				.header("X-API-Version", "2"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_API_VERSION"));
	}

	@Test
	void malformedVersionIsRejected() throws Exception {
		mockMvc.perform(get("/api/recipes")
				.header("X-API-Version", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_API_VERSION"));
	}

	@Test
	void versionFilterOnlyAppliesToApiEndpoints() throws Exception {
		mockMvc.perform(get("/actuator/health")
				.header("X-API-Version", "99"))
				.andExpect(status().isOk());
	}

	@Test
	void unsupportedVersionReturnsProperError() throws Exception {
		mockMvc.perform(get("/api/plans")
				.header("X-API-Version", "v2"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_API_VERSION"))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.path").exists());
	}
}
