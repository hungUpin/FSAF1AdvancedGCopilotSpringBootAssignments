package com.example.copilot.controller;

import com.example.copilot.dto.ProductDTO;
import com.example.copilot.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDTO> create(@Valid @RequestBody Map<String, Object> productMap) {
        // Extract categoryId from nested category object if present
        Object categoryObj = productMap.get("category");
        if (categoryObj instanceof Map) {
            Object catId = ((Map<?, ?>) categoryObj).get("id");
            if (catId != null) {
                productMap.put("categoryId", catId);
            }
            productMap.remove("category"); // Remove category key to avoid Jackson error
        }
        // Convert map to ProductDTO
        ProductDTO productDTO = new ObjectMapper().convertValue(productMap, ProductDTO.class);
        ProductDTO createdProduct = productService.create(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(productService.update(id, productDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(productService.searchProducts(keyword, categoryId, minPrice, maxPrice, pageable));
    }

    @RestControllerAdvice
    class ProductControllerExceptionHandler {
        @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
        public ResponseEntity<Object> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
            String errorDetails = ex.getConstraintViolations().stream()
                    .map(cv -> cv.getMessage())
                    .collect(java.util.stream.Collectors.joining(", "));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "timestamp", java.time.LocalDateTime.now().toString(),
                            "status", 400,
                            "error", "Validation Error",
                            "message", "Input validation failed",
                            "details", errorDetails
                    )
            );
        }

        @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
        public ResponseEntity<Object> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
            String errorDetails = ex.getBindingResult().getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(java.util.stream.Collectors.joining(", "));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "timestamp", java.time.LocalDateTime.now().toString(),
                            "status", 400,
                            "error", "Validation Error",
                            "message", "Input validation failed",
                            "details", errorDetails
                    )
            );
        }

        @ExceptionHandler(java.lang.IllegalArgumentException.class)
        public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "timestamp", java.time.LocalDateTime.now().toString(),
                            "status", 400,
                            "error", "Bad Request",
                            "message", ex.getMessage(),
                            "details", ex.getMessage()
                    )
            );
        }

        @ExceptionHandler(com.example.copilot.exception.ResourceNotFoundException.class)
        public ResponseEntity<Object> handleResourceNotFound(com.example.copilot.exception.ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of(
                            "timestamp", java.time.LocalDateTime.now().toString(),
                            "status", 404,
                            "error", "Resource Not Found",
                            "message", ex.getMessage(),
                            "details", ex.getMessage()
                    )
            );
        }
    }
}
