package com.example.copilot.service;

import com.example.copilot.dto.CategoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryDTO create(CategoryDTO categoryDTO);
    CategoryDTO update(Long id, CategoryDTO categoryDTO);
    void delete(Long id);
    CategoryDTO findById(Long id);
    List<CategoryDTO> findAllRootCategories();
    List<CategoryDTO> findSubcategories(Long parentId);
    Page<CategoryDTO> searchCategories(String keyword, Pageable pageable);
    Page<CategoryDTO> findRootCategories(Pageable pageable);
}
