package com.example.blogfunctions.application.port.inbound;

import com.example.blogfunctions.domain.model.TopicOneEvent;

public interface ProcessTopicOneEventUseCase {
    void process(TopicOneEvent event);
}
