package com.example.copilot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Interceptor for rate limiting requests, specifically targeting login endpoints
 * to prevent brute force attacks and protect against DoS attacks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, 
                           @NonNull HttpServletResponse response, 
                           @NonNull Object handler) throws Exception {
        
        String clientIp = getClientIpAddress(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("Rate limiting check for {} {} from IP: {}", method, requestUri, clientIp);
        
        // Apply strict rate limiting to login endpoint
        if (isLoginEndpoint(requestUri, method)) {
            if (!rateLimitingService.isLoginAllowed(clientIp)) {
                handleRateLimitExceeded(response, clientIp, "login");
                return false;
            }
        }
        // Apply general rate limiting to other endpoints (optional)
        else if (shouldApplyGeneralRateLimit(requestUri)) {
            if (!rateLimitingService.isGeneralRequestAllowed(clientIp)) {
                handleRateLimitExceeded(response, clientIp, "general");
                return false;
            }
        }
        
        return true;
    }

    /**
     * Checks if the current request is to the login endpoint.
     */
    private boolean isLoginEndpoint(String requestUri, String method) {
        return "POST".equalsIgnoreCase(method) && 
               requestUri != null && 
               requestUri.equals("/api/auth/login");
    }

    /**
     * Determines if general rate limiting should be applied to the endpoint.
     * You can customize this logic based on your requirements.
     */
    private boolean shouldApplyGeneralRateLimit(String requestUri) {
        // Apply rate limiting to all API endpoints except health checks and static resources
        return requestUri != null && 
               requestUri.startsWith("/api/") && 
               !requestUri.startsWith("/api/health") &&
               !requestUri.startsWith("/actuator/health");
    }

    /**
     * Handles rate limit exceeded scenario by returning appropriate HTTP response.
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String clientIp, String limitType) 
            throws IOException {
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // Add rate limit headers
        addRateLimitHeaders(response, clientIp, limitType);
        
        // Create JSON error response
        String jsonResponse = createRateLimitErrorResponse(limitType);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        
        log.warn("Rate limit exceeded for {} requests from IP: {} ({})", limitType, clientIp, limitType);
    }

    /**
     * Adds rate limiting headers to the response for client information.
     */
    private void addRateLimitHeaders(HttpServletResponse response, String clientIp, String limitType) {
        if ("login".equals(limitType)) {
            long availableTokens = rateLimitingService.getLoginAvailableTokens(clientIp);
            long retryAfterNanos = rateLimitingService.getLoginTimeUntilRefill(clientIp);
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(retryAfterNanos);
            
            response.setHeader("X-RateLimit-Limit", "10");
            response.setHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + retryAfterNanos / 1_000_000));
            response.setHeader("Retry-After", String.valueOf(Math.max(retryAfterSeconds, 1)));
        } else {
            // General rate limiting headers
            response.setHeader("X-RateLimit-Limit", "100");
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", "60");
        }
    }

    /**
     * Creates a JSON error response for rate limit exceeded scenarios.
     */
    private String createRateLimitErrorResponse(String limitType) {
        String message = "login".equals(limitType) ? 
            "Too many login attempts. Please try again later." :
            "Too many requests. Please slow down.";
            
        return String.format("""
            {
                "error": "Rate limit exceeded",
                "message": "%s",
                "type": "%s_rate_limit",
                "timestamp": "%s",
                "status": 429
            }
            """, message, limitType, LocalDateTime.now().toString());
    }

    /**
     * Extracts the real client IP address from the request, considering proxies.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check for X-Forwarded-For header (common with load balancers/proxies)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // Take the first IP in case of multiple IPs
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check for X-Real-IP header (Nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        // Check for Proxy-Client-IP header
        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp;
        }
        
        // Check for WL-Proxy-Client-IP header (WebLogic)
        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp;
        }
        
        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        
        // Handle IPv6 localhost
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        
        return remoteAddr;
    }
}
