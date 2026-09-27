package com.example.blogfunctions.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.webhook.signature")
public record WebhookProperties(
        boolean enabled,
        String secret,
        @NotBlank String header,
        boolean timestampEnabled,
        @NotBlank String timestampHeader,
        @Min(1) long maxAgeSeconds) {
    // Do not allow record-generated toString() to disclose the secret.
    @Override
    public String toString() {
        return "WebhookProperties[enabled=" + enabled + ", secret=<redacted>]";
    }
}
