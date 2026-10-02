package com.example.blogfunctions.configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceBusConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ServiceBusConfiguration.class);

    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("!'${app.service-bus.namespace:}'.empty || !'${app.service-bus.connection:}'.empty")
    public ServiceBusSenderClient serviceBusSenderClient(ServiceBusProperties properties,
            ObjectProvider<TokenCredential> credentialProvider) {
        if (!properties.isConfigured()) {
            return null;
        }

        ServiceBusClientBuilder builder = new ServiceBusClientBuilder();

        if (properties.namespace() != null && !properties.namespace().isBlank()) {
            String fqns = properties.fullyQualifiedNamespace();
            log.info("Initializing ServiceBusSenderClient via Azure Identity for namespace={} topic={}",
                    fqns, properties.topicName());

            TokenCredential credential = credentialProvider.getIfAvailable(() -> {
                DefaultAzureCredentialBuilder credentialBuilder = new DefaultAzureCredentialBuilder();
                if (properties.clientId() != null && !properties.clientId().isBlank()) {
                    credentialBuilder.managedIdentityClientId(properties.clientId());
                }
                return credentialBuilder.build();
            });

            return builder
                    .fullyQualifiedNamespace(fqns)
                    .credential(credential)
                    .sender()
                    .topicName(properties.topicName())
                    .buildClient();
        }

        log.info("Initializing ServiceBusSenderClient via connection string for topic={}", properties.topicName());
        return builder
                .connectionString(properties.connection())
                .sender()
                .topicName(properties.topicName())
                .buildClient();
    }
}
