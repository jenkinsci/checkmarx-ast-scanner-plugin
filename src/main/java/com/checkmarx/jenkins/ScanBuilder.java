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
import hudson.util.ArgumentListBuilder;
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
    private String presetName;
    private String teamName;
    private String credentialsId;
    private String checkmarxInstallation;
    private String additionalOptions;
    private boolean incrementalScan;
    private boolean sastEnabled;
    private boolean scaEnabled;
    private boolean containerScanEnabled;
    private boolean kicsEnabled;
    private boolean useGlobalFileFilters;
    private boolean useOwnServerCredentials;

    @DataBoundConstructor
    public ScanBuilder(boolean useOwnServerCredentials,
                       String serverUrl,
                       String projectName,
                       String presetName,
                       String teamName,
                       String credentialsId,
                       boolean incrementalScan,
                       String sastFileFilters,
                       String scaFileFilters,
                       boolean sastEnabled,
                       boolean scaEnabled,
                       boolean containerScanEnabled,
                       boolean kicsEnabled,
                       boolean useGlobalFileFilters,
                       String additionalOptions
    ) {
        this.useOwnServerCredentials = useOwnServerCredentials;
        this.serverUrl = serverUrl;
        this.projectName = (projectName == null) ? "TakefromBuildStep" : projectName;
        this.presetName = presetName;
        this.teamName = teamName;
        this.credentialsId = credentialsId;
        this.incrementalScan = incrementalScan;
        this.sastEnabled = sastEnabled;
        this.scaEnabled = scaEnabled;
        this.containerScanEnabled = containerScanEnabled;
        this.kicsEnabled = kicsEnabled;
        this.useGlobalFileFilters = useGlobalFileFilters;
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
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getPresetName() {
        return presetName;
    }

    @DataBoundSetter
    public void setPresetName(String presetName) {
        this.presetName = presetName;
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

    public boolean getIncrementalScan() {
        return incrementalScan;
    }

    @DataBoundSetter
    public void setIncrementalScan(boolean incrementalScan) {
        this.incrementalScan = incrementalScan;
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

    public boolean getUseGlobalFileFilters() {
        return useGlobalFileFilters;
    }

    @DataBoundSetter
    public void setUseGlobalFileFilters(boolean useGlobalFileFilters) {
        this.useGlobalFileFilters = useGlobalFileFilters;
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
        CxAuth wrapper = new CxAuth(CxAuthType.KEYSECRET, scanConfig.getServerUrl(), scanConfig.getCheckmarxToken().getId(), scanConfig.getCheckmarxToken().getToken().getPlainText(), checkmarxCliExecutable);

        Map<CxParamType, String> params = new HashMap<>();
        params.put(CxParamType.D, scanConfig.getSourceDirectory());
        params.put(CxParamType.V, " ");
        params.put(CxParamType.INCREMENTAL, "false");
        params.put(CxParamType.PROJECT_NAME, scanConfig.getProjectName());
        params.put(CxParamType.PROJECT_SOURCE_TYPE, "upload");
        params.put(CxParamType.PROJECT_TYPE, "sast");
        params.put(CxParamType.PRESET_NAME, scanConfig.getPresetName());
        params.put(CxParamType.FILTER, scanConfig.getZipFileFilters());

        CxScan cxScan = wrapper.cxScanCreate(params);
        log.info(cxScan.toString());


        //Directly using the cli from here

//        ArgumentListBuilder argsForTestCommand = buildArgumentList(checkmarxCliExecutable, "scan create", scanConfig);
//
//        FilePath cxTestReport = workspace.child("checkmarx_report.json");
//        FilePath cxTestDebug = workspace.child("checkmarx_report.json" + ".debug");
//        OutputStream cxTestOutput = null;
//        OutputStream cxTestDebugOutput = null;
//
//        log.info("Argument List: " + argsForTestCommand.toString());
//
//
//        try{
//
//            cxTestOutput = cxTestReport.write();
//            cxTestDebugOutput = cxTestDebug.write();
//
//            int exitCode = launcher.launch().cmds(argsForTestCommand).envs(envVars).stdout(cxTestOutput)
//                    .stderr(cxTestDebugOutput).quiet(true).pwd(workspace).join();
//
//            log.info("Exit code : " + exitCode);
//        }
//        catch (IOException ex) {
//            Util.displayIOException(ex, listener);
//            log.error("Checkmarx CLI command execution failed");
//            run.setResult(Result.FAILURE);
//            return ;
//        }finally {
//            if (cxTestOutput != null) {
//                cxTestOutput.close();
//            }
//            if (cxTestDebugOutput != null) {
//                cxTestDebugOutput.close();
//            }
//        }


        log.info("--------------- Checkmarx execution completed ---------------");

    }

    ArgumentListBuilder buildArgumentList(String checkmarxCliExecutable, String command, ScanConfig scanConfig) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder(checkmarxCliExecutable);

        args.add("scan");
        args.add("create");
        args.add("--key");
        args.add(scanConfig.getCheckmarxToken().getId());

        if (fixEmptyAndTrim(scanConfig.getCheckmarxToken().getToken().getPlainText()) != null) {
            args.add("--secret");
            args.add(scanConfig.getCheckmarxToken().getToken().getPlainText());
        }

        args.add("--base-uri");
        args.addQuoted(scanConfig.getServerUrl());

        args.add("--preset-name");
        args.add(scanConfig.getPresetName());

        args.add("--project-name");
        args.add(scanConfig.getProjectName());

        args.add("--filter");
        args.addQuoted(scanConfig.getZipFileFilters());

        args.add("--directory");
        args.addQuoted(scanConfig.getSourceDirectory());

        args.add("--project-source-type");
        args.addQuoted("upload");

        args.add("--incremental");
        args.add(scanConfig.isIncrementalScan());

        args.add("--project-type");
        args.add("sast");

        return args;
    }

    private void printConfiguration(ScanConfig scanConfig, CxLoggerAdapter log) {

        log.info("--**** Checkmarx Scan Configuration ****--");

        if (!getDescriptor().getBaseAuthUrl().isEmpty()) {
            log.info("Checkmarx Access Control Url: " + scanConfig.getBaseAuthUrl());
        }

        log.info("Checkmarx Server Url: " + scanConfig.getServerUrl());
        log.info("Project Name: " + scanConfig.getProjectName());
        log.info("Team Name: " + scanConfig.getTeamName());
        log.info("Preset Name: " + scanConfig.getPresetName());
        log.info("Incremental Scan: " + scanConfig.isIncrementalScan());
        log.info("Using Global Zip File Filters: " + getUseGlobalFileFilters());

        if (getUseGlobalFileFilters()) {
            log.info("Using Global Zip File Filters: " + scanConfig.getZipFileFilters());
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

    }

    private ScanConfig resolveConfiguration(Run<?, ?> run, FilePath workspace, DescriptorImpl descriptor, EnvVars envVars, CxLoggerAdapter log) {
        ScanConfig scanConfig = new ScanConfig();

        if (fixEmptyAndTrim(getProjectName()) != null) {
            scanConfig.setProjectName(getProjectName());
        }
        if (fixEmptyAndTrim(getTeamName()) != null) {
            scanConfig.setTeamName(getTeamName());
        }
        if (fixEmptyAndTrim(getPresetName()) != null) {
            scanConfig.setPresetName(getPresetName());
        }

//        if (descriptor.getUsebaseAuthUrl()) {
            scanConfig.setBaseAuthUrl(descriptor.getBaseAuthUrl());
//        }

        if (this.getUseOwnServerCredentials()) {
            scanConfig.setServerUrl(getServerUrl());
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(getCredentialsId()));

        } else {
            scanConfig.setServerUrl(descriptor.getServerUrl());
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(descriptor.getCredentialsId()));
        }

        scanConfig.setIncrementalScan(getIncrementalScan());

        scanConfig.setSastEnabled(getSastEnabled());
        scanConfig.setScaEnabled(getScaEnabled());
        scanConfig.setKicsEnabled(getKicsEnabled());
        scanConfig.setContainerScanEnabled(getContainerScanEnabled());

        if (getUseGlobalFileFilters()) {
            scanConfig.setZipFileFilters(descriptor.getZipFileFilters());
        }
        scanConfig.setAdditionalOptions(getAdditionalOptions());

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
