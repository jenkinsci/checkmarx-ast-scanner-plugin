package com.checkmarx.jenkins;

import com.checkmarx.jenkins.config.CheckmarxConstants;
import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.model.ScanConfig;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.*;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.Util.fixEmptyAndTrim;
import static java.util.stream.Collectors.joining;

public class CheckmarxScanBuilder extends Builder implements SimpleBuildStep {

    CxLoggerAdapter log;
    @Nullable
    private String serverUrl;
    private String projectName;
    private String teamName;
    private String credentialsId;
    private String checkmarxInstallation;
    private String additionalOptions;
    private String zipFileFilters;
    private boolean sastEnabled;
    private boolean scaEnabled;
    private boolean containerScanEnabled;
    private boolean kicsEnabled;
    private boolean useFileFiltersFromJobConfig;
    private boolean useOwnServerCredentials;

    @DataBoundConstructor
    public CheckmarxScanBuilder(boolean useOwnServerCredentials,
                                String serverUrl,
                                String projectName,
                                String teamName,
                                String credentialsId,
                                String zipFileFilters,
                                boolean sastEnabled,
                                boolean scaEnabled,
                                boolean containerScanEnabled,
                                boolean kicsEnabled,
                                boolean useFileFiltersFromJobConfig,
                                String additionalOptions
    ) {
        this.useOwnServerCredentials = useOwnServerCredentials;
        this.serverUrl = serverUrl;
        this.projectName = projectName;
        this.teamName = teamName;
        this.credentialsId = credentialsId;
        this.sastEnabled = sastEnabled;
        this.scaEnabled = scaEnabled;
        this.containerScanEnabled = containerScanEnabled;
        this.kicsEnabled = kicsEnabled;
        this.useFileFiltersFromJobConfig = useFileFiltersFromJobConfig;
        this.additionalOptions = additionalOptions;
        this.zipFileFilters = zipFileFilters;
    }

    public boolean getUseOwnServerCredentials() {
        return useOwnServerCredentials;
    }

