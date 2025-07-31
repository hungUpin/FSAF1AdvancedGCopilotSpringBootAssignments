package com.example.copilot.service.impl;

import com.example.copilot.dto.ProductDTO;
import com.example.copilot.entity.Category;
import com.example.copilot.entity.Product;
import com.example.copilot.exception.ResourceNotFoundException;
import com.example.copilot.repository.CategoryRepository;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @CacheEvict(value = "product-search", allEntries = true)
    public ProductDTO create(ProductDTO productDTO) {
        Product product = convertToEntity(productDTO);
        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    @Override
    @CacheEvict(value = {"product-details", "product-search"}, key = "#id")
    public ProductDTO update(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        updateProductFromDTO(product, productDTO);
        return convertToDTO(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = {"product-details", "product-search"}, key = "#id")
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product-details", key = "#id")
    public ProductDTO findById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> findAll(Pageable pageable) {
        return productRepository.findAll(pageable)
            .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String keyword, Long categoryId, Double minPrice, Double maxPrice, Pageable pageable) {
        return productRepository.searchProducts(keyword, categoryId, minPrice, maxPrice, pageable)
            .map(this::convertToDTO);
    }
    
    /**
     * OPTIMIZED: High-performance search method that leverages database collation indexes
     * instead of function-based searches. This method provides significantly better performance
     * for the critical product search API endpoint.
     * 
     * @param keyword search term (case-insensitive due to collation)
     * @param categoryId optional category filter
     * @param minPrice optional minimum price filter
     * @param maxPrice optional maximum price filter
     * @param pageable pagination parameters
     * @return page of matching products with relevance-based ordering
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "product-search", key = "#keyword + '_' + #categoryId + '_' + #minPrice + '_' + #maxPrice + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductDTO> searchProductsOptimized(String keyword, Long categoryId, Double minPrice, Double maxPrice, Pageable pageable) {
        // Use the optimized repository method that leverages proper indexing
        return productRepository.searchProductsOptimizedCollation(keyword, categoryId, minPrice, maxPrice, pageable)
            .map(this::convertToDTO);
    }
    
    /**
     * OPTIMIZED: Name-only search for maximum performance when description search is not needed.
     * This is the fastest search method and should be used when possible.
     * 
     * @param keyword search term for product names only
     * @param categoryId optional category filter
     * @param minPrice optional minimum price filter
     * @param maxPrice optional maximum price filter
     * @param pageable pagination parameters
     * @return page of matching products ordered by relevance
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "product-search", key = "#keyword + '_' + #categoryId + '_' + #minPrice + '_' + #maxPrice + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductDTO> searchProductsByNameOptimized(String keyword, Long categoryId, Double minPrice, Double maxPrice, Pageable pageable) {
        return productRepository.searchProductsByNameOptimized(keyword, categoryId, minPrice, maxPrice, pageable)
            .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> findByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable)
            .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> findByPriceRange(Double minPrice, Double maxPrice, Pageable pageable) {
        return productRepository.findByPriceRange(minPrice, maxPrice, pageable)
            .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> findLowStockProducts(Integer threshold, Pageable pageable) {
        return productRepository.findLowStockProducts(threshold, pageable)
            .map(this::convertToDTO);
    }

    private Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setId(dto.getId());
        updateProductFromDTO(product, dto);
        return product;
    }

    private void updateProductFromDTO(Product product, ProductDTO dto) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));
            product.setCategory(category);
        }
    }

    private ProductDTO convertToDTO(Product entity) {
        ProductDTO dto = new ProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        dto.setStock(entity.getStock());
        dto.setAverageRating(entity.getAverageRating());
        dto.setReviewCount(entity.getReviewCount());
        
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
        }
        
        return dto;
    }
}
