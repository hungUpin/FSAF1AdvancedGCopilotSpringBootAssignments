package com.example.copilot.service;

import com.example.copilot.dto.CreateOrderRequestDTO;
import com.example.copilot.entity.Order;
import com.example.copilot.entity.Product;
import com.example.copilot.entity.User;
import com.example.copilot.repository.OrderItemRepository;
import com.example.copilot.repository.OrderRepository;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.repository.UserRepository;
import com.example.copilot.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceNPEMappingTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderItemRepository orderItemRepository;

    @Test
    void testNullOrderItemsInMapping() {
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(1L);
        CreateOrderRequestDTO.Item item = new CreateOrderRequestDTO.Item();
        item.setProductId(1L);
        item.setQuantity(1);
        request.setItems(Arrays.asList(item));

        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(1L);
        product.setStock(10);
        product.setPrice(99.99);

        // Create an order that will be returned with null orderItems
        Order savedOrder = new Order();
        savedOrder.setId(100L);
        savedOrder.setOrderItems(null); // This will cause NPE in mapping

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.saveAll(any())).thenReturn(Arrays.asList(product));
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderItemRepository.saveAll(any())).thenReturn(Arrays.asList());

        // After the fix, this should NOT throw NPE anymore
        // Instead it should handle null gracefully
        try {
            orderService.placeOrder(request);
        } catch (NullPointerException e) {
            throw new AssertionError("NPE should be fixed, but still occurred: " + e.getMessage());
        } catch (Exception e) {
            // Other exceptions are acceptable
        }
    }
}
