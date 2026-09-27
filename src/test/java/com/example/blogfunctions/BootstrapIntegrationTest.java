package com.example.blogfunctions;

import com.example.blogfunctions.adapter.inbound.eventhub.TopicOneEventFunction;
import com.example.blogfunctions.adapter.inbound.http.SubmitBlogForReviewFunction;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Integration coverage for bean naming, configuration binding, and constructor wiring. */
class BootstrapIntegrationTest {
    @Test void createsBothAzureEntryPointsWithSpring() {
        try (var context = new SpringApplicationBuilder(BlogFunctionsApplication.class)
                .web(WebApplicationType.NONE)
                .run("--app.webhook.signature.enabled=false")) {
            assertNotNull(context.getBean(SubmitBlogForReviewFunction.class));
            assertNotNull(context.getBean(TopicOneEventFunction.class));
        }
    }
}
