package com.checkmarx.jenkins;

import com.checkmarx.ast.*;
import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.model.ScanConfig;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
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
    public ScanBuilder(boolean useOwnServerCredentials,
                       String serverUrl,
                       String projectName,
                       String teamName,
                       String credentialsId,
                       String sastFileFilters,
                       String scaFileFilters,
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
        this.projectName = (projectName == null) ? "TakefromBuildStep" : projectName;
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

        final DescriptorImpl descriptor = getDescriptor();
        log = new CxLoggerAdapter(listener.getLogger());
        EnvVars envVars = run.getEnvironment(listener);

        ScanConfig scanConfig = resolveConfiguration(run, workspace, descriptor, envVars, log);
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

        //----------Integration with the wrapper------------

        CxScanConfig scan = new CxScanConfig();
        scan.setBaseuri(scanConfig.getServerUrl());
        scan.setAuthType(CxAuthType.KEYSECRET);
        scan.setKey(scanConfig.getCheckmarxToken().getId());
        scan.setSecret(scanConfig.getCheckmarxToken().getToken().getPlainText());
        scan.setPathToExecutable(checkmarxCliExecutable);

        CxAuth wrapper = new CxAuth(scan, log);

        Map<CxParamType, String> params = new HashMap<>();
        params.put(CxParamType.D, scanConfig.getSourceDirectory());
        params.put(CxParamType.V, "");
        params.put(CxParamType.PROJECT_NAME, scanConfig.getProjectName());
        params.put(CxParamType.PROJECT_SOURCE_TYPE, ScanConfig.PROJECT_SOURCE_UPLOAD);
        params.put(CxParamType.PROJECT_TYPE, ScanConfig.SAST_SCAN_TYPE);
        params.put(CxParamType.FILTER, scanConfig.getZipFileFilters());
        params.put(CxParamType.ADDITIONAL_PARAMETERS, scanConfig.getAdditionalOptions());

        CxScan cxScan = wrapper.cxScanCreate(params);
        log.info(cxScan.toString());

        log.info("--------------- Checkmarx execution completed ---------------");

    }

    private void printConfiguration(ScanConfig scanConfig, CxLoggerAdapter log) {

        log.info("----**** Checkmarx Scan Configuration ****----");

        log.info("------ Global Configuration ------"); //Job Config
        if (!getDescriptor().getBaseAuthUrl().isEmpty()) {
            log.info("Checkmarx Access Control Url: " + scanConfig.getBaseAuthUrl());
        }
        log.info("Checkmarx Server Url: " + getDescriptor().getServerUrl());
        log.info("Global zip file filters: " + getDescriptor().getZipFileFilters());

        log.info("------ Build (Job) Configuration ------");   //Job Config
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

    private ScanConfig resolveConfiguration(Run<?, ?> run, FilePath workspace, DescriptorImpl descriptor, EnvVars envVars, CxLoggerAdapter log) {
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
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(getCredentialsId()));

        } else {
            scanConfig.setServerUrl(descriptor.getServerUrl());
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(descriptor.getCredentialsId()));
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

    private CheckmarxInstallation findCheckmarxInstallation() {
        return Stream.of((getDescriptor()).getInstallations())
                .filter(installation -> installation.getName().equals(checkmarxInstallation))
                .findFirst().orElse(null);
    }

    private CheckmarxApiToken getCheckmarxTokenCredential(String credentialsId) {
        return CredentialsMatchers.firstOrNull(lookupCredentials(CheckmarxApiToken.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()),
                withId(credentialsId));
    }

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }
}
