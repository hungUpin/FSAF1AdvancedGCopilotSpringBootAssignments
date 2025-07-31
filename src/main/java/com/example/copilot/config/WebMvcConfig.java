package com.example.copilot.config;

import com.example.copilot.security.RateLimitingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for registering interceptors.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register rate limiting interceptor specifically for login endpoint
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/auth/login")  // Primary target: login endpoint
                .addPathPatterns("/api/**")          // Optional: apply to all API endpoints
                .excludePathPatterns(
                    "/api/health/**",               // Exclude health check endpoints
                    "/actuator/**",                 // Exclude actuator endpoints
                    "/swagger-ui/**",               // Exclude Swagger UI
                    "/v3/api-docs/**",              // Exclude OpenAPI docs
                    "/error"                        // Exclude error handling
                );
    }
}