    @DataBoundSetter
    public void setUseOwnServerCredentials(boolean useOwnServerCredentials) {
        this.useOwnServerCredentials = useOwnServerCredentials;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    @DataBoundSetter
    public void setServerUrl(@Nullable String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getProjectName() {
        return projectName;
    }

    @DataBoundSetter
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTeamName() {
        return teamName;
    }

    @DataBoundSetter
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public boolean getSastEnabled() {
        return sastEnabled;
    }

    @DataBoundSetter
    public void setSastEnabled(boolean sastEnabled) {
        this.sastEnabled = sastEnabled;
    }

    public boolean getScaEnabled() {
        return scaEnabled;
    }

    @DataBoundSetter
    public void setScaEnabled(boolean scaEnabled) {
        this.scaEnabled = scaEnabled;
    }

    public boolean getContainerScanEnabled() {
        return containerScanEnabled;
    }

    @DataBoundSetter
    public void setContainerScanEnabled(boolean containerScanEnabled) {
        this.containerScanEnabled = containerScanEnabled;
    }

    public boolean getKicsEnabled() {
        return kicsEnabled;
    }

    @DataBoundSetter
    public void setKicsEnabled(boolean kicsEnabled) {
        this.kicsEnabled = kicsEnabled;
    }

    public boolean getUseFileFiltersFromJobConfig() {
        return useFileFiltersFromJobConfig;
    }

    @DataBoundSetter
    public void setUseFileFiltersFromJobConfig(boolean useFileFiltersFromJobConfig) {
        this.useFileFiltersFromJobConfig = useFileFiltersFromJobConfig;
    }

    public String getZipFileFilters() {
        return zipFileFilters;
    }

    @DataBoundSetter
    public void setZipFileFilters(@Nullable String zipFileFilters) {
        this.zipFileFilters = zipFileFilters;
    }

    public String getAdditionalOptions() {
        return additionalOptions;
    }

    @DataBoundSetter
    public void setAdditionalOptions(@Nullable String additionalOptions) {
        this.additionalOptions = additionalOptions;
    }

    @SuppressWarnings("unused")
    public String getCheckmarxInstallation() {
        return checkmarxInstallation;
    }

    @DataBoundSetter
    public void setCheckmarxInstallation(String checkmarxInstallation) {
        this.checkmarxInstallation = checkmarxInstallation;
    }

    @SneakyThrows
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        final CheckmarxScanBuilderDescriptor descriptor = getDescriptor();
        log = new CxLoggerAdapter(listener.getLogger());
        EnvVars envVars = run.getEnvironment(listener);

        ScanConfig scanConfig = resolveConfiguration(run, workspace, descriptor, envVars, log);
        printConfiguration(scanConfig, log);

        //Check for enabled scanners by the user.
        ArrayList<String> enabledScanners = PluginUtils.getEnabledScannersList(scanConfig, log);
        if (enabledScanners.isEmpty()) {
            log.info("None of the scanners are enabled. Aborting the build.");
            run.setResult(Result.FAILURE);
            return;
        }

        //// Check for required version of CLI
        CheckmarxInstallation installation = PluginUtils.findCheckmarxInstallation(checkmarxInstallation);
        if (installation == null) {
            log.info("Checkmarx installation named '" + checkmarxInstallation + "' was not found. Please configure the build properly and retry.");
            run.setResult(Result.FAILURE);
            return;
        }

        // install if necessary
        Computer computer = workspace.toComputer();
        Node node = computer != null ? computer.getNode() : null;
        if (node == null) {
            log.info("Not running on a build node.");
            run.setResult(Result.FAILURE);
            return;
        }

        installation = installation.forNode(node, listener);
        installation = installation.forEnvironment(envVars);
        String checkmarxCliExecutable = installation.getCheckmarxExecutable(launcher);

        if (checkmarxCliExecutable == null) {
            log.info("Can't retrieve the Checkmarx executable.");
            run.setResult(Result.FAILURE);
            return;
        }
        log.info("This is the executable: " + checkmarxCliExecutable);

        // Check if the configured token is valid.
        CheckmarxApiToken checkmarxToken = scanConfig.getCheckmarxToken();
        if (checkmarxToken == null) {
            log.error("Checkmarx API token with ID '" + credentialsId + "' was not found. Please configure the build properly and retry.");
            run.setResult(Result.FAILURE);
            return;
        }

        //----------Integration with the wrapper------------
        PluginUtils.submitScanDetailsToWrapper(scanConfig, checkmarxCliExecutable, log);

    }

    private void printConfiguration(ScanConfig scanConfig, CxLoggerAdapter log) {

        log.info("----**** Checkmarx Scan Configuration ****----");
        log.info("Checkmarx Server Url: " + scanConfig.getServerUrl());
        log.info("Project Name: " + scanConfig.getProjectName());
        log.info("Team Name: " + scanConfig.getTeamName());
        log.info("Using Job Specific File filters: " + getUseFileFiltersFromJobConfig());

        if (getUseFileFiltersFromJobConfig()) {
            log.info("Using File Filters: " + scanConfig.getZipFileFilters());
        }

        log.info("Additional Options: " + scanConfig.getAdditionalOptions());

        log.info("Enabled Scan Engines: ");

        int scanEngines = 1;
        if (scanConfig.isSastEnabled()) {
            log.info((scanEngines++) + ") Checkmarx SAST");
        }
        if (scanConfig.isScaEnabled()) {
            log.info((scanEngines++) + ") Checkmarx SCA");
        }
        if (scanConfig.isContainerScanEnabled()) {
            log.info((scanEngines++) + ") Checkmarx Container Scan");
        }
        if (scanConfig.isKicsEnabled()) {
            log.info((scanEngines++) + ") Checkmarx KICS Scan");
        }

        log.info("-----------------------------------");
    }

    private ScanConfig resolveConfiguration(Run<?, ?> run, FilePath workspace, CheckmarxScanBuilderDescriptor descriptor, EnvVars envVars, CxLoggerAdapter log) throws IOException, InterruptedException {
        ScanConfig scanConfig = new ScanConfig();

        if (fixEmptyAndTrim(getProjectName()) != null) {
            scanConfig.setProjectName(getProjectName());
        }
        if (fixEmptyAndTrim(getTeamName()) != null) {
            scanConfig.setTeamName(getTeamName());
        }

        if (descriptor.getUseAuthenticationUrl()) {
            scanConfig.setBaseAuthUrl(descriptor.getBaseAuthUrl());
        }

        if (this.getUseOwnServerCredentials()) {
            scanConfig.setServerUrl(getServerUrl());
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(run, getCredentialsId()));

        } else {
            scanConfig.setServerUrl(descriptor.getServerUrl());
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(run, descriptor.getCredentialsId()));
        }

