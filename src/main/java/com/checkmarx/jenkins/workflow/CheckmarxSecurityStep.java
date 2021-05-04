package com.checkmarx.jenkins.workflow;

import com.checkmarx.ast.*;
import com.checkmarx.jenkins.CheckmarxScanBuilder;
import com.checkmarx.jenkins.CxLoggerAdapter;
import com.checkmarx.jenkins.PluginUtils;
import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.model.ScanConfig;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import hudson.*;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;
import static hudson.Util.fixEmptyAndTrim;

public class CheckmarxSecurityStep  extends Step {

    // private static final Logger LOG = LoggerFactory.getLogger(CheckmarxSecurityStep.class.getName());
  //  private CxLoggerAdapter log;
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
    public CheckmarxSecurityStep() {
        // called from stapler
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
        this.projectName = Util.fixEmptyAndTrim(projectName);
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
        this.additionalOptions = Util.fixEmptyAndTrim(additionalOptions);
    }

    @SuppressWarnings("unused")
    public String getCheckmarxInstallation() {
        return checkmarxInstallation;
    }

    @DataBoundSetter
    public void setCheckmarxInstallation(String checkmarxInstallation) {
        this.checkmarxInstallation = checkmarxInstallation;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new CheckmarxSecurityStep.Execution(this, stepContext);
    }

    @Override
    public CheckmarxSecurityStepDescriptor getDescriptor() {
        return (CheckmarxSecurityStepDescriptor) super.getDescriptor();
    }

    @Extension
    @Symbol("checkmarxASTScan")
    public static class CheckmarxSecurityStepDescriptor extends StepDescriptor {

        private final CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor builderDescriptor;

        public CheckmarxSecurityStepDescriptor() {
            builderDescriptor = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>(Arrays.asList(EnvVars.class, Run.class, FilePath.class, Launcher.class, TaskListener.class));
        }

        @Override
        public String getFunctionName() {
            return "checkmarxASTScan";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Invoke Checkmarx AST Scan Pipeline Step";
        }

        @Override
        public String getConfigPage() {
            return getViewPage(CheckmarxScanBuilder.class, "config.jelly");
        }

        @SuppressWarnings("unused")
        public CheckmarxInstallation[] getInstallations() {
            return builderDescriptor.getInstallations();
        }

