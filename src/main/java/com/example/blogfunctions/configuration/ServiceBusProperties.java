package com.example.blogfunctions.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.service-bus")
public record ServiceBusProperties(
        String connection,
        @NotBlank String topicName) {
    public ServiceBusProperties {
        if (topicName == null || topicName.isBlank()) {
            topicName = "generic_events";
        }
    }

    // Do not allow record-generated toString() to disclose the connection string.
    @Override
    public String toString() {
        return "ServiceBusProperties[topicName=" + topicName + ", connection=<redacted>]";
    }
}
