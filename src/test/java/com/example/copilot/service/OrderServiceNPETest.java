package com.example.copilot.service;

import com.example.copilot.dto.CreateOrderRequestDTO;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceNPETest {

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
    void testNullRequest() {
        // Test with null request
        assertThrows(NullPointerException.class, () -> {
            orderService.placeOrder(null);
        });
    }

    @Test
    void testNullUserId() {
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(null); // Null user ID
        CreateOrderRequestDTO.Item item = new CreateOrderRequestDTO.Item();
        item.setProductId(1L);
        item.setQuantity(1);
        request.setItems(Arrays.asList(item));

        assertThrows(NullPointerException.class, () -> {
            orderService.placeOrder(request);
        });
    }

    @Test
    void testNullItems() {
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(1L);
        request.setItems(null); // Null items list

        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(NullPointerException.class, () -> {
            orderService.placeOrder(request);
        });
    }

    @Test
    void testNullProductId() {
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(1L);
        CreateOrderRequestDTO.Item item = new CreateOrderRequestDTO.Item();
        item.setProductId(null); // Null product ID
        item.setQuantity(1);
        request.setItems(Arrays.asList(item));

        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(NullPointerException.class, () -> {
            orderService.placeOrder(request);
        });
    }

    @Test
    void testNullProductPrice() {
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
        product.setPrice(null); // Null price - this will cause NPE when setting orderItem.setPrice(product.getPrice())

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.saveAll(any())).thenReturn(Arrays.asList(product));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThrows(NullPointerException.class, () -> {
            orderService.placeOrder(request);
        });
    }
}
