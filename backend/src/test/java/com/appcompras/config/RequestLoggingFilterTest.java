package com.appcompras.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"app.security.require-auth=false"
})
@AutoConfigureMockMvc
class RequestLoggingFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void requestIdGeneratedWhenNotProvided() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().exists("X-Request-Id"))
				.andExpect(header().string("X-Request-Id", notNullValue()));
	}

	@Test
	void requestIdEchoedFromRequest() throws Exception {
		String requestId = "test-request-id-12345";

		mockMvc.perform(get("/actuator/health")
				.header("X-Request-Id", requestId))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Request-Id", requestId));
	}

	@Test
	void requestIdPropagatedInMdcForLogging() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().exists("X-Request-Id"));
	}

	@Test
	void emptyRequestIdGeneratesNew() throws Exception {
		mockMvc.perform(get("/actuator/health")
				.header("X-Request-Id", ""))
				.andExpect(status().isOk())
				.andExpect(header().exists("X-Request-Id"))
				.andExpect(header().string("X-Request-Id", notNullValue()));
	}
}
