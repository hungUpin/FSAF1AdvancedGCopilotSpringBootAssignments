package com.example.copilot.service.impl;

import com.example.copilot.dto.CreateReviewRequestDTO;
import com.example.copilot.dto.ReviewDTO;
import com.example.copilot.dto.UpdateReviewRequestDTO;
import com.example.copilot.entity.Product;
import com.example.copilot.entity.Review;
import com.example.copilot.entity.User;
import com.example.copilot.exception.DuplicateReviewException;
import com.example.copilot.exception.ResourceNotFoundException;
import com.example.copilot.exception.UserNotPurchasedProductException;
import com.example.copilot.exception.ValidationException;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.repository.ReviewRepository;
import com.example.copilot.repository.UserRepository;
import com.example.copilot.service.OrderService;
import com.example.copilot.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    
    
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    @Override
    @Transactional
    public ReviewDTO addReview(ReviewDTO dto, Long userId, Long productId) {
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Validate product exists
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        // 1. First, call the orderRepository to verify that the user (userId) has at least one completed ('DELIVERED') order containing the product (productId)
        // If not, throw a new 'UserNotPurchasedProductException'
        if (!orderService.hasUserPurchasedProduct(userId, productId)) {
            throw new UserNotPurchasedProductException("User has not purchased and received this product");
        }
        
        // 2. Then, call the reviewRepository to check if a review from this user for this product already exists
        // If yes, throw a 'DuplicateReviewException'
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateReviewException("User has already reviewed this product");
        }
        
        // 3. If all checks pass, map the DTO to a new Review entity and save it
        Review review = new Review();
        review.setContent(dto.getContent());
        review.setRating(dto.getRating());
        review.setUser(user);
        review.setProduct(product);
        
        Review savedReview = reviewRepository.save(review);
        
        // Now, call a new private helper method named 'updateProductAverageRating'
        updateProductAverageRating(productId);
        
        return mapToDTO(savedReview);
    }

    @Override
    @Transactional
    public ReviewDTO createReview(Long userId, CreateReviewRequestDTO request) {
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        
        // Check if user has purchased the product
        if (!orderService.hasUserPurchasedProduct(userId, request.getProductId())) {
            throw new ValidationException("You can only review products you have purchased and received");
        }
        
        // Check if user has already reviewed this product
        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new ValidationException("You have already reviewed this product");
        }
        
        // Create review
        Review review = new Review();
        review.setContent(request.getContent());
        review.setRating(request.getRating());
        review.setUser(user);
        review.setProduct(product);
        
        Review savedReview = reviewRepository.save(review);
        
        // Update product rating statistics
        recalculateProductAverageRating(request.getProductId());
        
        return mapToDTO(savedReview);
    }

    @Override
    @Transactional
    public ReviewDTO updateReview(Long userId, Long reviewId, UpdateReviewRequestDTO request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        // Check if the review belongs to the user
        if (!review.getUser().getId().equals(userId)) {
            throw new ValidationException("You can only update your own reviews");
        }
        
        review.setContent(request.getContent());
        review.setRating(request.getRating());
        
        Review updatedReview = reviewRepository.save(review);
        
        // Update product rating statistics
        recalculateProductAverageRating(review.getProduct().getId());
        
        return mapToDTO(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        // Check if the review belongs to the user
        if (!review.getUser().getId().equals(userId)) {
            throw new ValidationException("You can only delete your own reviews");
        }
        
        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        
        // Update product rating statistics
        recalculateProductAverageRating(productId);
    }

    @Override
    public ReviewDTO getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        return mapToDTO(review);
    }

    @Override
    public List<ReviewDTO> getReviewsByProductId(Long productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);
        return reviews.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<ReviewDTO> getReviewsByUserId(Long userId) {
        List<Review> reviews = reviewRepository.findByUserId(userId);
        return reviews.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Page<ReviewDTO> getReviewsByProductId(Long productId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findAll(
            (root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("product").get("id"), productId),
            pageable
        );
        return reviews.map(this::mapToDTO);
    }

    @Override
    public boolean canUserReviewProduct(Long userId, Long productId) {
        // User can review if they have purchased the product and haven't reviewed it yet
        return orderService.hasUserPurchasedProduct(userId, productId) 
            && !reviewRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    private void updateProductRatingStatistics(Long productId) {
        recalculateProductAverageRating(productId);
    }

    /**
     * Helper method that accepts a productId and updates the Product's average rating
     * Uses JPQL query in ReviewRepository to calculate the average rating directly in the database for efficiency
     */
    @Transactional
    private void updateProductAverageRating(Long productId) {
        // Use JPQL query to calculate the average rating directly in the database for efficiency
        Double averageRating = reviewRepository.findAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.countByProductId(productId);
        
        // Update the Product with the calculated average rating
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        product.setAverageRating(averageRating != null ? averageRating : 0.0);
        product.setReviewCount(reviewCount.intValue());
        
        productRepository.save(product);
    }

    @Transactional
    private void recalculateProductAverageRating(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        Double averageRating = reviewRepository.findAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.countByProductId(productId);
        
        product.setAverageRating(averageRating != null ? averageRating : 0.0);
        product.setReviewCount(reviewCount.intValue());
        
        productRepository.save(product);
    }

    private ReviewDTO mapToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setContent(review.getContent());
        dto.setRating(review.getRating());
        dto.setProductId(review.getProduct().getId());
        dto.setProductName(review.getProduct().getName());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getName());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
}
