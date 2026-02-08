package com.appcompras.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiVersionFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestWithoutVersionHeaderStillWorks() throws Exception {
        mockMvc.perform(get("/api/ingredients"))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithSupportedVersionHeaderStillWorks() throws Exception {
        mockMvc.perform(get("/api/ingredients")
                        .header("X-API-Version", "v1"))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithUnsupportedVersionReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/ingredients")
                        .header("X-API-Version", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_API_VERSION"))
                .andExpect(jsonPath("$.error").value("Unsupported API version. Use X-API-Version: 1"));
    }
}
