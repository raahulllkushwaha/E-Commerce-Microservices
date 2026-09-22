package com.rahul.productservice.config;

import com.rahul.productservice.event.OrderCancelledEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", "kafka:9092");
        props.put("group.id", "product-group");
        props.put("auto.offset.reset", "earliest");
        return props;
    }

    @Bean
    public ConsumerFactory<String, OrderCancelledEvent> orderCancelledConsumerFactory() {

        JacksonJsonDeserializer<OrderCancelledEvent> deserializer =
                new JacksonJsonDeserializer<>(OrderCancelledEvent.class);

        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                baseProps(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCancelledEvent> orderCancelledFactory() {

        var factory =
                new ConcurrentKafkaListenerContainerFactory<String, OrderCancelledEvent>();

        factory.setConsumerFactory(orderCancelledConsumerFactory());

        return factory;
    }
}