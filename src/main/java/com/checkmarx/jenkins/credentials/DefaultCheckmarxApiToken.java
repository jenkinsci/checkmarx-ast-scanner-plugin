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
    private final Secret token;

    @DataBoundConstructor
    public DefaultCheckmarxApiToken(final CredentialsScope scope, final String id, final String description, @Nonnull final String token) {
        super(scope, id, description);
        this.token = fromString(token);
    }

    @Nonnull
    @Override
    public Secret getToken() {
        return this.token;
    }

    @Extension
    public static class DefaultCheckmarxApiTokenDescriptor extends BaseStandardCredentials.BaseStandardCredentialsDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Checkmarx API token";
        }
    }


}
