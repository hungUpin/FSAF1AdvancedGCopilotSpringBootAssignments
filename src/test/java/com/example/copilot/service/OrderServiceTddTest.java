package com.example.copilot.service;

import com.example.copilot.entity.Order;
import com.example.copilot.entity.OrderItem;
import com.example.copilot.entity.Product;
import com.example.copilot.entity.User;
import com.example.copilot.entity.OrderStatus;
import com.example.copilot.exception.ResourceNotFoundException;
import com.example.copilot.repository.OrderRepository;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.repository.UserRepository;
import com.example.copilot.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Arrays;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTddTest {
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
    void testCancelPendingOrderRestoresStock() {
        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setStock(5);
        product.setPrice(19.99); // Add price

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);

        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderItems(Arrays.asList(item));

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        orderService.cancelOrder(100L);

        // Assert
        assertEquals(OrderStatus.CANCELLED.name(), order.getStatus().name());
        assertEquals(7, product.getStock()); // 5 + 2
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
    }

    @Test
    void testCancelNonExistentOrder() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderService.cancelOrder(999L);
        });
        assertTrue(exception.getMessage().toLowerCase().contains("order"));
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCancelNonPendingOrder() {
        // Arrange
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(100L);
        });
        assertTrue(exception.getMessage().toLowerCase().contains("pending"));
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testPlaceOrderSuccess() {
        // Arrange
        User user = new User();
        user.setId(1L);

        Product product1 = new Product();
        product1.setId(1L);
        product1.setStock(10);
        product1.setPrice(49.99); // Add price

        Product product2 = new Product();
        product2.setId(2L);
        product2.setStock(20);
        product2.setPrice(79.99); // Add price

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(productRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(orderItemRepository.saveAll(any())).thenReturn(java.util.Collections.emptyList());

        // Build CreateOrderRequestDTO
        com.example.copilot.dto.CreateOrderRequestDTO request = new com.example.copilot.dto.CreateOrderRequestDTO();
        request.setUserId(1L);
        com.example.copilot.dto.CreateOrderRequestDTO.Item item1 = new com.example.copilot.dto.CreateOrderRequestDTO.Item();
        item1.setProductId(1L);
        item1.setQuantity(2);
        com.example.copilot.dto.CreateOrderRequestDTO.Item item2 = new com.example.copilot.dto.CreateOrderRequestDTO.Item();
        item2.setProductId(2L);
        item2.setQuantity(3);
        request.setItems(Arrays.asList(item1, item2));

        // Act
        com.example.copilot.dto.OrderDTO orderDTO = orderService.placeOrder(request);

        // Assert
        assertNotNull(orderDTO);
        assertEquals(OrderStatus.PENDING.name(), orderDTO.getStatus());
        assertEquals(1L, orderDTO.getUserId());
        assertEquals(2, orderDTO.getItems().size());
        assertEquals(8, product1.getStock()); // 10 - 2
        assertEquals(17, product2.getStock()); // 20 - 3
        verify(productRepository).saveAll(any());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testPlaceOrderInsufficientStock() {
        // Arrange
        User user = new User();
        user.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setStock(5);
        product.setPrice(39.99); // Add price

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Build CreateOrderRequestDTO
        com.example.copilot.dto.CreateOrderRequestDTO request = new com.example.copilot.dto.CreateOrderRequestDTO();
        request.setUserId(1L);
        com.example.copilot.dto.CreateOrderRequestDTO.Item item = new com.example.copilot.dto.CreateOrderRequestDTO.Item();
        item.setProductId(1L);
        item.setQuantity(10); // Requesting more than available
        request.setItems(Arrays.asList(item));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(request);
        });
        assertTrue(exception.getMessage().toLowerCase().contains("insufficient stock"));
        assertEquals(5, product.getStock()); // Stock should remain unchanged
        verify(productRepository, never()).saveAll(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testPlaceOrderUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Build CreateOrderRequestDTO
        com.example.copilot.dto.CreateOrderRequestDTO request = new com.example.copilot.dto.CreateOrderRequestDTO();
        request.setUserId(999L);
        com.example.copilot.dto.CreateOrderRequestDTO.Item item = new com.example.copilot.dto.CreateOrderRequestDTO.Item();
        item.setProductId(1L);
        item.setQuantity(1);
        request.setItems(Arrays.asList(item));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(request);
        });
        assertTrue(exception.getMessage().toLowerCase().contains("user not found"));
        verify(productRepository, never()).saveAll(any());
        verify(orderRepository, never()).save(any());
    }
}
