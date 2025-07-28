package com.example.copilot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.example.copilot.entity.Order;
import com.example.copilot.entity.OrderItem;
import com.example.copilot.entity.Product;
import com.example.copilot.entity.User;
import com.example.copilot.entity.OrderStatus;
import com.example.copilot.repository.OrderRepository;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.repository.UserRepository;
import com.example.copilot.service.impl.OrderServiceImpl;
import java.util.Arrays;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class OrderServiceUnitTest {
    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.example.copilot.repository.OrderItemRepository orderItemRepository;

    @Test
    void testPlaceOrderSuccess() {
        // Arrange
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(2L);
        product.setStock(10);
        product.setPrice(99.99); // Add price to fix IllegalStateException
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(3);
        Order order = new Order();
        order.setUser(user);
        order.setOrderItems(Arrays.asList(item));
        order.setStatus(OrderStatus.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(productRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(orderItemRepository.saveAll(any())).thenReturn(java.util.Collections.emptyList());

        // Build CreateOrderRequestDTO
        com.example.copilot.dto.CreateOrderRequestDTO request = new com.example.copilot.dto.CreateOrderRequestDTO();
        request.setUserId(1L);
        com.example.copilot.dto.CreateOrderRequestDTO.Item itemDto = new com.example.copilot.dto.CreateOrderRequestDTO.Item();
        itemDto.setProductId(2L);
        itemDto.setQuantity(3);
        request.setItems(Arrays.asList(itemDto));

        // Act
        com.example.copilot.dto.OrderDTO placedOrder = orderService.placeOrder(request);

        // Assert
        assertNotNull(placedOrder);
        assertEquals("PENDING", placedOrder.getStatus());
        assertEquals(1, placedOrder.getItems().size());
        assertEquals(7, product.getStock()); // 10 - 3
        verify(orderRepository).save(any(Order.class));
        verify(productRepository).saveAll(any());
    }
}
