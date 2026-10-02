package com.example.blogfunctions.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.service-bus")
public record ServiceBusProperties(
        String namespace,
        String clientId,
        String connection,
        @NotBlank String topicName) {
    public ServiceBusProperties {
        if (topicName == null || topicName.isBlank()) {
            topicName = "generic_events";
        }
    }

    public String fullyQualifiedNamespace() {
        if (namespace == null || namespace.isBlank()) {
            return null;
        }
        return namespace.contains(".") ? namespace : namespace + ".servicebus.windows.net";
    }

    public boolean isConfigured() {
        return (namespace != null && !namespace.isBlank()) || (connection != null && !connection.isBlank());
    }

    // Do not allow record-generated toString() to disclose the connection string.
    @Override
    public String toString() {
        return "ServiceBusProperties[topicName=" + topicName + ", namespace=" + namespace
                + ", clientId=" + clientId + ", connection=" + (connection != null ? "<redacted>" : "null") + "]";
    }
}
