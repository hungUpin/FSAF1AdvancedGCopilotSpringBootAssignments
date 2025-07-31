package com.example.copilot.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Data
public class CreateOrderRequestDTO {
    @NotNull
    private Long userId;
    @NotNull
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long productId;
        @NotNull
        @Positive
        private Integer quantity;
    }
}
