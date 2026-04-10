package com.streametl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streametl.config.EtlProperties;
import com.streametl.datalake.LayerProcessor;
import com.streametl.mapper.PipelineMapper;
import com.streametl.service.PipelineService;
import com.streametl.violation.ViolationDetector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PipelineController.class)
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        PipelineService pipelineService() {
            return new PipelineService(
                    new ViolationDetector(),
                    new LayerProcessor(),
                    new EtlProperties(
                            new EtlProperties.ViolationProperties("WARNING"),
                            new EtlProperties.WindowProperties(300),
                            new EtlProperties.DataLakeProperties(true)));
        }

        @Bean
        PipelineMapper pipelineMapper() {
            return new PipelineMapper();
        }
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/pipeline/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("stream-etl-engine"));
    }

    @Test
    void processReturnsTransformedRecord() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("payload", Map.of("id", "1", "name", "test")));

        mockMvc.perform(post("/api/v1/pipeline/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layer").value("BRONZE"))
                .andExpect(jsonPath("$.sourceId").value("api"));
    }

    @Test
    void processWithMissingIdReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("payload", Map.of("name", "no-id")));

        mockMvc.perform(post("/api/v1/pipeline/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void processWithEmptyPayloadReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("payload", Map.of()));

        mockMvc.perform(post("/api/v1/pipeline/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void detectViolationsReturnsViolations() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("payload", Map.of("price", -10, "quantity", -5)));

        mockMvc.perform(post("/api/v1/pipeline/detect-violations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void detectViolationsReturnsEmptyForCleanRecord() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("payload", Map.of("price", 100, "quantity", 5)));

        mockMvc.perform(post("/api/v1/pipeline/detect-violations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void promoteToGoldReturnsGoldLayer() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("payload", Map.of("id", "1", "value", 42)));

        mockMvc.perform(post("/api/v1/pipeline/promote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .param("targetLayer", "GOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layer").value("GOLD"));
    }

    @Test
    void promoteWithInvalidLayerReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("payload", Map.of("id", "1")));

        mockMvc.perform(post("/api/v1/pipeline/promote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .param("targetLayer", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void promoteWithLowercaseLayerReturnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("payload", Map.of("id", "1", "value", 42)));

        mockMvc.perform(post("/api/v1/pipeline/promote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .param("targetLayer", "gold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layer").value("GOLD"));
    }
}
