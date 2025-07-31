package com.example.copilot.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private Integer quantity;
    private Double price;
}
