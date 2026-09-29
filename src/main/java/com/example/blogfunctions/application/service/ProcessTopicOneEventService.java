package com.example.blogfunctions.application.service;

import com.example.blogfunctions.application.port.inbound.ProcessTopicOneEventUseCase;
import com.example.blogfunctions.application.port.outbound.PublishGenericEventPort;
import com.example.blogfunctions.domain.model.TopicOneEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessTopicOneEventService implements ProcessTopicOneEventUseCase {
    private static final Logger log = LoggerFactory.getLogger(ProcessTopicOneEventService.class);
    private final PublishGenericEventPort publisherPort;

    public ProcessTopicOneEventService(PublishGenericEventPort publisherPort) {
        this.publisherPort = publisherPort;
    }

    @Override
    public void process(TopicOneEvent value) {
        if (value.payload() == null || value.payload().isBlank()) {
            throw new IllegalArgumentException("Cannot publish empty event payload");
        }
        publisherPort.publish(value.id(), value.payload());
        log.info("Topic one event outcome=forwarded eventId={}", value.id());
    }
}

