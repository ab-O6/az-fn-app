package com.example.blogfunctions.adapter.outbound.servicebus;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.example.blogfunctions.application.port.outbound.PublishGenericEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceBusGenericEventPublisher implements PublishGenericEventPort {
    private static final Logger log = LoggerFactory.getLogger(ServiceBusGenericEventPublisher.class);
    private final ServiceBusSenderClient senderClient;

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
            throw new IllegalStateException("ServiceBusSenderClient is not configured; verify app.service-bus.connection");
        }
        var message = new ServiceBusMessage(BinaryData.fromString(payload))
                .setContentType("application/json");
        if (id != null && !id.isBlank()) {
            message.setMessageId(id);
        }
        senderClient.sendMessage(message);
        log.info("Service bus message published topic={} eventId={}", senderClient.getEntityPath(), id);
    }
}