        @SuppressWarnings("unused")
        public boolean hasInstallationsAvailable() {
            return builderDescriptor.hasInstallationsAvailable();
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String checkmarxTokenId) {
            return builderDescriptor.doFillCredentialsIdItems(item, checkmarxTokenId);
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckCredentialsId(@AncestorInPath Item item, @QueryParameter String value) {
            return builderDescriptor.doCheckCredentialsId(item, value);
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Void> {


        private static final long serialVersionUID = 1L;

        private final transient CheckmarxSecurityStep checkmarxSecurityStep;
        private transient CxLoggerAdapter log;

        public Execution(@Nonnull CheckmarxSecurityStep checkmarxSecurityStep, @Nonnull StepContext context) {
            super(context);
            this.checkmarxSecurityStep = checkmarxSecurityStep;
        }

        @Override
        protected Void run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            log = new CxLoggerAdapter(taskListener.getLogger());

            EnvVars envVars = getContext().get(EnvVars.class);
            if (envVars == null) {
                log.error("Required context parameter 'EnvVars' is missing.");
                return null;
            }

            FilePath workspace = getContext().get(FilePath.class);
            if (workspace == null) {
                log.error("Required context parameter 'FilePath' (workspace) is missing.");
                return null;
            }
            Launcher launcher = getContext().get(Launcher.class);
            if (launcher == null) {
                log.error("Required context parameter 'Launcher' is missing.");
                return null;
            }
            Run build = getContext().get(Run.class);
            if (build == null) {
                log.error("Required context parameter 'Run' is missing.");
                return null;
            }

            // look for a checkmarx installation
            CheckmarxInstallation installation = PluginUtils.findCheckmarxInstallation(checkmarxSecurityStep.checkmarxInstallation);

            if (installation == null) {
                log.info("Checkmarx installation named '" + checkmarxSecurityStep.checkmarxInstallation + "' was not found. Please configure the build properly and retry.");
                build.setResult(Result.FAILURE);
                return null;
            }

                // install if necessary
                Computer computer = workspace.toComputer();
                Node node = computer != null ? computer.getNode() : null;
                if (node == null) {
                    log.info("Not running on a build node.");
                    build.setResult(Result.FAILURE);
                    return null;
                }

                installation = installation.forNode(node, taskListener);
                installation = installation.forEnvironment(envVars);
                String checkmarxCliExecutable = installation.getCheckmarxExecutable(launcher);

                if (checkmarxCliExecutable == null) {
                    log.info("Can't retrieve the Checkmarx executable.");
                    build.setResult(Result.FAILURE);
                    return null;
                }

                CheckmarxApiToken checkmarxToken = PluginUtils.getCheckmarxTokenCredential(build,checkmarxSecurityStep.credentialsId);
                if (checkmarxToken == null) {
                    log.error("Checkmarx API token with ID '" + checkmarxSecurityStep.credentialsId + "' was not found. Please configure the build properly and retry.");
                    build.setResult(Result.FAILURE);
                    return null;
                }

                //----------Integration with the wrapper------------

            final CheckmarxSecurityStepDescriptor descriptor = checkmarxSecurityStep.getDescriptor();
            ScanConfig scanConfig = resolveConfiguration(build, workspace, descriptor, envVars, log);
            printConfiguration(scanConfig, log);

            //Check for enabled scanners by the user.
            ArrayList<String> enabledScanners = PluginUtils.getEnabledScannersList(scanConfig, log);
            if(enabledScanners.isEmpty())
            {
                log.info("None of the scanners are enabled. Aborting the build.");
                build.setResult(Result.FAILURE);
                return null;
            }

            PluginUtils.submitScanDetailsToWrapper(scanConfig,checkmarxCliExecutable,log);
            return null;
            }

        private void printConfiguration(ScanConfig scanConfig, CxLoggerAdapter log) {

            log.info("----**** Checkmarx Scan Configuration ****----");
            log.info("Checkmarx Server Url: " + scanConfig.getServerUrl());
            log.info("Project Name: " + scanConfig.getProjectName());
            log.info("Team Name: " + scanConfig.getTeamName());
            log.info("Using Job Specific File filters: " + checkmarxSecurityStep.getUseFileFiltersFromJobConfig());

            if (checkmarxSecurityStep.getUseFileFiltersFromJobConfig()) {
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

        private ScanConfig resolveConfiguration(Run<?, ?> run, FilePath workspace, CheckmarxSecurityStepDescriptor descriptor, EnvVars envVars, CxLoggerAdapter log) {
            ScanConfig scanConfig = new ScanConfig();

            if (fixEmptyAndTrim(checkmarxSecurityStep.getProjectName()) != null) {
                scanConfig.setProjectName(checkmarxSecurityStep.getProjectName());
            }
            if (fixEmptyAndTrim(checkmarxSecurityStep.getTeamName()) != null) {
                scanConfig.setTeamName(checkmarxSecurityStep.getTeamName());
            }

//            if (descriptor.getUseAuthenticationUrl()) {
//                scanConfig.setBaseAuthUrl(descriptor.getBaseAuthUrl());
//            }

            if (checkmarxSecurityStep.getUseOwnServerCredentials()) {
                scanConfig.setServerUrl(checkmarxSecurityStep.getServerUrl());
                scanConfig.setCheckmarxToken(PluginUtils.getCheckmarxTokenCredential(run, checkmarxSecurityStep.getCredentialsId()));

            }
//            else {
//                scanConfig.setServerUrl(descriptor.getServerUrl());
//                scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(run, descriptor.getCredentialsId()));
//            }

            scanConfig.setSastEnabled(checkmarxSecurityStep.getSastEnabled());
            scanConfig.setScaEnabled(checkmarxSecurityStep.getScaEnabled());
            scanConfig.setKicsEnabled(checkmarxSecurityStep.getKicsEnabled());
            scanConfig.setContainerScanEnabled(checkmarxSecurityStep.getContainerScanEnabled());

            if (checkmarxSecurityStep.getUseFileFiltersFromJobConfig()) {
                scanConfig.setZipFileFilters(checkmarxSecurityStep.getZipFileFilters());
            }
//            else {
//                scanConfig.setZipFileFilters(descriptor.getZipFileFilters());
//            }

            if (fixEmptyAndTrim(checkmarxSecurityStep.getAdditionalOptions()) != null) {
                scanConfig.setAdditionalOptions(checkmarxSecurityStep.getAdditionalOptions());
            }

            File file = new File(workspace.getRemote());
            String sourceDir = file.getAbsolutePath();
            scanConfig.setSourceDirectory(sourceDir);

            return scanConfig;
        }

        }

}
