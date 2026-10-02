package com.example.blogfunctions.configuration;

import com.azure.core.credential.TokenCredential;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AzureConfigurationTest {

    @Test
    void createsDefaultAzureCredentialWithoutClientId() {
        AzureConfiguration configuration = new AzureConfiguration();
        TokenCredential credential = configuration.tokenCredential(null);
        assertNotNull(credential);
    }

    @Test
    void createsDefaultAzureCredentialWithClientId() {
        AzureConfiguration configuration = new AzureConfiguration();
        TokenCredential credential = configuration.tokenCredential("test-client-id");
        assertNotNull(credential);
    }

    @Test
    void createsDefaultAzureCredentialWithBlankClientId() {
        AzureConfiguration configuration = new AzureConfiguration();
        TokenCredential credential = configuration.tokenCredential("   ");
        assertNotNull(credential);
    }
}
