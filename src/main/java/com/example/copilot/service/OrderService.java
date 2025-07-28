package com.example.copilot.service;

import com.example.copilot.dto.CreateOrderRequestDTO;
import com.example.copilot.dto.OrderDTO;
import org.springframework.transaction.annotation.Transactional;

public interface OrderService {
    //@Transactional
    OrderDTO placeOrder(CreateOrderRequestDTO request);
    void cancelOrder(Long orderId);
    boolean hasUserPurchasedProduct(Long userId, Long productId);
    // Other CRUD methods can be added as needed
}