        scanConfig.setSastEnabled(getSastEnabled());
        scanConfig.setScaEnabled(getScaEnabled());
        scanConfig.setKicsEnabled(getKicsEnabled());
        scanConfig.setContainerScanEnabled(getContainerScanEnabled());

        if (getUseFileFiltersFromJobConfig()) {
            scanConfig.setZipFileFilters(this.getZipFileFilters());
        } else {
            scanConfig.setZipFileFilters(descriptor.getZipFileFilters());
        }

        if (fixEmptyAndTrim(getAdditionalOptions()) != null) {
            scanConfig.setAdditionalOptions(getAdditionalOptions());
        }

        File file = new File(workspace.getRemote());
        String sourceDir = file.getAbsolutePath();
        scanConfig.setSourceDirectory(sourceDir);

        return scanConfig;
    }

    private CheckmarxApiToken getCheckmarxTokenCredential(Run<?, ?> run, String credentialsId) {
        return findCredentialById(credentialsId, CheckmarxApiToken.class, run);
    }

    @Override
    public CheckmarxScanBuilderDescriptor getDescriptor() {
        return (CheckmarxScanBuilderDescriptor) super.getDescriptor();
    }

    @Symbol("checkmarxASTScanner")
    @Extension
    public static class CheckmarxScanBuilderDescriptor extends BuildStepDescriptor<Builder> {

        public static final String DEFAULT_FILTER_PATTERNS = CheckmarxConstants.DEFAULT_FILTER_PATTERNS;
        private static final Logger LOG = LoggerFactory.getLogger(CheckmarxScanBuilderDescriptor.class.getName());
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

        public CheckmarxScanBuilderDescriptor() {
            load();
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Execute Checkmarx AST Scan";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @SuppressFBWarnings("EI_EXPOSE_REP")
        public CheckmarxInstallation[] getInstallations() {
            return this.installations;
        }

        public void setInstallations(final CheckmarxInstallation... installations) {
            this.installations = installations;
            this.save();
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

        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            JSONObject pluginData = formData.getJSONObject("checkmarx");
            req.bindJSON(this, pluginData);
            save();
            return false;
            //  return super.configure(req, formData);
        }

        public boolean hasInstallationsAvailable() {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Available Checkmarx installations: {}",
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

        public FormValidation doCheckCredentialsId(@AncestorInPath Item item,
                                                   @QueryParameter String value
        ) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }

            if (fixEmptyAndTrim(value) == null) {
                return FormValidation.error("Checkmarx API token is required.");
            }

            if (null == CredentialsMatchers.firstOrNull(lookupCredentials(CheckmarxApiToken.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList()),
                    anyOf(withId(value), CredentialsMatchers.instanceOf(CheckmarxApiToken.class)))) {
                return FormValidation.error("Cannot find currently selected Checkmarx API token.");
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

}
