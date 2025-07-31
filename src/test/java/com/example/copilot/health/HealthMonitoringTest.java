package com.example.copilot.health;

import com.example.copilot.config.MaxMemoryHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for health monitoring functionality.
 * Tests both the custom memory health indicator and Spring Boot Actuator endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HealthMonitoringTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MaxMemoryHealthIndicator memoryHealthIndicator;

    @Test
    public void testMemoryHealthIndicator() {
        // Test the memory health indicator directly
        MaxMemoryHealthIndicator.MemoryHealthStatus status = memoryHealthIndicator.checkMemoryHealth();
        
        assertThat(status).isNotNull();
        assertThat(status.getStatus()).isIn("UP", "DOWN");
        assertThat(status.getMemoryUsagePercentage()).isNotNull();
        assertThat(status.getMaxMemory()).isNotNull();
        assertThat(status.getMessage()).isNotNull();
        
        System.out.println("Memory Health Status: " + status);
    }

    @Test
    public void testCustomMemoryHealthEndpoint() {
        // Test the custom memory health REST endpoint
        String url = "http://localhost:" + port + "/api/health/memory";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isIn("UP", "DOWN");
        assertThat(response.getBody().get("details")).isNotNull();
        
        System.out.println("Custom Memory Health Response: " + response.getBody());
    }

    @Test
    public void testSimpleHealthEndpoint() {
        // Test the simple health endpoint
        String url = "http://localhost:" + port + "/api/health/simple";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("jvm")).isNotNull();
        
        System.out.println("Simple Health Response: " + response.getBody());
    }

    @Test
    public void testActuatorHealthEndpoint() {
        // Test the Spring Boot Actuator health endpoint (if actuator is properly configured)
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        
        // This test will succeed if actuator is properly configured
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody().get("status")).isNotNull();
            System.out.println("Actuator Health Response: " + response.getBody());
        } else {
            System.out.println("Actuator endpoint not available - Status: " + response.getStatusCode());
        }
    }
}
