package com.example.copilot.repository;

import com.example.copilot.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :min AND :max")
    Page<Product> findByPriceRange(@Param("min") Double min, @Param("max") Double max, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.stock < :threshold")
    Page<Product> findLowStockProducts(@Param("threshold") Integer threshold, Pageable pageable);

    // Native SQL query to count products by category ID
    @Query(value = "SELECT COUNT(*) FROM products WHERE category_id = :categoryId", nativeQuery = true)
    long countByCategoryId(@Param("categoryId") Long categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

    // Optimized query that leverages database indexes more effectively
    // Separates name and description searches for better index utilization
    @Query("SELECT p FROM Product p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:keyword IS NULL OR (" +
           "    LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "    OR (:searchDescription = true AND LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))" +
           "))")
    Page<Product> searchProductsOptimized(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        @Param("searchDescription") boolean searchDescription,
        Pageable pageable
    );
    
    // OPTIMIZED: Leverages proper collation and index-friendly queries
    // Removes LOWER() function calls to allow index usage with proper collation
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "ORDER BY " +
           "CASE WHEN p.name LIKE CONCAT(:keyword, '%') THEN 1 " +
           "     WHEN p.name LIKE CONCAT('%', :keyword, '%') THEN 2 " +
           "     ELSE 3 END, p.name")
    Page<Product> searchProductsOptimizedCollation(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        Pageable pageable
    );
    
    // OPTIMIZED: Name-only search for maximum performance when description search not needed
    @Query("SELECT p FROM Product p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%')) " +
           "ORDER BY " +
           "CASE WHEN p.name LIKE CONCAT(:keyword, '%') THEN 1 " +
           "     WHEN p.name LIKE CONCAT('%', :keyword, '%') THEN 2 " +
           "     ELSE 3 END, p.name")
    Page<Product> searchProductsByNameOptimized(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        Pageable pageable
    );
}
