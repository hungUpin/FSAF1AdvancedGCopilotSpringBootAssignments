package com.example.copilot.service;

import com.example.copilot.dto.CreateOrderRequestDTO;
import com.example.copilot.entity.*;
import com.example.copilot.repository.OrderRepository;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.repository.UserRepository;
import com.example.copilot.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTransactionTest {
    
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
    void testStockDeductedEvenWhenOrderSaveFails() {
        // Arrange
        User user = new User();
        user.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setStock(10);
        product.setPrice(99.99);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        
        // Mock product save to succeed (stock will be deducted)
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        
        // Mock order save to fail (simulating database constraint violation)
        when(orderRepository.save(any(Order.class)))
            .thenThrow(new DataIntegrityViolationException("Database constraint violation"));

        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(1L);
        CreateOrderRequestDTO.Item item = new CreateOrderRequestDTO.Item();
        item.setProductId(1L);
        item.setQuantity(3);
        request.setItems(Arrays.asList(item));

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            orderService.placeOrder(request);
        });

        // Verify that stock was indeed deducted even though order save failed
        assertEquals(7, product.getStock()); // 10 - 3 = 7
        
        // Verify that product save was called (stock was deducted)
        verify(productRepository).save(product);
        
        // Verify that order save was attempted but failed
        verify(orderRepository).save(any(Order.class));
        
        // Verify that order items were never saved due to the failure
        verify(orderItemRepository, never()).saveAll(any());
    }

    @Test
    void testStockDeductedEvenWhenOrderItemSaveFails() {
        // Arrange
        User user = new User();
        user.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setStock(10);
        product.setPrice(99.99);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        
        // Mock product save to succeed (stock will be deducted)
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        
        // Mock order save to succeed
        Order savedOrder = new Order();
        savedOrder.setId(100L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        
        // Mock order items save to fail
        when(orderItemRepository.saveAll(any()))
            .thenThrow(new DataIntegrityViolationException("OrderItem save failed"));

        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(1L);
        CreateOrderRequestDTO.Item item = new CreateOrderRequestDTO.Item();
        item.setProductId(1L);
        item.setQuantity(3);
        request.setItems(Arrays.asList(item));

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            orderService.placeOrder(request);
        });

        // Verify that stock was indeed deducted even though order items save failed
        assertEquals(7, product.getStock()); // 10 - 3 = 7
        
        // Verify that all operations were attempted
        verify(productRepository).save(product);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(any());
    }
}
