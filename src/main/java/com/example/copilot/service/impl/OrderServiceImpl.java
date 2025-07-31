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
        // Build complete order using builder pattern
        Order completedOrder = new OrderBuilder(request)
            .withValidatedUser()
            .withOrderItems()
            .buildAndPersist();
        
        // Convert to DTO for response
        return mapToOrderDTO(completedOrder, completedOrder.getUser());
    }

    /**
     * Builder pattern for order creation.
     * Ensures proper sequencing and encapsulates order assembly logic.
     */
    private class OrderBuilder {
        private final CreateOrderRequestDTO request;
        private User user;
        private Order order;
        private List<OrderItem> orderItems;

        public OrderBuilder(CreateOrderRequestDTO request) {
            this.request = request;
        }

        public OrderBuilder withValidatedUser() {
            this.user = validateAndGetUser(request.getUserId());
            this.order = createInitialOrder(user);
            return this;
        }

        public OrderBuilder withOrderItems() {
            this.orderItems = processOrderItems(request.getItems(), order);
            return this;
        }

        public Order buildAndPersist() {
            if (user == null || order == null || orderItems == null) {
                throw new IllegalStateException("Order builder not properly initialized");
            }
            
            order.setOrderItems(orderItems);
            return OrderServiceImpl.this.persistOrder(order, orderItems);
        }
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
        
        // Process each item request
        for (CreateOrderRequestDTO.Item itemReq : itemRequests) {
            ProcessedOrderItemResult result = processIndividualOrderItem(itemReq, order);
            orderItems.add(result.getOrderItem());
            productsToUpdate.add(result.getUpdatedProduct());
        }
        
        // Batch update all product stock changes
        batchUpdateProductStock(productsToUpdate);
        
        return orderItems;
    }
    
    /**
     * Processes a single order item request: validates product, manages stock, and creates order item.
     * This method encapsulates the logic for handling one item to improve readability and testability.
     * 
     * @param itemRequest the item request to process
     * @param order the order this item belongs to
     * @return ProcessedOrderItemResult containing the created order item and updated product
     */
    private ProcessedOrderItemResult processIndividualOrderItem(CreateOrderRequestDTO.Item itemRequest, Order order) {
        Product product = validateAndGetProduct(itemRequest.getProductId());
        validateStock(product, itemRequest.getQuantity());
        
        // Reduce stock for this item
        reduceProductStock(product, itemRequest.getQuantity());
        
        // Create the order item
        OrderItem orderItem = createOrderItem(order, product, itemRequest.getQuantity());
        
        return new ProcessedOrderItemResult(orderItem, product);
    }
    
    /**
     * Result object for individual order item processing.
     * Encapsulates the order item and updated product for better data flow.
     */
    private static class ProcessedOrderItemResult {
        private final OrderItem orderItem;
        private final Product updatedProduct;
        
        public ProcessedOrderItemResult(OrderItem orderItem, Product updatedProduct) {
            this.orderItem = orderItem;
            this.updatedProduct = updatedProduct;
        }
        
        public OrderItem getOrderItem() { return orderItem; }
        public Product getUpdatedProduct() { return updatedProduct; }
    }
    
    /**
     * Reduces the stock of a product by the specified quantity.
     * This method encapsulates the stock reduction logic for better reusability.
     * 
     * @param product the product whose stock should be reduced
     * @param quantity the quantity to reduce from stock
     */
    private void reduceProductStock(Product product, Integer quantity) {
        Integer currentStock = product.getStock();
        if (currentStock == null) {
            throw new IllegalStateException("Product stock is not set for product: " + product.getName());
        }
        product.setStock(currentStock - quantity);
    }
    
    /**
     * Performs batch update of product stock levels for better database performance.
     * 
     * @param productsToUpdate list of products with updated stock levels
     */
    private void batchUpdateProductStock(List<Product> productsToUpdate) {
        if (!productsToUpdate.isEmpty()) {
            productRepository.saveAll(productsToUpdate);
        }
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
        Order order = validateOrderForCancellation(orderId);
        restoreStockForCancelledOrder(order);
        updateOrderStatusToCancelled(order);
    }
    
    /**
     * Validates that an order exists and can be cancelled.
     * 
     * @param orderId the ID of the order to validate
     * @return the validated order entity
     * @throws ResourceNotFoundException if order not found
     * @throws IllegalStateException if order cannot be cancelled
     */
    private Order validateOrderForCancellation(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }
        
        return order;
    }
    
    /**
     * Restores stock for all items in a cancelled order.
     * 
     * @param order the order whose items need stock restoration
     */
    private void restoreStockForCancelledOrder(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return; // No items to restore stock for
        }
        
        List<Product> productsToUpdate = restoreStockForOrderItems(order.getOrderItems());
        batchUpdateProductStock(productsToUpdate);
    }
    
    /**
     * Restores stock for a collection of order items.
     * This method encapsulates the logic of iterating through items and updating stock.
     * 
     * @param orderItems the order items to restore stock for
     * @return list of products with updated stock levels
     */
    private List<Product> restoreStockForOrderItems(List<OrderItem> orderItems) {
        List<Product> productsToUpdate = new ArrayList<>();
        
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            if (product != null) {
                restoreStockForSingleItem(product, item.getQuantity());
                productsToUpdate.add(product);
            }
        }
        
        return productsToUpdate;
    }
    
    /**
     * Restores stock for a single product by adding back the specified quantity.
     * 
     * @param product the product to restore stock for
     * @param quantity the quantity to add back to stock
     */
    private void restoreStockForSingleItem(Product product, Integer quantity) {
        Integer currentStock = product.getStock();
        if (currentStock == null) {
            throw new IllegalStateException("Product stock is not set for product: " + product.getName());
        }
        product.setStock(currentStock + quantity);
    }
    
    /**
     * Updates the order status to CANCELLED and persists the change.
     * 
     * @param order the order to update
     */
    private void updateOrderStatusToCancelled(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    public boolean hasUserPurchasedProduct(Long userId, Long productId) {
        return orderRepository.existsByUserIdAndOrderItemsProductIdAndStatus(userId, productId, OrderStatus.DELIVERED);
    }
}
