package com.example.copilot.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private LocalDateTime orderDate;
    private String status;
    private Long userId;
    private List<OrderItemDTO> items;
}
