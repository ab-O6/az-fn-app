package com.example.blogfunctions.application.service;

import com.example.blogfunctions.application.port.inbound.ProcessTopicOneEventUseCase;
import com.example.blogfunctions.domain.model.TopicOneEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessTopicOneEventService implements ProcessTopicOneEventUseCase {
    private static final Logger log = LoggerFactory.getLogger(ProcessTopicOneEventService.class);

    @Override
    public void process(TopicOneEvent value) {
        // Scaffold boundary: replace with a durable workflow before promising persistence.
        log.info("Topic one event outcome=processed");
    }
}
