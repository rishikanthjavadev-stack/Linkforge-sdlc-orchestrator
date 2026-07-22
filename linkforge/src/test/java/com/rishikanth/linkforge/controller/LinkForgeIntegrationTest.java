package com.rishikanth.linkforge.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LinkForgeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shortenRedirectAndAnalyticsRoundTrip() throws Exception {
        // 1. Shorten a URL
        String requestBody = """
                {"longUrl": "https://www.example.com/some/very/long/path?query=1"}
                """;

        String responseJson = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.shortUrl").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(responseJson);
        String code = node.get("code").asText();

        // 2. Redirect should 302 to the original long URL
        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "https://www.example.com/some/very/long/path?query=1"));

        // 3. Analytics should reflect one click
        mockMvc.perform(get("/api/analytics/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(1))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void shortenRejectsInvalidUrl() throws Exception {
        String requestBody = """
                {"longUrl": "not-a-valid-url"}
                """;

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redirectReturns404ForUnknownCode() throws Exception {
        mockMvc.perform(get("/ZZZZZZ999"))
                .andExpect(status().isNotFound());
    }
}
