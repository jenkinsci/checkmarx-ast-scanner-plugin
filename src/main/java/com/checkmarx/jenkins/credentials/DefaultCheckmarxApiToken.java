package com.checkmarx.jenkins.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import lombok.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

import static hudson.util.Secret.fromString;

/**
 * Default implementation of {@link CheckmarxApiToken} for use by Jenkins {@link com.cloudbees.plugins.credentials.CredentialsProvider}
 * instances that store {@link Secret} locally.
 */

public class DefaultCheckmarxApiToken extends BaseStandardCredentials implements CheckmarxApiToken {

    @NonNull
    private final String clientId;

    @NonNull
    private final Secret secret;

    @DataBoundConstructor
    public DefaultCheckmarxApiToken(final CredentialsScope scope, final String id, final String description, @NonNull final String clientId, @NonNull final String secret) {
        super(scope, id, description);
        this.clientId = clientId;
        this.secret = fromString(secret);
    }
    @NonNull
    @Override
    public String getClientId() {
        return this.clientId;
    }

    @NonNull
    @Override
    public Secret getToken() {
        return this.secret;
    }

    @Extension
    public static class DefaultCheckmarxApiTokenDescriptor extends BaseStandardCredentials.BaseStandardCredentialsDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Checkmarx Client Id and Client Secret";
        }
    }
}
