package com.example.copilot.controller;

import com.example.copilot.dto.CreateReviewRequestDTO;
import com.example.copilot.dto.ReviewDTO;
import com.example.copilot.dto.UpdateReviewRequestDTO;
import com.example.copilot.exception.ErrorDetails;
import com.example.copilot.exception.ValidationException;
import com.example.copilot.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Management", description = "APIs for managing product reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/users/{userId}")
    @Operation(summary = "Create a new review", description = "Create a review for a product (user must have purchased the product)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Review created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or user hasn't purchased the product"),
        @ApiResponse(responseCode = "404", description = "User or product not found"),
        @ApiResponse(responseCode = "409", description = "User has already reviewed this product")
    })
    public ResponseEntity<ReviewDTO> createReview(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody CreateReviewRequestDTO request,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Invalid input data");
        }
        
        ReviewDTO review = reviewService.createReview(userId, request);
        return new ResponseEntity<>(review, HttpStatus.CREATED);
    }

    @PutMapping("/{reviewId}/users/{userId}")
    @Operation(summary = "Update a review", description = "Update an existing review (user can only update their own reviews)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "User can only update their own reviews"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ReviewDTO> updateReview(
            @Parameter(description = "Review ID") @PathVariable Long reviewId,
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody UpdateReviewRequestDTO request,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Invalid input data");
        }
        
        ReviewDTO review = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(review);
    }

    @DeleteMapping("/{reviewId}/users/{userId}")
    @Operation(summary = "Delete a review", description = "Delete a review (user can only delete their own reviews)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Review deleted successfully"),
        @ApiResponse(responseCode = "403", description = "User can only delete their own reviews"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Review ID") @PathVariable Long reviewId,
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review by ID", description = "Retrieve a specific review by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review found"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ReviewDTO> getReview(
            @Parameter(description = "Review ID") @PathVariable Long reviewId) {
        
        ReviewDTO review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get reviews for a product", description = "Retrieve all reviews for a specific product with pagination")
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

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get reviews by user", description = "Retrieve all reviews written by a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<ReviewDTO>> getUserReviews(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        List<ReviewDTO> reviews = reviewService.getReviewsByUserId(userId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/users/{userId}/products/{productId}/can-review")
    @Operation(summary = "Check if user can review product", description = "Check if a user is eligible to review a specific product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Eligibility checked successfully")
    })
    public ResponseEntity<Map<String, Boolean>> canUserReviewProduct(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        
        boolean canReview = reviewService.canUserReviewProduct(userId, productId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("canReview", canReview);
        return ResponseEntity.ok(response);
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
