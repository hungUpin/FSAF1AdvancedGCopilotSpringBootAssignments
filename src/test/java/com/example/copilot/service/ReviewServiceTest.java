package com.example.copilot.service;

import com.example.copilot.dto.CreateReviewRequestDTO;
import com.example.copilot.dto.ReviewDTO;
import com.example.copilot.dto.UpdateReviewRequestDTO;
import com.example.copilot.entity.*;
import com.example.copilot.exception.ResourceNotFoundException;
import com.example.copilot.exception.ValidationException;
import com.example.copilot.repository.OrderRepository;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User testUser;
    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        // Create test product
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(100.0);
        testProduct.setStock(10);
        testProduct = productRepository.save(testProduct);

        // Create test order (delivered)
        testOrder = new Order();
        testOrder.setUser(testUser);
        testOrder.setOrderDate(LocalDateTime.now().minusDays(7));
        testOrder.setStatus(OrderStatus.DELIVERED);
        testOrder = orderRepository.save(testOrder);

        // Create order item
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(testOrder);
        orderItem.setProduct(testProduct);
        orderItem.setQuantity(1);
        orderItem.setPrice(100.0);
        testOrder.getOrderItems().add(orderItem);
        orderRepository.save(testOrder);
    }

    @Test
    void addReview_ShouldCreateReview_WhenUserHasPurchasedProduct() {
        // Given
        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setContent("Excellent product, highly recommended!");
        reviewDTO.setRating(5);

        // When
        ReviewDTO result = reviewService.addReview(reviewDTO, testUser.getId(), testProduct.getId());

        // Then
        assertNotNull(result);
        assertEquals("Excellent product, highly recommended!", result.getContent());
        assertEquals(5, result.getRating());
        assertEquals(testProduct.getId(), result.getProductId());
        assertEquals(testUser.getId(), result.getUserId());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void createReview_ShouldCreateReview_WhenUserHasPurchasedProduct() {
        // Given
        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setContent("Great product, really satisfied with the quality!");
        request.setRating(5);
        request.setProductId(testProduct.getId());

        // When
        ReviewDTO result = reviewService.createReview(testUser.getId(), request);

        // Then
        assertNotNull(result);
        assertEquals("Great product, really satisfied with the quality!", result.getContent());
        assertEquals(5, result.getRating());
        assertEquals(testProduct.getId(), result.getProductId());
        assertEquals(testUser.getId(), result.getUserId());
    }

    @Test
    void createReview_ShouldThrowException_WhenUserHasNotPurchasedProduct() {
        // Given
        User otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setRole("USER");
        final User savedOtherUser = userRepository.save(otherUser);

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setContent("Great product!");
        request.setRating(5);
        request.setProductId(testProduct.getId());

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> reviewService.createReview(savedOtherUser.getId(), request));
        assertEquals("You can only review products you have purchased and received", exception.getMessage());
    }

    @Test
    void createReview_ShouldThrowException_WhenUserAlreadyReviewedProduct() {
        // Given
        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setContent("Great product!");
        request.setRating(5);
        request.setProductId(testProduct.getId());

        // Create first review
        reviewService.createReview(testUser.getId(), request);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> reviewService.createReview(testUser.getId(), request));
        assertEquals("You have already reviewed this product", exception.getMessage());
    }

    @Test
    void updateReview_ShouldUpdateReview_WhenUserOwnsReview() {
        // Given
        CreateReviewRequestDTO createRequest = new CreateReviewRequestDTO();
        createRequest.setContent("Original review");
        createRequest.setRating(3);
        createRequest.setProductId(testProduct.getId());

        ReviewDTO createdReview = reviewService.createReview(testUser.getId(), createRequest);

        UpdateReviewRequestDTO updateRequest = new UpdateReviewRequestDTO();
        updateRequest.setContent("Updated review content");
        updateRequest.setRating(5);

        // When
        ReviewDTO result = reviewService.updateReview(testUser.getId(), createdReview.getId(), updateRequest);

        // Then
        assertEquals("Updated review content", result.getContent());
        assertEquals(5, result.getRating());
    }

    @Test
    void deleteReview_ShouldDeleteReview_WhenUserOwnsReview() {
        // Given
        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setContent("Review to be deleted");
        request.setRating(4);
        request.setProductId(testProduct.getId());

        ReviewDTO createdReview = reviewService.createReview(testUser.getId(), request);

        // When
        reviewService.deleteReview(testUser.getId(), createdReview.getId());

        // Then
        assertThrows(ResourceNotFoundException.class, 
            () -> reviewService.getReviewById(createdReview.getId()));
    }

    @Test
    void getReviewsByProductId_ShouldReturnReviews_WhenProductHasReviews() {
        // Given
        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setContent("Great product!");
        request.setRating(5);
        request.setProductId(testProduct.getId());

        reviewService.createReview(testUser.getId(), request);

        // When
        List<ReviewDTO> result = reviewService.getReviewsByProductId(testProduct.getId());

        // Then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Great product!", result.get(0).getContent());
    }

    @Test
    void canUserReviewProduct_ShouldReturnTrue_WhenUserCanReview() {
        // When
        boolean result = reviewService.canUserReviewProduct(testUser.getId(), testProduct.getId());

        // Then
        assertTrue(result);
    }

    @Test
    void canUserReviewProduct_ShouldReturnFalse_WhenUserAlreadyReviewed() {
        // Given
        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setContent("Already reviewed");
        request.setRating(4);
        request.setProductId(testProduct.getId());

        reviewService.createReview(testUser.getId(), request);

        // When
        boolean result = reviewService.canUserReviewProduct(testUser.getId(), testProduct.getId());

        // Then
        assertFalse(result);
    }
}
