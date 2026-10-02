package com.example.blogfunctions.configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AzureConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AzureConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public TokenCredential tokenCredential(
            @Value("${app.azure.client-id:${AZURE_CLIENT_ID:${app.service-bus.client-id:}}}") String clientId) {
        log.info("Initializing TokenCredential bean via Azure Identity DefaultAzureCredential (clientId={})",
                (clientId != null && !clientId.isBlank()) ? clientId : "default");
        DefaultAzureCredentialBuilder builder = new DefaultAzureCredentialBuilder();
        if (clientId != null && !clientId.isBlank()) {
            builder.managedIdentityClientId(clientId);
        }
        return builder.build();
    }
}
