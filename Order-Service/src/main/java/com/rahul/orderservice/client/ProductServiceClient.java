package com.rahul.orderservice.client;

import com.rahul.orderservice.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "product-service", url = "http://product-service:8082")
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ProductApiResponse getProductById(@PathVariable UUID id);

    @PutMapping("/api/products/{id}/reduce-stock")
    void reduceStock(@PathVariable("id") UUID id, @RequestParam("quantity") Integer quantity);

    @PutMapping("/api/products/{id}/increase-stock")
    void increaseStock(@PathVariable("id") UUID id, @RequestParam("quantity") Integer quantity);
}
