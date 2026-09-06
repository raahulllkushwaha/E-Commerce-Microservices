package com.rahul.productservice.category;

import com.rahul.productservice.category.dto.CategoryRequest;
import com.rahul.productservice.category.dto.CategoryResponse;
import com.rahul.productservice.common.exception.DuplicateResourceException;
import com.rahul.productservice.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService{

    private final CategoryRepository categoryRepository;


    @Override
    public CategoryResponse createCategory(CategoryRequest request){
        if(categoryRepository.existsByName(request.getName())){
            throw new DuplicateResourceException("Category already exist");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(category);

        return CategoryResponse.builder()
                .id(savedCategory.getId())
                .name(savedCategory.getName())
                .description(savedCategory.getDescription())
                .createdAt(savedCategory.getCreatedAt())
                .build();
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(cat -> CategoryResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .description(cat.getDescription())
                        .createdAt(cat.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(UUID id){
        Category category = categoryRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }

    @Override
    public CategoryResponse updateCategory(UUID id, CategoryRequest request){
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

       category.setName(request.getName());
       category.setDescription(request.getDescription());

       Category updatedCategory = categoryRepository.save(category);

       return CategoryResponse.builder()
               .id(updatedCategory.getId())
               .name(updatedCategory.getName())
               .description(updatedCategory.getDescription())
               .createdAt(updatedCategory.getCreatedAt())
               .build();
    }

    @Override
    public void deleteCategory(UUID id){

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        categoryRepository.delete(category);
    }
}
