package com.rahul.productservice.product;

import com.rahul.productservice.product.dto.ProductRequest;
import com.rahul.productservice.product.dto.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(UUID id);
    List<ProductResponse> getProductsByCategory(UUID categoryId);
    ProductResponse updateProduct(UUID id, ProductRequest request);
    void deleteProduct(UUID id);
}
