package com.example.copilot.repository;

import com.example.copilot.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    List<Review> findByProductId(Long productId);
    List<Review> findByUserId(Long userId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Long countByProductId(@Param("productId") Long productId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);
    
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId AND r.product.id = :productId")
    Optional<Review> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Create a single, efficient native SQL query named 'getDashboardStats' to fetch multiple statistics in one database call
     * Returns:
     * 1. 'totalRevenue' as the sum of price*quantity for all 'DELIVERED' orders
     * 2. 'totalOrders' as the total count of all orders
     * 3. 'newCustomersThisMonth' as the count of users created in the current calendar month
     */
    @Query(value = """
        SELECT 
            COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.price * oi.quantity ELSE 0 END), 0) as totalRevenue,
            COUNT(DISTINCT o.id) as totalOrders,
            (SELECT COUNT(u.id) 
             FROM users u 
             WHERE YEAR(u.created_at) = YEAR(CURDATE()) 
             AND MONTH(u.created_at) = MONTH(CURDATE())) as newCustomersThisMonth
        FROM orders o
        LEFT JOIN order_items oi ON o.id = oi.order_id
        """, nativeQuery = true)
    Object[] getDashboardStats();
}
