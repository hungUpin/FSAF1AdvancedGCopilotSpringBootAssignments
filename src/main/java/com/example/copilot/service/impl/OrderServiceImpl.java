package com.example.copilot.service.impl;

import com.example.copilot.dto.CreateOrderRequestDTO;
import com.example.copilot.dto.OrderDTO;
import com.example.copilot.dto.OrderItemDTO;
import com.example.copilot.entity.*;
import com.example.copilot.exception.InsufficientStockException;
import com.example.copilot.exception.ResourceNotFoundException;
import com.example.copilot.repository.OrderItemRepository;
import com.example.copilot.repository.OrderRepository;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.repository.UserRepository;
import com.example.copilot.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderDTO placeOrder(CreateOrderRequestDTO request) {
        // 1. Validate user exists
        User user = validateAndGetUser(request.getUserId());
        
        // 2. Create initial order
        Order order = createInitialOrder(user);
        
        // 3. Process order items (validate products, manage stock, create order items)
        List<OrderItem> orderItems = processOrderItems(request.getItems(), order);
        
        // 4. Complete order setup and persist
        order.setOrderItems(orderItems);
        Order savedOrder = persistOrder(order, orderItems);
        
        // 5. Convert to DTO for response
        return mapToOrderDTO(savedOrder, user);
    }

    /**
     * Validates that the user exists and returns the user entity.
     */
    private User validateAndGetUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    /**
     * Creates a new order with initial status and timestamp.
     */
    private Order createInitialOrder(User user) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    /**
     * Processes all order items: validates products, manages stock, and creates order items.
     */
    private List<OrderItem> processOrderItems(List<CreateOrderRequestDTO.Item> itemRequests, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();
        List<Product> productsToUpdate = new ArrayList<>();
        
        // First pass: validate all products and calculate stock changes
        for (CreateOrderRequestDTO.Item itemReq : itemRequests) {
            Product product = validateAndGetProduct(itemReq.getProductId());
            validateStock(product, itemReq.getQuantity());
            
            // Prepare stock update (but don't persist yet)
            product.setStock(product.getStock() - itemReq.getQuantity());
            productsToUpdate.add(product);
            
            OrderItem orderItem = createOrderItem(order, product, itemReq.getQuantity());
            orderItems.add(orderItem);
        }
        
        // Second pass: persist all stock updates together
        productRepository.saveAll(productsToUpdate);
        
        return orderItems;
    }

    /**
     * Validates stock availability without updating the database.
     */
    private void validateStock(Product product, Integer requestedQuantity) {
        Integer currentStock = product.getStock();
        if (currentStock == null) {
            throw new IllegalStateException("Product stock is not set for product: " + product.getName());
        }
        if (currentStock < requestedQuantity) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
        }
    }

    /**
     * Validates that the product exists and returns the product entity.
     */
    private Product validateAndGetProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    /**
     * Creates an order item with the specified product and quantity.
     */
    private OrderItem createOrderItem(Order order, Product product, Integer quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        
        Double price = product.getPrice();
        if (price == null) {
            throw new IllegalStateException("Product price is not set for product: " + product.getName());
        }
        orderItem.setPrice(price);
        return orderItem;
    }

    /**
     * Persists the order and its items to the database.
     */
    private Order persistOrder(Order order, List<OrderItem> orderItems) {
        Order savedOrder = orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        return savedOrder;
    }

    /**
     * Maps the saved order entity to a DTO for the response.
     */
    private OrderDTO mapToOrderDTO(Order savedOrder, User user) {
        List<OrderItemDTO> itemDTOs = savedOrder.getOrderItems() != null ? 
            savedOrder.getOrderItems().stream()
                .map(this::mapToOrderItemDTO)
                .toList() : 
            new ArrayList<>();

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(savedOrder.getId());
        orderDTO.setOrderDate(savedOrder.getOrderDate());
        orderDTO.setStatus(savedOrder.getStatus() != null ? savedOrder.getStatus().name() : "UNKNOWN");
        orderDTO.setUserId(user.getId());
        orderDTO.setItems(itemDTOs);
        return orderDTO;
    }

    /**
     * Maps an order item entity to a DTO.
     */
    private OrderItemDTO mapToOrderItemDTO(OrderItem orderItem) {
        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setProductId(orderItem.getProduct() != null ? orderItem.getProduct().getId() : null);
        itemDTO.setQuantity(orderItem.getQuantity());
        itemDTO.setPrice(orderItem.getPrice());
        return itemDTO;
    }

    @Override
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }
        // Restore stock for each item
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    public boolean hasUserPurchasedProduct(Long userId, Long productId) {
        return orderRepository.existsByUserIdAndOrderItemsProductIdAndStatus(userId, productId, OrderStatus.DELIVERED);
    }
}
