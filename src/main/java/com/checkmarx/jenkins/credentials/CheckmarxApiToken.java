package com.checkmarx.jenkins.credentials;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;

import javax.annotation.Nonnull;
import java.io.IOException;

@NameWith(value = CheckmarxApiToken.NameProvider.class, priority = 1)
public interface CheckmarxApiToken extends StandardCredentials {
    @Nonnull
    Secret getToken() throws IOException, InterruptedException;

    class NameProvider extends CredentialsNameProvider<CheckmarxApiToken> {

        @Nonnull
        @Override
        public String getName(@Nonnull final CheckmarxApiToken credentials) {
            final String description = Util.fixEmptyAndTrim(credentials.getDescription());
            //return description != null ? description : credentials.getId();
            return credentials.getId() + (description != null ? " (" + description + ")" : "");

        }
    }
}
