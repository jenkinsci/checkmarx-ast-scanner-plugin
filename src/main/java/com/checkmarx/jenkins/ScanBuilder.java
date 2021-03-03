package com.checkmarx.jenkins;

import com.checkmarx.ast.CxAuth;
import com.checkmarx.ast.CxAuthType;
import com.checkmarx.ast.CxParamType;
import com.checkmarx.ast.CxScan;
import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.credentials.DefaultCheckmarxApiToken;
import com.checkmarx.jenkins.model.ScanConfig;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import lombok.SneakyThrows;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.Util.fixEmptyAndTrim;

public class ScanBuilder extends Builder implements SimpleBuildStep {

    private static final Logger LOG = LoggerFactory.getLogger(ScanBuilder.class.getName());
    CxLoggerAdapter log;
    private boolean useOwnServerCredentials;
    @Nullable
    private String serverUrl;
    private String projectName;
    private String presetName;
    private String teamName;
    private String credentialsId;
    private String fileFilters;
    private String checkmarxInstallation;
    private String additionalOptions;
    @Nullable
    private String filterPattern;
    private boolean incrementalScan;
    private boolean sastEnabled;
    private boolean scaEnabled;

    @DataBoundConstructor
    public ScanBuilder(boolean useOwnServerCredentials,
                       String serverUrl,
                       String projectName,
                       String presetName,
                       String teamName,
                       String credentialsId,
                       boolean incrementalScan,
                       String fileFilters,
                       @Nullable String filterPattern,
                       boolean sastEnabled,
                       boolean scaEnabled,
                       String additionalOptions
    ) {
        this.useOwnServerCredentials = useOwnServerCredentials;
        this.serverUrl = serverUrl;
        this.projectName = (projectName == null) ? "TakefromBuildStep" : projectName;
        this.presetName = presetName;
        this.teamName = teamName;
        this.credentialsId = credentialsId;
        this.incrementalScan = incrementalScan;
        this.fileFilters = fileFilters;
        this.filterPattern = filterPattern;
        this.sastEnabled = sastEnabled;
        this.scaEnabled = scaEnabled;
        this.additionalOptions = additionalOptions;
    }

