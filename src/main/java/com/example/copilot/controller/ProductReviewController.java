package com.example.copilot.controller;

import com.example.copilot.dto.ReviewDTO;
import com.example.copilot.exception.ErrorDetails;
import com.example.copilot.exception.ValidationException;
import com.example.copilot.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Product Review Management", description = "APIs for managing product reviews as per your plan")
public class ProductReviewController {

    private final ReviewService reviewService;

    @PostMapping("/products/{productId}/reviews")
    @Operation(
        summary = "Add a new review for a product", 
        description = "Create a review for a product (secured endpoint - user must have purchased the product)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Review created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or user hasn't purchased the product"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "409", description = "User has already reviewed this product")
    })
    public ResponseEntity<ReviewDTO> addReview(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "User ID (for demo - in real app this comes from auth)") @RequestParam Long userId,
            @Valid @RequestBody ReviewDTO reviewDTO,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Invalid input data");
        }
        
        // Call the addReview method as per your plan
        ReviewDTO review = reviewService.addReview(reviewDTO, userId, productId);
        
        return new ResponseEntity<>(review, HttpStatus.CREATED);
    }

    @GetMapping("/products/{productId}/reviews")
    @Operation(
        summary = "Get all reviews for a product", 
        description = "Retrieve all reviews for a specific product (public endpoint)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Page<ReviewDTO>> getProductReviews(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReviewDTO> reviews = reviewService.getReviewsByProductId(productId, pageable);
        
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/products/{productId}/reviews/simple")
    @Operation(
        summary = "Get all reviews for a product (simple list)", 
        description = "Retrieve all reviews for a specific product as a simple list (public endpoint)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<List<ReviewDTO>> getProductReviewsList(
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        
        List<ReviewDTO> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(reviews);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorDetails> handleValidationException(ValidationException ex) {
        ErrorDetails errorDetails = new ErrorDetails(
            java.time.LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            ex.getMessage(),
            "Review validation failed"
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }
}
