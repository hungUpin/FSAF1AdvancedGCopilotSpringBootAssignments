package com.example.copilot.service;

import com.example.copilot.dto.CreateReviewRequestDTO;
import com.example.copilot.dto.ReviewDTO;
import com.example.copilot.dto.UpdateReviewRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {
    // Main method as per your plan
    ReviewDTO addReview(ReviewDTO dto, Long userId, Long productId);
    
    // Additional methods for comprehensive functionality
    ReviewDTO createReview(Long userId, CreateReviewRequestDTO request);
    ReviewDTO updateReview(Long userId, Long reviewId, UpdateReviewRequestDTO request);
    void deleteReview(Long userId, Long reviewId);
    ReviewDTO getReviewById(Long reviewId);
    List<ReviewDTO> getReviewsByProductId(Long productId);
    List<ReviewDTO> getReviewsByUserId(Long userId);
    Page<ReviewDTO> getReviewsByProductId(Long productId, Pageable pageable);
    boolean canUserReviewProduct(Long userId, Long productId);
}
