package com.checkmarx.jenkins;

import com.checkmarx.jenkins.config.CheckmarxConstants;
import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.allOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.Util.fixEmptyAndTrim;
import static java.util.stream.Collectors.joining;

@Symbol("checkmarxPlugin")
@Extension
public final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    public static final String DEFAULT_FILTER_PATTERNS = CheckmarxConstants.DEFAULT_FILTER_PATTERNS;
    private static final Logger LOG = LoggerFactory.getLogger(DescriptorImpl.class.getName());
    //  Persistent plugin global configuration parameters
    @Nullable
    private String serverUrl;
    private String baseAuthUrl;
    private boolean useAuthenticationUrl;
    private String credentialsId;
    @Nullable
    private String zipFileFilters;

    @CopyOnWrite
    private volatile CheckmarxInstallation[] installations = new CheckmarxInstallation[0];

    public DescriptorImpl() {
        super(ScanBuilder.class); //to be confirmed
        load();
    }

    @Nullable
    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(@Nullable String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getBaseAuthUrl() {
        return baseAuthUrl;
    }

    public void setBaseAuthUrl(@Nullable String baseAuthUrl) {
        this.baseAuthUrl = baseAuthUrl;
    }

    public boolean getUseAuthenticationUrl() {
        return this.useAuthenticationUrl;
    }

    public void setUseAuthenticationUrl(final boolean useAuthenticationUrl) {
        this.useAuthenticationUrl = useAuthenticationUrl;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Nullable
    public String getZipFileFilters() {
        return this.zipFileFilters;
    }

    public void setZipFileFilters(@Nullable final String zipFileFilters) {
        this.zipFileFilters = zipFileFilters;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Execute Checkmarx Scan";
    }

    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        JSONObject pluginData = formData.getJSONObject("checkmarx");
        req.bindJSON(this, pluginData);
        save();
        return false;
        //  return super.configure(req, formData);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public CheckmarxInstallation[] getInstallations() {
        return this.installations;
    }

    public void setInstallations(final CheckmarxInstallation... installations) {
        this.installations = installations;
        this.save();
    }

    public boolean hasInstallationsAvailable() {
        if (DescriptorImpl.LOG.isTraceEnabled()) {
            DescriptorImpl.LOG.trace("Available Checkmarx installations: {}",
                    Arrays.stream(this.installations).map(CheckmarxInstallation::getName).collect(joining(",", "[", "]")));
        }

        return this.installations.length > 0;
    }

    public FormValidation doTestConnection(@QueryParameter final String serverUrl, @QueryParameter final String credentialsId, @AncestorInPath Item item,
                                           @AncestorInPath final Job job) {
        try {
            if (job == null) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            } else {
                job.checkPermission(Item.CONFIGURE);
            }
            // test logic here
            return FormValidation.ok("Success");
        } catch (final Exception e) {
            return FormValidation.error("Client error : " + e.getMessage());
        }

    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {

        StandardListBoxModel result = new StandardListBoxModel();
        if (item == null) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return result.includeCurrentValue(credentialsId);
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ)
                    && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return result.includeCurrentValue(credentialsId);
            }
        }
        return result.includeEmptyValue()
                .includeAs(ACL.SYSTEM, item, CheckmarxApiToken.class)
                .includeCurrentValue(credentialsId);

    }

    public FormValidation doCheckCredentialsId(
            @AncestorInPath Item item,
            @QueryParameter String value
    ) {
        if (fixEmptyAndTrim(value) == null) {
            return FormValidation.error("Checkmarx API token is required.");
        } else {
            if (null == CredentialsMatchers.firstOrNull(lookupCredentials(CheckmarxApiToken.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList()),
                    allOf(withId(value), CredentialsMatchers.instanceOf(CheckmarxApiToken.class)))) {
                return FormValidation.error("Cannot find currently selected Checkmarx API token.");
            }
        }
        return FormValidation.ok();

    }

    public String getCredentialsDescription() {
        if (this.getServerUrl() == null || this.getServerUrl().isEmpty()) {
            return "not set";
        }

        return "Server URL: " + this.getServerUrl();

    }

}