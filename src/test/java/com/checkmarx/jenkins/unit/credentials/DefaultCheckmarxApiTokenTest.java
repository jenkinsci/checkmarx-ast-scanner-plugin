package com.checkmarx.jenkins.unit.credentials;

import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.credentials.DefaultCheckmarxApiToken;
import com.cloudbees.plugins.credentials.CredentialsScope;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

import static org.junit.Assert.*;

public class DefaultCheckmarxApiTokenTest {

    @Test
    public void testTokenCreation() {
        DefaultCheckmarxApiToken token = new DefaultCheckmarxApiToken(
            CredentialsScope.GLOBAL,
            "test-id",
            "test-description",
            "test-client-id",
            "test-secret"
        );

        assertEquals("test-id", token.getId());
        assertEquals("test-description", token.getDescription());
        assertEquals("test-client-id", token.getClientId());
        assertEquals("test-secret", token.getToken().getPlainText());
    }

    @Test
    public void testTokenCreationWithNullDescription() {
        DefaultCheckmarxApiToken token = new DefaultCheckmarxApiToken(
            CredentialsScope.GLOBAL,
            "test-id",
            null,
            "test-client-id",
            "test-secret"
        );

        assertEquals("test-id", token.getId());
        assertEquals(token.getDescription(), "");
        assertEquals("test-client-id", token.getClientId());
        assertEquals("test-secret", token.getToken().getPlainText());
    }

    @Test
    public void testTokenCreationWithEmptyStrings() {
        DefaultCheckmarxApiToken token = new DefaultCheckmarxApiToken(
            CredentialsScope.SYSTEM,
            "",
            "",
            "",
            ""
        );

        // Should generate UUID if id is empty
        Assertions.assertNotEquals("", token.getId());
        Assertions.assertNotNull(token.getId());
        assertEquals(token.getDescription(), "");
        assertEquals("", token.getClientId());
        assertEquals("", token.getToken().getPlainText());
    }

    @Test
    public void testTokenWithSystemScope() {
        DefaultCheckmarxApiToken token = new DefaultCheckmarxApiToken(
            CredentialsScope.SYSTEM,
            "test-id",
            "test-description",
            "test-client-id",
            "test-secret"
        );

        assertEquals(CredentialsScope.SYSTEM, token.getScope());
    }

    @Test
    public void testNameProviderWithDescription() throws IOException, InterruptedException {
        DefaultCheckmarxApiToken token = new DefaultCheckmarxApiToken(
            CredentialsScope.GLOBAL,
            "test-id",
            "test-description",
            "test-client-id",
            "test-secret"
        );

        CheckmarxApiToken.NameProvider nameProvider = new CheckmarxApiToken.NameProvider();
        assertEquals("test-id (test-description)", nameProvider.getName(token));
    }

    @Test
    public void testNameProviderWithoutDescription() throws IOException, InterruptedException {
        DefaultCheckmarxApiToken token = new DefaultCheckmarxApiToken(
            CredentialsScope.GLOBAL,
            "test-id",
            null,
            "test-client-id",
            "test-secret"
        );

        CheckmarxApiToken.NameProvider nameProvider = new CheckmarxApiToken.NameProvider();
        assertEquals("test-id", nameProvider.getName(token));
    }

    @Test
    public void testNameProviderWithEmptyDescription() throws IOException, InterruptedException {
        DefaultCheckmarxApiToken token = new DefaultCheckmarxApiToken(
            CredentialsScope.GLOBAL,
            "test-id",
            "",
            "test-client-id",
            "test-secret"
        );

        CheckmarxApiToken.NameProvider nameProvider = new CheckmarxApiToken.NameProvider();
        assertEquals("test-id", nameProvider.getName(token));
    }

    @Test
    public void testDescriptor() {
        DefaultCheckmarxApiToken.DefaultCheckmarxApiTokenDescriptor descriptor = 
            new DefaultCheckmarxApiToken.DefaultCheckmarxApiTokenDescriptor();
        
        assertEquals("Checkmarx Client Id and Client Secret", descriptor.getDisplayName());
    }
} 