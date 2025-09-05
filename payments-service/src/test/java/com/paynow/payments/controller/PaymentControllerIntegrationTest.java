package com.paynow.payments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paynow.common.dto.PaymentDecisionRequest;
import com.paynow.common.dto.PaymentDecisionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for PaymentController
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRejectRequestWithoutApiKey() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        PaymentDecisionRequest request = new PaymentDecisionRequest(
                "c_123", new BigDecimal("100.00"), "USD", "p_789", "idmp_123"
        );

        mockMvc.perform(post("/payments/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser
    void shouldValidateRequestBody() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        PaymentDecisionRequest invalidRequest = new PaymentDecisionRequest();
        // Missing required fields

        mockMvc.perform(post("/payments/decide")
                        .header("X-API-Key", "payment-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}