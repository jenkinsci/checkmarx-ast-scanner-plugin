package com.checkmarx.jenkins.credentials;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;
import lombok.NonNull;

import java.io.IOException;

@NameWith(value = CheckmarxApiToken.NameProvider.class, priority = 1)
public interface CheckmarxApiToken extends StandardCredentials {
    @NonNull
    String getClientId() throws IOException, InterruptedException;

    @NonNull
    Secret getToken() throws IOException, InterruptedException;

    class NameProvider extends CredentialsNameProvider<CheckmarxApiToken> {

        @NonNull
        @Override
        public String getName(@NonNull final CheckmarxApiToken credentials) {
            final String description = Util.fixEmptyAndTrim(credentials.getDescription());

            return credentials.getId() + (description != null ? " (" + description + ")" : "");
        }
    }
}
