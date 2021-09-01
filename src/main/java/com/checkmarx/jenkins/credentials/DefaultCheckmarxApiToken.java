package com.checkmarx.jenkins.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

import static hudson.util.Secret.fromString;

/**
 * Default implementation of {@link CheckmarxApiToken} for use by Jenkins {@link com.cloudbees.plugins.credentials.CredentialsProvider}
 * instances that store {@link Secret} locally.
 */

public class DefaultCheckmarxApiToken extends BaseStandardCredentials implements CheckmarxApiToken {

    @Nonnull
    private final String clientId;

    @Nonnull
    private final Secret secret;

    @DataBoundConstructor
    public DefaultCheckmarxApiToken(final CredentialsScope scope, final String id, final String description, @Nonnull final String clientId, @Nonnull final String secret) {
        super(scope, id, description);
        this.clientId = clientId;
        this.secret = fromString(secret);
    }
    @Nonnull
    @Override
    public String getClientId() {
        return this.clientId;
    }

    @Nonnull
    @Override
    public Secret getToken() {
        return this.secret;
    }

    @Extension
    public static class DefaultCheckmarxApiTokenDescriptor extends BaseStandardCredentials.BaseStandardCredentialsDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Checkmarx Client Id and Client Secret";
        }
    }
}
