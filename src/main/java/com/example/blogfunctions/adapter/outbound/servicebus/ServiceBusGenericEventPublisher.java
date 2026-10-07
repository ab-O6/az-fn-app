package com.example.blogfunctions.adapter.outbound.servicebus;

import java.security.SecureRandom;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.example.blogfunctions.application.port.outbound.PublishGenericEventPort;

@Component
public class ServiceBusGenericEventPublisher implements PublishGenericEventPort {
    private static final Logger log = LoggerFactory.getLogger(ServiceBusGenericEventPublisher.class);
    private final ServiceBusSenderClient senderClient;

    private final List<String> traceIds = List.of(
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
            "00-a6e5b4c3d2e1f0a9b8c7d6e5f4a3b2c1-1234567890abcdef-01",
            "00-7f8e9d0c1b2a3f4e5d6c7b8a9f0e1d2c-abcdef1234567890-01",
            "00-1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d-fedcba0987654321-01",
            "00-9876543210abcdef0123456789abcdef-55aa55aa55aa55aa-01");

    private final SecureRandom random = new SecureRandom();

    @Autowired
    public ServiceBusGenericEventPublisher(ObjectProvider<ServiceBusSenderClient> senderClientProvider) {
        this(senderClientProvider.getIfAvailable());
    }

    public ServiceBusGenericEventPublisher(ServiceBusSenderClient senderClient) {
        this.senderClient = senderClient;
    }

    @Override
    public void publish(String id, String payload) {
        if (senderClient == null) {
            throw new IllegalStateException(
                    "ServiceBusSenderClient is not configured; verify app.service-bus.connection");
        }
        var message = new ServiceBusMessage(BinaryData.fromString(payload))
                .setContentType("application/json");
        if (id != null && !id.isBlank()) {
            message.setMessageId(id);
        }

        message.getApplicationProperties().put("traceparent", traceIds.get(random.nextInt(0, 5)));
        senderClient.sendMessage(message);
        log.info("Service bus message published topic={} eventId={}", senderClient.getEntityPath(), id);
    }
}
