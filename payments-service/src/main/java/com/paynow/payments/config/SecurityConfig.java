package com.paynow.payments.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paynow.common.error.PaymentError;
import com.paynow.common.util.CorrelationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

import java.io.IOException;
import java.util.Set;

/**
 * Security configuration with API key authentication
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {
    
    @Value("${app.api.keys:payment-api-key,internal-service-key}")
    private Set<String> validApiKeys;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers("/payments/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilter(apiKeyAuthenticationFilter())
            .authenticationProvider(preAuthenticatedAuthenticationProvider())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint()));

        return http.build();
    }

    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter() {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(validApiKeys);
        filter.setAuthenticationManager(authentication -> {
            if (authentication.isAuthenticated()) {
                return authentication;
            }
            throw new org.springframework.security.core.AuthenticationException("Invalid API key") {};
        });
        return filter;
    }

    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(token -> {
            String apiKey = (String) token.getPrincipal();
            return new org.springframework.security.core.userdetails.User(
                    apiKey, "", true, true, true, true, 
                    Set.of(() -> "ROLE_API_USER"));
        });
        return provider;
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint();
    }

    /**
     * Custom API Key authentication filter
     */
    public static class ApiKeyAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
        
        private static final String API_KEY_HEADER = "X-API-Key";
        private final Set<String> validApiKeys;

        public ApiKeyAuthenticationFilter(Set<String> validApiKeys) {
            this.validApiKeys = validApiKeys;
        }

        @Override
        protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            if (apiKey != null && validApiKeys.contains(apiKey)) {
                log.debug("Valid API key provided");
                return apiKey;
            }
            log.debug("Invalid or missing API key");
            return null;
        }

        @Override
        protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
            return "N/A";
        }
    }

    /**
     * Custom authentication entry point for API key errors
     */
    public static class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
        
        private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                           AuthenticationException authException) throws IOException {
            
            String requestId = CorrelationUtils.generateRequestId();
            log.warn("Authentication failed for request: {} - {}", request.getRequestURI(), authException.getMessage());

            PaymentError error = PaymentError.unauthorized(
                    "Invalid or missing API key", requestId, request.getRequestURI());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.setHeader(CorrelationUtils.REQUEST_ID_HEADER, requestId);
            
            String errorJson = objectMapper.writeValueAsString(error);
            response.getWriter().write(errorJson);
        }
    }
}