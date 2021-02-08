package com.checkmarx.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class ScanBuilder extends Builder implements SimpleBuildStep {

    @Nullable
    private String serverUrl;
    private String key;
    private String secret;
    private String projectName;
    private String scanTags;
    private String credentialsId;// to be implemented

    @DataBoundConstructor
    public ScanBuilder(String serverUrl,
                       String key,
                       String secret,
                       String projectName,
                       String scanTags
    ) {
        this.serverUrl = serverUrl;
        this.key = key;
        this.secret = Secret.fromString(secret).getEncryptedValue();
        this.projectName = (projectName == null) ? "TakefromBuildStep" : projectName;
        this.scanTags = scanTags;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    @DataBoundSetter
    public void setServerUrl(@Nullable String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getKey() {
        return key;
    }

    @DataBoundSetter
    public void setKey(@Nullable String key) {
        this.key = key;
    }

    public String getSecret() {
        return secret;
    }

    @DataBoundSetter
    public void setSecret(@Nullable String secret) {
        this.secret = secret;
    }

    public String getProjectName() {
        return projectName;
    }

    @DataBoundSetter
    public void setProjectName(@Nullable String projectName) {
        this.projectName = projectName;
    }

    public String getPscanTags() {
        return scanTags;
    }

    @DataBoundSetter
    public void setScanTags(@Nullable String scanTags) {
        this.scanTags = scanTags;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
    }

    //---------------------------  Descriptor class ---------------------------------

    @Symbol("checkmarxPlugin")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        //  Persistent plugin global configuration parameters
        @Nullable
        private String serverUrl;
        private String key;
        private String secret;
        private String credentialsId; //To be implemented


        @Nullable
        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(@Nullable String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public FormValidation doTestConnection(@QueryParameter final String serverUrl, @AncestorInPath Item item) {

            try {
                //try health-check from wrapper
            } catch (Exception e) {
                return FormValidation.error(e.getMessage(), "Failed to login to Checkmarx server");
            }
            return FormValidation.ok("Success");
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Execute Checkmarx Scan";
        }

    }

}
