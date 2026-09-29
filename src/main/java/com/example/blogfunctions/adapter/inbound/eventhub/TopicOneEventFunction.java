package com.example.blogfunctions.adapter.inbound.eventhub;

import com.example.blogfunctions.application.port.inbound.ProcessTopicOneEventUseCase;
import com.example.blogfunctions.domain.model.TopicOneEvent;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class TopicOneEventFunction {
    private static final Logger log = LoggerFactory.getLogger(TopicOneEventFunction.class);
    private final ProcessTopicOneEventUseCase useCase;
    private final JsonMapper mapper;

    public TopicOneEventFunction(ProcessTopicOneEventUseCase useCase, JsonMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @FunctionName("topicOneEventProcessor")
    @FixedDelayRetry(maxRetryCount = 5, delayInterval = "00:00:10")
    public void run(@EventHubTrigger(name = "events", eventHubName = "%EVENT_HUB_NAME%",
            connection = "EVENT_HUB_CONNECTION", consumerGroup = "%EVENT_HUB_CONSUMER_GROUP%",
            cardinality = Cardinality.MANY, dataType = "string") List<String> events,
            ExecutionContext context) {
        log.info("Event batch size={} invocationId={}", events.size(), context.getInvocationId());
        for (String raw : events) {
            TopicOneEvent event;
            try {
                var node = mapper.readTree(raw);
                if (node == null || !node.isObject() || (node.has("id") && !node.get("id").isTextual())) {
                    throw new IllegalArgumentException("Invalid event shape");
                }
                event = new TopicOneEvent(node.has("id") ? node.get("id").asText() : null, raw);
            } catch (JacksonException | IllegalArgumentException ex) {
                // Deliberate initial poison-event policy: skip malformed events without logging payloads.
                log.warn("Event outcome=rejected reason=invalid_json_object invocationId={}", context.getInvocationId());
                continue;
            }
            // Propagate processing failures so the Functions retry policy can retry the batch.
            useCase.process(event);
        }
        log.info("Event batch outcome=completed size={} invocationId={}", events.size(), context.getInvocationId());
    }
}
