package com.example.copilot.dto;

public class DashboardStatsDTO {
    private Double totalRevenue;
    private Long totalOrders;
    private Long newCustomersThisMonth;

    public DashboardStatsDTO() {}

    public DashboardStatsDTO(Double totalRevenue, Long totalOrders, Long newCustomersThisMonth) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.newCustomersThisMonth = newCustomersThisMonth;
    }

    // Getters and Setters
    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Long getNewCustomersThisMonth() {
        return newCustomersThisMonth;
    }

    public void setNewCustomersThisMonth(Long newCustomersThisMonth) {
        this.newCustomersThisMonth = newCustomersThisMonth;
    }
}
