package com.example.copilot.service;

import com.example.copilot.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductDTO create(ProductDTO productDTO);
    ProductDTO update(Long id, ProductDTO productDTO);
    void delete(Long id);
    ProductDTO findById(Long id);
    Page<ProductDTO> findAll(Pageable pageable);
    Page<ProductDTO> searchProducts(String keyword, Long categoryId, Double minPrice, Double maxPrice, Pageable pageable);
    Page<ProductDTO> findByCategoryId(Long categoryId, Pageable pageable);
    Page<ProductDTO> findByPriceRange(Double minPrice, Double maxPrice, Pageable pageable);
    Page<ProductDTO> findLowStockProducts(Integer threshold, Pageable pageable);
}
