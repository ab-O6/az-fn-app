package com.example.blogfunctions.configuration;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceBusConfigurationTest {

    @Test
    void propertiesDefaultsAndNamespaceFormatting() {
        ServiceBusProperties defaultProps = new ServiceBusProperties(null, null, null, null);
        assertEquals("generic_events", defaultProps.topicName());
        assertNull(defaultProps.fullyQualifiedNamespace());
        assertFalse(defaultProps.isConfigured());

        ServiceBusProperties shortNs = new ServiceBusProperties("axb2asb", "client-id-123", null, "custom_topic");
        assertEquals("axb2asb.servicebus.windows.net", shortNs.fullyQualifiedNamespace());
        assertTrue(shortNs.isConfigured());
        assertEquals("client-id-123", shortNs.clientId());
        assertEquals("custom_topic", shortNs.topicName());

        ServiceBusProperties fullNs = new ServiceBusProperties("axb2asb.servicebus.windows.net", null, null, "custom_topic");
        assertEquals("axb2asb.servicebus.windows.net", fullNs.fullyQualifiedNamespace());
        assertTrue(fullNs.isConfigured());

        ServiceBusProperties connOnly = new ServiceBusProperties(null, null, "Endpoint=sb://...;SharedAccessKey=secret", "custom_topic");
        assertTrue(connOnly.isConfigured());
        assertTrue(connOnly.toString().contains("<redacted>"));
        assertFalse(connOnly.toString().contains("secret"));
    }

    @Test
    void returnsNullWhenPropertiesNotConfigured() {
        ServiceBusConfiguration configuration = new ServiceBusConfiguration();
        ServiceBusProperties props = new ServiceBusProperties("", "", "", "generic_events");
        @SuppressWarnings("unchecked")
        ObjectProvider<TokenCredential> provider = mock(ObjectProvider.class);

        ServiceBusSenderClient client = configuration.serviceBusSenderClient(props, provider);
        assertNull(client);
    }

    @Test
    void initializesWithTokenCredentialWhenNamespaceProvided() {
        ServiceBusConfiguration configuration = new ServiceBusConfiguration();
        ServiceBusProperties props = new ServiceBusProperties("axb2asb.servicebus.windows.net", "uami-client-id", null, "test-topic");

        TokenCredential mockCredential = mock(TokenCredential.class);
        when(mockCredential.getToken(any())).thenReturn(Mono.just(new AccessToken("test-token", OffsetDateTime.MAX)));

        @SuppressWarnings("unchecked")
        ObjectProvider<TokenCredential> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenReturn(mockCredential);

        ServiceBusSenderClient client = configuration.serviceBusSenderClient(props, provider);
        assertNotNull(client);
        assertEquals("test-topic", client.getEntityPath());
        client.close();
    }
}
