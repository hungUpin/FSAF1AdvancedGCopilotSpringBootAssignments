package com.example.copilot.config;

import org.springframework.stereotype.Component;

/**
 * Custom Health Indicator that monitors JVM memory usage.
 * Reports DOWN status if used memory exceeds 90% of maximum memory.
 * 
 * Note: This is a simplified version without Spring Boot Actuator dependencies
 * for compilation compatibility. To fully enable this, ensure spring-boot-starter-actuator
 * is properly included in your pom.xml dependencies.
 */
@Component
public class MaxMemoryHealthIndicator {

    private static final double MEMORY_THRESHOLD = 0.90; // 90%
    
    /**
     * Manual health check method - can be called programmatically
     * In a full Spring Boot Actuator setup, this would implement HealthIndicator interface
     */
    public MemoryHealthStatus checkMemoryHealth() {
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Calculate memory usage percentage
        double memoryUsageRatio = (double) usedMemory / maxMemory;
        double memoryUsagePercentage = memoryUsageRatio * 100;
        
        // Create health status
        MemoryHealthStatus status = new MemoryHealthStatus();
        status.setMaxMemory(formatBytes(maxMemory));
        status.setTotalMemory(formatBytes(totalMemory));
        status.setUsedMemory(formatBytes(usedMemory));
        status.setFreeMemory(formatBytes(freeMemory));
        status.setMemoryUsagePercentage(String.format("%.2f%%", memoryUsagePercentage));
        status.setThreshold(String.format("%.1f%%", MEMORY_THRESHOLD * 100));
        
        // Determine health status
        if (memoryUsageRatio > MEMORY_THRESHOLD) {
            status.setStatus("DOWN");
            status.setMessage("Memory usage exceeded threshold - Consider increasing heap size or optimizing memory usage");
        } else {
            status.setStatus("UP");
            status.setMessage("Memory usage within acceptable limits");
        }
        
        return status;
    }
    
    /**
     * Formats bytes into human-readable format (KB, MB, GB)
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Inner class to hold memory health status information
     */
    public static class MemoryHealthStatus {
        private String status;
        private String message;
        private String maxMemory;
        private String totalMemory;
        private String usedMemory;
        private String freeMemory;
        private String memoryUsagePercentage;
        private String threshold;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getMaxMemory() { return maxMemory; }
        public void setMaxMemory(String maxMemory) { this.maxMemory = maxMemory; }
        
        public String getTotalMemory() { return totalMemory; }
        public void setTotalMemory(String totalMemory) { this.totalMemory = totalMemory; }
        
        public String getUsedMemory() { return usedMemory; }
        public void setUsedMemory(String usedMemory) { this.usedMemory = usedMemory; }
        
        public String getFreeMemory() { return freeMemory; }
        public void setFreeMemory(String freeMemory) { this.freeMemory = freeMemory; }
        
        public String getMemoryUsagePercentage() { return memoryUsagePercentage; }
        public void setMemoryUsagePercentage(String memoryUsagePercentage) { this.memoryUsagePercentage = memoryUsagePercentage; }
        
        public String getThreshold() { return threshold; }
        public void setThreshold(String threshold) { this.threshold = threshold; }
        
        @Override
        public String toString() {
            return String.format("MemoryHealth{status='%s', usage='%s', message='%s'}", 
                status, memoryUsagePercentage, message);
        }
    }
}
