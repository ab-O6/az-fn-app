package com.example.blogfunctions.adapter.outbound.servicebus;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceBusGenericEventPublisherTest {

    @Test
    void publishesMessageWithContentTypeAndMessageId() {
        ServiceBusSenderClient client = mock(ServiceBusSenderClient.class);
        when(client.getEntityPath()).thenReturn("generic_events");
        ServiceBusGenericEventPublisher publisher = new ServiceBusGenericEventPublisher(client);

        publisher.publish("event-123", "{\"hello\":\"world\"}");

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(client).sendMessage(captor.capture());

        ServiceBusMessage sentMessage = captor.getValue();
        assertEquals("event-123", sentMessage.getMessageId());
        assertEquals("application/json", sentMessage.getContentType());
        assertEquals("{\"hello\":\"world\"}", sentMessage.getBody().toString());
    }

    @Test
    void publishesMessageWithoutMessageIdWhenNullOrBlank() {
        ServiceBusSenderClient client = mock(ServiceBusSenderClient.class);
        when(client.getEntityPath()).thenReturn("generic_events");
        ServiceBusGenericEventPublisher publisher = new ServiceBusGenericEventPublisher(client);

        publisher.publish(null, "{\"sample\":true}");

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(client).sendMessage(captor.capture());

        ServiceBusMessage sentMessage = captor.getValue();
        assertNull(sentMessage.getMessageId());
        assertEquals("application/json", sentMessage.getContentType());
        assertEquals("{\"sample\":true}", sentMessage.getBody().toString());
    }

    @Test
    void throwsIllegalStateExceptionWhenSenderClientNotConfigured() {
        ServiceBusGenericEventPublisher publisher = new ServiceBusGenericEventPublisher((ServiceBusSenderClient) null);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> publisher.publish("id", "{}"));
        assertTrue(ex.getMessage().contains("ServiceBusSenderClient is not configured"));
    }
}
