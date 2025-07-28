package com.example.copilot.controller;

import com.example.copilot.dto.DashboardStatsDTO;
import com.example.copilot.repository.ReviewRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "Admin dashboard statistics")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReviewRepository reviewRepository;

    @Operation(summary = "Get Dashboard Statistics", 
               description = "Retrieve key business metrics including revenue, orders, and new customers (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        Object[] stats = reviewRepository.getDashboardStats();
        
        if (stats != null && stats.length >= 3) {
            // Extract values from the query result
            Double totalRevenue = stats[0] != null ? ((Number) stats[0]).doubleValue() : 0.0;
            Long totalOrders = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
            Long newCustomersThisMonth = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
            
            DashboardStatsDTO dashboardStats = new DashboardStatsDTO(
                totalRevenue, 
                totalOrders, 
                newCustomersThisMonth
            );
            
            return ResponseEntity.ok(dashboardStats);
        } else {
            // Return default values if query fails
            DashboardStatsDTO defaultStats = new DashboardStatsDTO(0.0, 0L, 0L);
            return ResponseEntity.ok(defaultStats);
        }
    }
}
