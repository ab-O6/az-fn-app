package com.example.blogfunctions.configuration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceBusConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ServiceBusConfiguration.class);

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.service-bus", name = "connection")
    public ServiceBusSenderClient serviceBusSenderClient(ServiceBusProperties properties) {
        if (properties.connection() == null || properties.connection().isBlank()) {
            return null;
        }
        log.info("Initializing ServiceBusSenderClient for topic={}", properties.topicName());
        return new ServiceBusClientBuilder()
                .connectionString(properties.connection())
                .sender()
                .topicName(properties.topicName())
                .buildClient();
    }
}
