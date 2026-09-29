package com.example.blogfunctions.domain.model;

/**
 * Event contract for consumed topic_one events.
 * id is optional; payload content stays outside logs.
 */
public record TopicOneEvent(String id, String payload) {
    public TopicOneEvent(String id) {
        this(id, null);
    }
}
