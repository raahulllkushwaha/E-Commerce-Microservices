package com.rahul.productservice.product;

import com.rahul.productservice.category.Category;
import com.rahul.productservice.category.CategoryRepository;
import com.rahul.productservice.common.exception.DuplicateResourceException;
import com.rahul.productservice.common.exception.InsufficientStockException;
import com.rahul.productservice.common.exception.InvalidCredentialsException;
import com.rahul.productservice.common.exception.ResourceNotFoundException;
import com.rahul.productservice.product.dto.ProductRequest;
import com.rahul.productservice.product.dto.ProductResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;


    // =========================================================
    // CREATE PRODUCT
    // =========================================================

    @Test
    @DisplayName("Should create product successfully when SKU is unique and category exists")
    void shouldCreateProductSuccessfully() {

        UUID categoryId = UUID.randomUUID();

        ProductRequest request = ProductRequest.builder()
                .name("iPhone 15")
                .description("Apple smartphone")
                .sku("IPHONE-15")
                .price(new BigDecimal("70000"))
                .stockQuantity(10)
                .categoryId(categoryId)
                .build();

        Category category = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .build();

        when(productRepository.existsBySku("IPHONE-15"))
                .thenReturn(false);

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        Product savedProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("iPhone 15")
                .description("Apple smartphone")
                .sku("IPHONE-15")
                .price(new BigDecimal("70000"))
                .stockQuantity(10)
                .category(category)
                .sellerEmail("seller@gmail.com")
                .build();

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response =
                productService.createProduct(
                        "seller@gmail.com",
                        request
                );

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("iPhone 15");
        assertThat(response.getStockQuantity()).isEqualTo(10);

        verify(productRepository).save(any(Product.class));
    }


    @Test
    @DisplayName("Should throw DuplicateResourceException when SKU already exists")
    void shouldRejectDuplicateSku() {

        ProductRequest request = ProductRequest.builder()
                .name("iPhone 15")
                .sku("IPHONE-15")
                .price(new BigDecimal("70000"))
                .stockQuantity(10)
                .categoryId(UUID.randomUUID())
                .build();

        when(productRepository.existsBySku("IPHONE-15"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                productService.createProduct(
                        "seller@gmail.com",
                        request
                )
        )
                .isInstanceOf(DuplicateResourceException.class);

        verify(productRepository, never())
                .save(any(Product.class));
    }


    @Test
    @DisplayName("Should throw ResourceNotFoundException when category does not exist")
    void shouldThrowExceptionWhenCategoryNotFoundOnCreation() {

        UUID categoryId = UUID.randomUUID();

        ProductRequest request = ProductRequest.builder()
                .name("iPhone 15")
                .sku("IPHONE-15")
                .categoryId(categoryId)
                .build();

        when(productRepository.existsBySku("IPHONE-15"))
                .thenReturn(false);

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.createProduct(
                        "seller@gmail.com",
                        request
                )
        )
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never())
                .save(any(Product.class));
    }


    // =========================================================
    // STOCK
    // =========================================================

    @Test
    @DisplayName("Should throw InsufficientStockException when stock is insufficient")
    void shouldRejectOrderWhenStockIsInsufficient() {

        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .stockQuantity(2)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() ->
                productService.reduceStock(productId, 5)
        )
                .isInstanceOf(InsufficientStockException.class);

        verify(productRepository, never())
                .save(any(Product.class));
    }


    @Test
    @DisplayName("Should reduce stock successfully when stock is sufficient")
    void shouldReduceStockSuccessfully() {

        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .stockQuantity(10)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        productService.reduceStock(productId, 3);

        assertThat(product.getStockQuantity())
                .isEqualTo(7);

        verify(productRepository)
                .save(product);
    }


    @Test
    @DisplayName("Should increase stock successfully")
    void shouldIncreaseStockSuccessfully() {

        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .stockQuantity(5)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        productService.increaseStock(productId, 3);

        assertThat(product.getStockQuantity())
                .isEqualTo(8);

        verify(productRepository)
                .save(product);
    }


    @Test
    @DisplayName("Should throw ResourceNotFoundException when product does not exist")
    void shouldThrowExceptionWhenProductNotFound() {

        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.reduceStock(productId, 1)
        )
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never())
                .save(any(Product.class));
    }


    // =========================================================
    // UPDATE PRODUCT / SELLER AUTHORIZATION
    // =========================================================

    @Test
    @DisplayName("Should reject seller when updating another seller's product")
    void sellerShouldNotUpdateAnotherSellersProduct() {

        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .sellerEmail("owner@gmail.com")
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        ProductRequest request = ProductRequest.builder()
                .name("Updated Product")
                .categoryId(UUID.randomUUID())
                .build();

        assertThatThrownBy(() ->
                productService.updateProduct(
                        productId,
                        "attacker@gmail.com",
                        "SELLER",
                        request
                )
        )
                .isInstanceOf(InvalidCredentialsException.class);

        verify(productRepository, never())
                .save(any(Product.class));
    }


    @Test
    @DisplayName("Should allow seller to update their own product")
    void sellerShouldUpdateOwnProduct() {

        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Category category = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .build();

        Product product = Product.builder()
                .id(productId)
                .name("Old Name")
                .sellerEmail("seller@gmail.com")
                .category(category)
                .build();

        ProductRequest request = ProductRequest.builder()
                .name("New Name")
                .description("Updated")
                .sku("NEW-SKU")
                .price(new BigDecimal("1000"))
                .stockQuantity(20)
                .categoryId(categoryId)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(productRepository.save(product))
                .thenReturn(product);

        ProductResponse response =
                productService.updateProduct(
                        productId,
                        "seller@gmail.com",
                        "SELLER",
                        request
                );

        assertThat(response).isNotNull();
        assertThat(response.getName())
                .isEqualTo("New Name");

        verify(productRepository)
                .save(product);
    }
}