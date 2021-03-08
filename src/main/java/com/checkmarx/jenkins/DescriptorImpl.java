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
    private String credentialsId;
    @Nullable
    private String zipFileFilters;

    @CopyOnWrite
    private volatile CheckmarxInstallation[] installations = new CheckmarxInstallation[0];

    public DescriptorImpl() {
        super(ScanBuilder.class); //to be confirmed
        this.load();
    }

    @Nullable
    public String getServerUrl() {
        return this.serverUrl;
    }

    public void setServerUrl(@Nullable final String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getBaseAuthUrl() {
        return this.baseAuthUrl;
    }

    public void setBaseAuthUrl(@Nullable final String baseAuthUrl) {
        this.baseAuthUrl = baseAuthUrl;
    }

    public String getCredentialsId() {
        return this.credentialsId;
    }

    public void setCredentialsId(final String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Nullable
    public String getZipFileFilters() {
        return zipFileFilters;
    }

    public void setZipFileFilters(@Nullable String zipFileFilters) {
        this.zipFileFilters = zipFileFilters;
    }

    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Execute Checkmarx Scan";
    }

    public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
        final JSONObject pluginData = formData.getJSONObject("checkmarx");
        req.bindJSON(this, pluginData);
        this.save();
        return false;
        //  return super.configure(req, formData);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public CheckmarxInstallation[] getInstallations() {
        return installations;
    }

    public void setInstallations(CheckmarxInstallation... installations) {
        this.installations = installations;
        save();
    }

    public boolean hasInstallationsAvailable() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Available Checkmarx installations: {}",
                    Arrays.stream(installations).map(CheckmarxInstallation::getName).collect(joining(",", "[", "]")));
        }

        return installations.length > 0;
    }

    public FormValidation doTestConnection(@QueryParameter String serverUrl, @QueryParameter String credentialsId, @AncestorInPath final Item item,
                                           @AncestorInPath Job job) {
        try {
            if (job == null) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            } else {
                job.checkPermission(Item.CONFIGURE);
            }
            // test logic here
            return FormValidation.ok("Success");
        } catch (Exception e) {
            return FormValidation.error("Client error : " + e.getMessage());
        }

    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Item item, @QueryParameter final String credentialsId) {

        final StandardListBoxModel result = new StandardListBoxModel();
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

//        return result
//                .includeEmptyValue()
//                .includeMatchingAs(
//                        item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM,
//                        item,
//                        CheckmarxApiToken.class,
//                        new ArrayList<>(),
//                        CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(CheckmarxApiToken.class)))
//                .includeCurrentValue(credentialsId);

    }

    public FormValidation doCheckCredentialsId(
            @AncestorInPath final Item item,
            @QueryParameter final String value
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

//        if (item == null) {
//            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
//                return FormValidation.ok();
//            }
//        } else {
//            if (!item.hasPermission(Item.EXTENDED_READ)
//                    && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
//                return FormValidation.ok();
//            }
//        }
//        if (StringUtils.isBlank(value)) {
//            return FormValidation.ok(); //print message in case of blank ?
//        }

//        if (CredentialsProvider.listCredentials(DefaultCheckmarxApiToken.class, item, item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM,
//                new ArrayList<>(),
//                CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(DefaultCheckmarxApiToken.class))).isEmpty()) {
//            return FormValidation.error("Cannot find currently selected credentials");
//        }

        //       return FormValidation.ok();
    }

    public String getCredentialsDescription() {
        if (getServerUrl() == null || getServerUrl().isEmpty()) {
            return "not set";
        }

        return "Server URL: " + getServerUrl();

    }


}

