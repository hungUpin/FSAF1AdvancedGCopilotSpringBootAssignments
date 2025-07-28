package com.example.copilot.controller;

import com.example.copilot.dto.CreateOrderRequestDTO;
import com.example.copilot.dto.OrderDTO;
import com.example.copilot.entity.Order;
import com.example.copilot.service.OrderService;
import com.example.copilot.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<OrderDTO> placeOrder(@Valid @RequestBody CreateOrderRequestDTO request) {
        OrderDTO createdOrder = orderService.placeOrder(request);
        // Assuming createdOrder.getId() is not null and can be used for location URI
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUser(@PathVariable Long userId) {
        List<OrderDTO> orders = orderRepository.findByUserId(userId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(orders);
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus().name());
        dto.setUserId(order.getUser().getId());
        // You can add mapping for items if needed
        return dto;
    }

    @RestControllerAdvice
    class OrderControllerExceptionHandler {
        @ExceptionHandler(com.example.copilot.exception.ResourceNotFoundException.class)
        public ResponseEntity<Object> handleResourceNotFound(com.example.copilot.exception.ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 404,
                    "error", "Resource Not Found",
                    "message", ex.getMessage(),
                    "details", "uri=/api/v1/orders"
                )
            );
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<Object> handleIllegalState(IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 400,
                    "error", "Bad Request",
                    "message", ex.getMessage(),
                    "details", ex.getMessage()
                )
            );
        }
        
        @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
        public ResponseEntity<Object> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
            String errorDetails = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 400,
                    "error", "Validation Error", 
                    "message", "Input validation failed",
                    "details", errorDetails,
                    "id", (String)null,
                    "email", (String)null
                )
            );
        }
    }
}
