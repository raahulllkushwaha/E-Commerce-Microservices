package com.rahul.productservice.category;

import com.rahul.productservice.category.dto.CategoryRequest;
import com.rahul.productservice.category.dto.CategoryResponse;
import com.rahul.productservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse categoryResponse = categoryService.createCategory(request);

        ApiResponse<CategoryResponse> response = ApiResponse.<CategoryResponse>builder()
                .success(true)
                .message("Category added successfully!")
                .data(categoryResponse)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {

        List<CategoryResponse> categoryResponse = categoryService.getAllCategories();

        ApiResponse<List<CategoryResponse>> response = ApiResponse.<List<CategoryResponse>>builder()
                .success(true)
                .message("Category fetched successfully!")
                .data(categoryResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {

        CategoryResponse categoryResponse = categoryService.getCategoryById(id);

        ApiResponse<CategoryResponse> response = ApiResponse.<CategoryResponse>builder()
                .success(true)
                .message("Category by id!")
                .data(categoryResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {

        CategoryResponse categoryResponse = categoryService.updateCategory(id, request);

        ApiResponse<CategoryResponse> response = ApiResponse.<CategoryResponse>builder()
                .success(true)
                .message("Category updated successfully.")
                .data(categoryResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {

        categoryService.deleteCategory(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Category deleted")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }
}