    static StandardCredentials getCredentialsById(String credentialsId, Run<?, ?> run) {
        return CredentialsProvider.findCredentialById(
                credentialsId,
                DefaultCheckmarxApiToken.class,
                run,
                Collections.emptyList());
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
    public void setProjectName(@Nullable String projectName) {
        this.projectName = projectName;
    }

    public String getPresetName() {
        return presetName;
    }

    @DataBoundSetter
    public void setPresetName(@Nullable String presetName) {
        this.presetName = presetName;
    }

    public String getTeamName() {
        return teamName;
    }

    @DataBoundSetter
    public void setTeamName(@Nullable String teamName) {
        this.teamName = teamName;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public boolean getIncrementalScan() {
        return incrementalScan;
    }

    @DataBoundSetter
    public void setIncrementalScan(@Nullable boolean incrementalScan) {
        this.incrementalScan = incrementalScan;
    }

    public String getFileFilters() {
        return fileFilters;
    }

    @DataBoundSetter
    public void setFileFilters(@Nullable String fileFilters) {
        this.fileFilters = fileFilters;
    }

    @Nullable
    public String getFilterPattern() {
        return filterPattern;
    }

    @DataBoundSetter
    public void setFilterPattern(@Nullable String filterPattern) {
        this.filterPattern = filterPattern;
    }

    public Boolean getSastEnabled() {
        return sastEnabled;
    }

    @DataBoundSetter
    public void setSastEnabled(Boolean sastEnabled) {
        this.sastEnabled = sastEnabled;
    }

    public Boolean getScaEnabled() {
        return scaEnabled;
    }

    @DataBoundSetter
    public void setScaEnabled(Boolean scaEnabled) {
        this.scaEnabled = scaEnabled;
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


        final DescriptorImpl descriptor = getDescriptor();
        log = new CxLoggerAdapter(listener.getLogger());
        EnvVars envVars = run.getEnvironment(listener);

        ScanConfig scanConfig = resolveConfiguration(run, descriptor, envVars, log);
        printConfiguration(scanConfig, log);

        //// Check for required version of CLI
        CheckmarxInstallation installation = findCheckmarxInstallation();
        String checkmarxCliExecutable;
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
        checkmarxCliExecutable = installation.getCheckmarxExecutable(launcher);

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

        if (installation != null) {
            VirtualChannel nodeChannel = node.getChannel();

            if (nodeChannel != null) {
                String toolHome = installation.getHome();
                if (fixEmptyAndTrim(toolHome) != null) {
                    FilePath checkmarxToolHome = new FilePath(nodeChannel, toolHome);
                    String customBuildPath = checkmarxToolHome.act(new CustomBuildToolPathCallable());
                    envVars.put("PATH", customBuildPath);

                    LOG.info("Custom build tool path: '{}'", customBuildPath);
                }
            }
        }

        //Integration with the wrapper
        CxAuth wrapper = new CxAuth(CxAuthType.KEYSECRET, scanConfig.getServerUrl(), scanConfig.getCheckmarxToken().getId(), scanConfig.getCheckmarxToken().getToken().getPlainText(), "");


        //get the current workspace where the sourcecode is checked out.
        File file = new File(workspace.getRemote());
        String sourceDir = file.getAbsolutePath();

        Map<CxParamType, String> params = new HashMap<>();
        params.put(CxParamType.D, sourceDir.toString());
        params.put(CxParamType.V, " ");
        params.put(CxParamType.INCREMENTAL, "false");
        params.put(CxParamType.PROJECT_NAME, scanConfig.getProjectName());
        params.put(CxParamType.PROJECT_SOURCE_TYPE, "upload");
        params.put(CxParamType.PROJECT_TYPE, "sast");
        params.put(CxParamType.PRESET_NAME, scanConfig.getPresetName());
        params.put(CxParamType.FILTERS, scanConfig.getZipFileFilters());

        CxScan cxScan = wrapper.cxScanCreate(params);
        log.info(cxScan.toString());

        log.info("--------------- Checkmarx execution completed ---------------");

    }

    private void printConfiguration(ScanConfig scanConfig, CxLoggerAdapter log) {

        log.info("--**** Checkmarx Scan Configuration ****--");
        log.info("Checkmarx Server Url: " + scanConfig.getServerUrl());
        log.info("Project Name: " + scanConfig.getProjectName());
        log.info("Team Name: " + scanConfig.getTeamName());
        log.info("Preset Name: " + scanConfig.getPresetName());
        log.info("Incremental Scan: " + scanConfig.isIncrementalScan());
        log.info("SAST File Filters: " + scanConfig.getSastScanFilters());
        log.info("SCA File Filters: " + scanConfig.getScaScanFilters());
        log.info("Additional Options: " + scanConfig.getAdditionalOptions());

    }

    private ScanConfig resolveConfiguration(Run<?, ?> run, DescriptorImpl descriptor, EnvVars envVars, CxLoggerAdapter log) {
        ScanConfig scanConfig = new ScanConfig();

        scanConfig.setProjectName(getProjectName());
        scanConfig.setTeamName(getTeamName());
        scanConfig.setPresetName(getPresetName());

        if (getUseOwnServerCredentials()) {
            scanConfig.setServerUrl(getServerUrl());
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(getCredentialsId()));

        } else {
            scanConfig.setServerUrl(descriptor.getServerUrl());
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(descriptor.getCredentialsId()));
        }

        scanConfig.setIncrementalScan(getIncrementalScan());

        if (getSastEnabled()) {
            scanConfig.setSastEnabled(getSastEnabled());
            scanConfig.setSastScanFilters("");
        }

        if (getScaEnabled()) {
            scanConfig.setScaEnabled(getScaEnabled());
            scanConfig.setScaScanFilters("");
        }

        scanConfig.setAdditionalOptions(getAdditionalOptions());

        return scanConfig;
    }

    private CheckmarxInstallation findCheckmarxInstallation() {
        return Stream.of((getDescriptor()).getInstallations())
                .filter(installation -> installation.getName().equals(checkmarxInstallation))
                .findFirst().orElse(null);
    }

    private void printStoredCredentials(Run<?, ?> run, DescriptorImpl descriptor, CxLoggerAdapter logger) {

        String credentialsId = descriptor.getCredentialsId();
        StandardCredentials creds = getCredentialsById(credentialsId, run);
        log.info(creds.getDescription());
        log.info(creds.toString());

    }

    private CheckmarxApiToken getCheckmarxTokenCredential(String credentialsId) {
        return CredentialsMatchers.firstOrNull(lookupCredentials(CheckmarxApiToken.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()),
                withId(this.credentialsId));
    }

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }
}
