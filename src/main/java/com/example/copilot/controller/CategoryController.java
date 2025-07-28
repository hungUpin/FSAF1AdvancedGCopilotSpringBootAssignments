package com.example.copilot.controller;

import com.example.copilot.dto.CategoryDTO;
import com.example.copilot.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDTO> create(@Valid @RequestBody CategoryDTO categoryDTO) {
        return ResponseEntity.ok(categoryService.create(categoryDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO categoryDTO) {
        return ResponseEntity.ok(categoryService.update(id, categoryDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @GetMapping("/roots")
    public ResponseEntity<List<CategoryDTO>> getRootCategories() {
        return ResponseEntity.ok(categoryService.findAllRootCategories());
    }

    @GetMapping("/subcategories/{parentId}")
    public ResponseEntity<List<CategoryDTO>> getSubcategories(@PathVariable Long parentId) {
        return ResponseEntity.ok(categoryService.findSubcategories(parentId));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CategoryDTO>> search(
            @RequestParam(required = false) String keyword,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(categoryService.searchCategories(keyword, pageable));
    }

    @GetMapping("/roots/page")
    public ResponseEntity<Page<CategoryDTO>> getRootCategoriesPage(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(categoryService.findRootCategories(pageable));
    }
}
