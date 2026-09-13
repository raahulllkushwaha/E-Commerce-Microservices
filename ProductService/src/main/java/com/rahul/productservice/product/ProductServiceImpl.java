package com.rahul.productservice.product;

import com.rahul.productservice.category.Category;
import com.rahul.productservice.category.CategoryRepository;
import com.rahul.productservice.common.exception.DuplicateResourceException;
import com.rahul.productservice.common.exception.InvalidCredentialsException;
import com.rahul.productservice.common.exception.ResourceNotFoundException;
import com.rahul.productservice.product.dto.ProductRequest;
import com.rahul.productservice.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public ProductResponse createProduct(String sellerEmail, ProductRequest request){

        if(productRepository.existsBySku(request.getSku())){
            throw new DuplicateResourceException("Product already exist");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .sellerEmail(sellerEmail)
                .build();

        Product savedProduct = productRepository.save(product);

        return ProductResponse.builder()
                .id(savedProduct.getId())
                .name(savedProduct.getName())
                .description(savedProduct.getDescription())
                .sku(savedProduct.getSku())
                .price(savedProduct.getPrice())
                .stockQuantity(savedProduct.getStockQuantity())
                .categoryName(savedProduct.getCategory().getName())
                .isActive(savedProduct.isActive())
                .createdAt(savedProduct.getCreatedAt())
                .build();
    }

    @Override
    public List<ProductResponse> getAllProducts(){
       List<Product> products = productRepository.findAll();

       return products.stream()
               .map(product -> ProductResponse.builder()
                       .id(product.getId())
                       .name(product.getName())
                       .description(product.getDescription())
                       .sku(product.getSku())
                       .price(product.getPrice())
                       .stockQuantity(product.getStockQuantity())
                       .categoryName(product.getCategory().getName())
                       .isActive(product.isActive())
                       .createdAt(product.getCreatedAt())
                       .build())
               .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByCategory(UUID categoryId){
        List<Product> products = productRepository.findByCategoryId(categoryId);

        return products.stream()
                .map(product -> ProductResponse.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .sku(product.getSku())
                        .price(product.getPrice())
                        .stockQuantity(product.getStockQuantity())
                        .categoryName(product.getCategory().getName())
                        .isActive(product.isActive())
                        .createdAt(product.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public  ProductResponse getProductById(UUID id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));


        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .categoryName(product.getCategory().getName())
                .isActive(product.isActive())
                .createdAt(product.getCreatedAt())
                .build();
    }

    @Override
    public ProductResponse updateProduct(UUID id, String sellerEmail, String role, ProductRequest request){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!role.equals("ADMIN") && !product.getSellerEmail().equals(sellerEmail)) {
            throw new InvalidCredentialsException("You are not allowed to update this product");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);

        return ProductResponse.builder()
                .id(updatedProduct.getId())
                .name(updatedProduct.getName())
                .description(updatedProduct.getDescription())
                .sku(updatedProduct.getSku())
                .price(updatedProduct.getPrice())
                .stockQuantity(updatedProduct.getStockQuantity())
                .categoryName(updatedProduct.getCategory().getName())
                .isActive(updatedProduct.isActive())
                .createdAt(updatedProduct.getCreatedAt())
                .build();
    }

    @Override
    public void deleteProduct(UUID id, String sellerEmail, String role){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!role.equals("ADMIN") && !product.getSellerEmail().equals(sellerEmail)) {
            throw new InvalidCredentialsException("You are not allowed to delete this product");
        }

        product.setActive(false);
        productRepository.delete(product);
    }
}

