package com.example.blogfunctions.application.port.outbound;

/**
 * Outbound port for publishing events to downstream messaging systems.
 */
public interface PublishGenericEventPort {
    void publish(String id, String payload);
}
