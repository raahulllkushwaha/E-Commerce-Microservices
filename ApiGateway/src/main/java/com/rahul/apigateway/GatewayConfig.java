package com.rahul.apigateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> userRoute() {
        return route("user-service")
                .route(path("/api/users/**"), http())
                .before(uri("http://localhost:8081"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> addressRoute() {
        return route("address-service")
                .route(path("/api/addresses/**"), http())
                .before(uri("http://localhost:8081"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> productRoute() {
        return route("product-service")
                .route(path("/api/products/**"), http())
                .before(uri("http://localhost:8082"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> categoryRoute() {
        return route("category-service")
                .route(path("/api/categories/**"), http())
                .before(uri("http://localhost:8082"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> cartRoute() {
        return route("cart-service")
                .route(path("/api/carts", "/api/carts/**"), http())
                .before(uri("http://localhost:8083"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderRoute() {
        return route("order-service")
                .route(path("/api/orders/**"), http())
                .before(uri("http://localhost:8083"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentRoute() {
        return route("payment-service")
                .route(path("/api/payments/**"), http())
                .before(uri("http://localhost:8084"))
                .build();
    }
}