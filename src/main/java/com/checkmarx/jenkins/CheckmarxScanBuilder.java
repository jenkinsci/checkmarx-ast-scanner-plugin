package com.checkmarx.jenkins;

import com.checkmarx.ast.wrapper.CxException;
import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.exception.CheckmarxException;
import com.checkmarx.jenkins.model.ScanConfig;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.*;
import hudson.model.*;
import hudson.security.ACL;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.verb.POST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.*;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.Util.fixEmptyAndTrim;
import static java.util.stream.Collectors.joining;

public class CheckmarxScanBuilder extends Builder implements SimpleBuildStep {

    public static final String DEFAULT_BRANCH_WARN = "If blank, branch name points to %s, %s or %s environment variables";

    public static final String GIT_BRANCH = "GIT_BRANCH";
    public static final String GIT_BRANCH_VAR = "${GIT_BRANCH}";
    public static final String CVS_BRANCH = "CVS_BRANCH";
    public static final String CVS_BRANCH_VAR = "${CVS_BRANCH}";
    public static final String SVN_REVISION = "SVN_REVISION";
    public static final String SVN_REVISION_VAR = "${SVN_REVISION}";
    public static final String LOGFILE = "./output.log";


    CxLoggerAdapter log;
    @Nullable
    private String serverUrl;
    private boolean useAuthenticationUrl;
    private String baseAuthUrl;
    private String tenantName;
    private String projectName;
    private String branchName;
    private String credentialsId;
    private String checkmarxInstallation;
    private String additionalOptions;
    private boolean useOwnAdditionalOptions;
    private boolean useOwnServerCredentials;

    @DataBoundConstructor
    public CheckmarxScanBuilder(boolean useOwnServerCredentials,
                                @Nullable String serverUrl,
                                boolean useAuthenticationUrl,
                                String baseAuthUrl,
                                String tenantName,
                                String projectName,
                                String credentialsId,
                                boolean useOwnAdditionalOptions,
                                String additionalOptions,
                                String branchName
    ) {
        this.useOwnServerCredentials = useOwnServerCredentials;
        this.serverUrl = serverUrl;
        this.useAuthenticationUrl = useAuthenticationUrl;
        this.baseAuthUrl = baseAuthUrl;
        this.tenantName = tenantName;
        this.projectName = projectName;
        this.credentialsId = credentialsId;
        this.useOwnAdditionalOptions = useOwnAdditionalOptions;
        this.additionalOptions = additionalOptions;
        this.branchName = branchName;
    }

    public CheckmarxScanBuilder() {

    }

    public boolean getUseOwnServerCredentials() {
        return useOwnServerCredentials;
    }

    @DataBoundSetter
    public void setUseOwnServerCredentials(boolean useOwnServerCredentials) {
        this.useOwnServerCredentials = useOwnServerCredentials;
    }

    @Nullable
    public String getServerUrl() {
        return serverUrl;
    }

    @DataBoundSetter
    public void setServerUrl(@Nullable String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getTenantName() {
        return tenantName;
    }

    @DataBoundSetter
    public void setTenantName(@Nullable String tenantName) {
        this.tenantName = tenantName;
    }

    public String getProjectName() {
        return projectName;
    }

    @DataBoundSetter
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getBranchName() {
        return branchName;
    }

    @DataBoundSetter
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public boolean getUseOwnAdditionalOptions() {
        return useOwnAdditionalOptions;
    }

    @DataBoundSetter
    public void setUseOwnAdditionalOptions(boolean useOwnAdditionalOptions) {
        this.useOwnAdditionalOptions = useOwnAdditionalOptions;
    }

    public String getAdditionalOptions() {
        return additionalOptions;
    }

    @DataBoundSetter
    public void setAdditionalOptions(@Nullable String additionalOptions) {
        this.additionalOptions = additionalOptions;
    }

    public String getCheckmarxInstallation() {
        return checkmarxInstallation;
    }

    @DataBoundSetter
    public void setCheckmarxInstallation(String checkmarxInstallation) {
        this.checkmarxInstallation = checkmarxInstallation;
    }

    public boolean isUseAuthenticationUrl() {
        return useAuthenticationUrl;
    }

    @DataBoundSetter
    public void setUseAuthenticationUrl(boolean useAuthenticationUrl) {
        this.useAuthenticationUrl = useAuthenticationUrl;
    }

    public String getBaseAuthUrl() {
        return baseAuthUrl;
    }

    @DataBoundSetter
    public void setBaseAuthUrl(String baseAuthUrl) {
        this.baseAuthUrl = baseAuthUrl;
    }


    @SneakyThrows
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, EnvVars envVars, @NonNull Launcher launcher, @NonNull TaskListener listener) {
        final CheckmarxScanBuilderDescriptor descriptor = getDescriptor();
        log = new CxLoggerAdapter(listener.getLogger());

        ScanConfig scanConfig;
        try {
            scanConfig = resolveConfiguration(run, workspace, descriptor, envVars);
        } catch (Exception e) {
            log.info(e.getMessage());
            run.setResult(Result.FAILURE);
            return;
        }

        PluginUtils.insertSecretsAsEnvVars(scanConfig, envVars);

        printConfiguration(envVars, descriptor, log);

        if (!getUseOwnServerCredentials()) checkmarxInstallation = descriptor.getCheckmarxInstallation();
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

        String checkmarxCliExecutable;

        installation = installation.forNode(node, listener);
        installation = installation.forEnvironment(envVars);
        checkmarxCliExecutable = Optional.of(installation.getCheckmarxExecutable(launcher)).orElseThrow(() -> new Exception("Cannot use node"));

        if (checkmarxCliExecutable == null) {
            log.info("Can't retrieve the Checkmarx executable.");
            run.setResult(Result.FAILURE);
            return;
        }

        // Check if the configured token is valid.
        CheckmarxApiToken checkmarxToken = scanConfig.getCheckmarxToken();
        if (checkmarxToken == null) {
            log.error("Checkmarx credentials with ID '" + credentialsId + "' was not found. Please configure the build properly and retry.");
            run.setResult(Result.FAILURE);
            return;
        }

        final List<String> argumentsForCommand = PluginUtils.submitScanDetailsToWrapper(scanConfig, checkmarxCliExecutable, this.log);
        ArgumentListBuilder arguments = new ArgumentListBuilder();
        FilePath tempDir = workspace.createTempDir("cx", "");
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        arguments.add(argumentsForCommand);

        try {
            int exitCode = launcher.launch().cmds(arguments).envs(envVars).stdout(
                    // Writing stdout to file
                    new OutputStream() {
                        @Override
                        public void write(int b) throws IOException {
                            fos.write(b);
                            listener.getLogger().write(b);
                        }

                        @Override
                        public void flush() throws IOException {
                            super.flush();
                            fos.flush();
                            listener.getLogger().flush();
                        }

                        @Override
                        public void close() throws IOException {
                            super.close();
                            fos.close();
                            listener.getLogger().close();
                        }
                    }).join();

            if (exitCode != 0) {
                log.error(String.format("Exit code from AST-CLI: %s", exitCode));
                log.info("Generating failed report");
                run.setResult(Result.FAILURE);
            }
        } catch (InterruptedException interruptedException) {
            String logFile = fos.toString(String.valueOf(StandardCharsets.UTF_8));
            String scanId = PluginUtils.getScanIdFromLogFile(logFile);
            if (!scanId.isEmpty()) {
                log.info("Cancelling scan with id: {}", scanId);
                launcher.launch().cmds(PluginUtils.scanCancel(UUID.fromString(scanId), scanConfig, checkmarxCliExecutable, this.log)).envs(envVars).stdout(listener.getLogger()).join();
                log.info("Successfully canceled scan with id: {}", scanId);
            }
            run.setResult(Result.ABORTED);
        } catch (Exception e) {
            log.info(e.getMessage());
            run.setResult(Result.FAILURE);
        }
        String logFile = fos.toString(String.valueOf(StandardCharsets.UTF_8));
        String scanId = PluginUtils.getScanIdFromLogFile(logFile);

        if (scanId.isEmpty()) {
            log.error("Scan ID is empty");
            return;
        }

        ArgumentListBuilder htmlArguments = new ArgumentListBuilder();
        ArgumentListBuilder jsonArguments = new ArgumentListBuilder();

        try {
            final List<String> htmlReportCommand = PluginUtils.generateHTMLReport(UUID.fromString(scanId), scanConfig, checkmarxCliExecutable, log);
            htmlArguments.add(htmlReportCommand);
            //Adding temp directory path name to command arguments
            htmlArguments.add("--output-path");
            htmlArguments.add(tempDir.getRemote());
            //Adding output file name to command arguments
            htmlArguments.add("--output-name");
            htmlArguments.add(PluginUtils.CHECKMARX_AST_RESULTS);

            launcher.launch().cmds(htmlArguments).envs(envVars).stdout(listener.getLogger()).join();

            final List<String> jsonReportCommand = PluginUtils.generateJsonReport(UUID.fromString(scanId), scanConfig, checkmarxCliExecutable, log);
            jsonArguments.add(jsonReportCommand);
            //Adding temp directory path name to command arguments
            jsonArguments.add("--output-path");
            jsonArguments.add(tempDir.getRemote());
            //Adding output file name to command arguments
            jsonArguments.add("--output-name");
            jsonArguments.add(PluginUtils.CHECKMARX_AST_RESULTS);

            launcher.launch().cmds(jsonArguments).envs(envVars).stdout(listener.getLogger()).join();

            //Getting created report files path
            FilePath htmlReportFilePath = tempDir.child(PluginUtils.CHECKMARX_AST_RESULTS_HTML);
            FilePath jsonReportFilePath = tempDir.child(PluginUtils.CHECKMARX_AST_RESULTS_JSON);

            ArtifactArchiver artifactArchiverHtml = new ArtifactArchiver(workspace.toURI().relativize(htmlReportFilePath.toURI()).toString());
            artifactArchiverHtml.perform(run, workspace, envVars, launcher, listener);

            ArtifactArchiver artifactArchiverJson = new ArtifactArchiver(workspace.toURI().relativize(jsonReportFilePath.toURI()).toString());
            artifactArchiverJson.perform(run, workspace, envVars, launcher, listener);

            saveInArtifactAdditionalReports(scanConfig, workspace, envVars, launcher, listener, run);

        } finally {
            //Deleting temporary directory to clean up the workspace env
            tempDir.deleteContents();
            tempDir.delete();
        }

        if (run.getActions(CheckmarxScanResultsAction.class).isEmpty()) {
            run.addAction(new CheckmarxScanResultsAction());
        }
        run.setResult(Result.SUCCESS);
    }

    private void saveInArtifactAdditionalReports(ScanConfig scanConfig, FilePath workspace, EnvVars envVars, Launcher launcher, TaskListener listener, Run<?, ?> run) throws IOException, InterruptedException {
        if (scanConfig.getAdditionalOptions().contains("--report-format")) {
            try {
                String additionalOptions = scanConfig.getAdditionalOptions();

                String formatTypes = extractOptionValue(additionalOptions, "--report-format");
                String[] formats = formatTypes.split(",");

                for (String formatType : formats) {
                    String fileName = (additionalOptions.contains("--output-name")
                            ? extractOptionValue(additionalOptions, "--output-name")
                            : PluginUtils.defaultOutputName) + "." + formatType;
                    String outputPath = additionalOptions.contains("--output-path")
                            ? extractOptionValue(additionalOptions, "--output-path")
                            : ".";
                    String fullFilePath = new File(outputPath, fileName).getPath();
                    FilePath destinationPath = workspace.child(fileName);

                    File fileToCopy = new File(fullFilePath);

                    if (fileToCopy.exists()) {
                        new FilePath(fileToCopy).copyTo(destinationPath);

                        ArtifactArchiver artifactArchiver = new ArtifactArchiver(fileName);
                        artifactArchiver.perform(run, workspace, envVars, launcher, listener);
                    }
                }
            } catch (Exception e) {
                log.error("Error saving additional reports: " + e.getMessage());
            }

        }
    }

    private String extractOptionValue(String options, String optionKey) {
        if (options.contains(optionKey)) {
            String[] parts = options.split(optionKey, 2);
            if (parts.length > 1) {
                return parts[1].trim().split(" ")[0];
            }
        }
        return "";
    }

    /**
     * Return branch name when filled by the user. Otherwise try to pick from environment variables
     *
     * @param envVars
     * @return
     */
    private String getBranchNameOrDefault(EnvVars envVars) {

        if (StringUtils.isNotEmpty(getBranchName())) return envVars.expand(getBranchName());
        if (StringUtils.isNotEmpty(envVars.get(GIT_BRANCH))) return envVars.get(GIT_BRANCH).replaceAll("^([^/]+)/", "");
        if (StringUtils.isNotEmpty(envVars.get(CVS_BRANCH))) return envVars.get(CVS_BRANCH);
        if (StringUtils.isNotEmpty(envVars.get(SVN_REVISION))) return envVars.get(SVN_REVISION);

        return "";
    }

    /**
     * Return branch name to print
     *
     * @param envVars
     * @return
     */
    private String getBranchToPrint(EnvVars envVars) {

        if (StringUtils.isNotEmpty(getBranchName())) return getBranchName();
        if (StringUtils.isNotEmpty(envVars.get(GIT_BRANCH))) return GIT_BRANCH_VAR;
        if (StringUtils.isNotEmpty(envVars.get(CVS_BRANCH))) return CVS_BRANCH_VAR;
        if (StringUtils.isNotEmpty(envVars.get(SVN_REVISION))) return SVN_REVISION_VAR;

        return "";
    }

    /**
     * Prints scan configuration which is gonna be used by the CLI
     *
     * @param envVars
     * @param descriptor
     * @param log
     */
    private void printConfiguration(EnvVars envVars, CheckmarxScanBuilderDescriptor descriptor, CxLoggerAdapter log) {
        log.info("----**** Checkmarx Scan Configuration ****----");

        String serverUrl = getUseOwnServerCredentials() ? getServerUrl() : descriptor.getServerUrl();
        log.info("Checkmarx Server Url: " + serverUrl);

        boolean useOwnBaseAuthUrl = getUseOwnServerCredentials() && isUseAuthenticationUrl();
        boolean useGlobalAuthUrl = !useOwnBaseAuthUrl && descriptor.getUseAuthenticationUrl();
        String authUrl = useOwnBaseAuthUrl ? getBaseAuthUrl() : useGlobalAuthUrl ? descriptor.getBaseAuthUrl() : "";

        if (StringUtils.isNotEmpty(authUrl)) {
            log.info("Checkmarx Auth Server Url: " + authUrl);
        }

        String tenantName = getUseOwnServerCredentials() ? getTenantName() : descriptor.getTenantName();
        log.info("Tenant Name: " + Optional.ofNullable(tenantName).orElse(""));
        log.info("Project Name: " + getProjectName());
        log.info("Branch name: " + getBranchToPrint(envVars));

        log.info("Using global additional options: " + !getUseOwnAdditionalOptions());

        String additionalOptions = getUseOwnAdditionalOptions() ? getAdditionalOptions() : descriptor.getAdditionalOptions();
        log.info("Additional Options: " + Optional.ofNullable(additionalOptions).orElse(""));

    }

    private ScanConfig resolveConfiguration(Run<?, ?> run, FilePath workspace, CheckmarxScanBuilderDescriptor descriptor, EnvVars envVars) throws CheckmarxException {

        checkMandatoryFields(descriptor);

        ScanConfig scanConfig = new ScanConfig();
        scanConfig.setProjectName(envVars.expand(getProjectName()));

        if (descriptor.getUseAuthenticationUrl()) {
            scanConfig.setBaseAuthUrl(envVars.expand(descriptor.getBaseAuthUrl()));
        }

        if (this.getUseOwnServerCredentials()) {
            scanConfig.setServerUrl(envVars.expand(getServerUrl()));
            scanConfig.setTenantName(envVars.expand(fixEmptyAndTrim(getTenantName())));
            if (this.isUseAuthenticationUrl()) {
                scanConfig.setBaseAuthUrl(envVars.expand(this.getBaseAuthUrl()));
            }
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(run, getCredentialsId()));

        } else {
            scanConfig.setServerUrl(envVars.expand(descriptor.getServerUrl()));
            scanConfig.setTenantName(envVars.expand(fixEmptyAndTrim(descriptor.getTenantName())));
            scanConfig.setCheckmarxToken(getCheckmarxTokenCredential(run, descriptor.getCredentialsId()));
        }

        String branchName = getBranchNameOrDefault(envVars);
        scanConfig.setBranchName(branchName);

        String additionalOptions = getUseOwnAdditionalOptions() ? getAdditionalOptions() : descriptor.getAdditionalOptions();
        if (fixEmptyAndTrim(additionalOptions) != null) {
            scanConfig.setAdditionalOptions(envVars.expand(additionalOptions));
        }

        scanConfig.setSourceDirectory(workspace.getRemote());

        return scanConfig;
    }

    /**
     * Check if all mandatory fields are filled in
     */
    private void checkMandatoryFields(CheckmarxScanBuilderDescriptor descriptor) throws CheckmarxException {
        if (fixEmptyAndTrim(getProjectName()) == null)
            throw new CheckmarxException("Please provide a valid project name.");
        if (!getUseOwnServerCredentials() && fixEmptyAndTrim(descriptor.getServerUrl()) == null)
            throw new CheckmarxException("Please setup the server url in the global settings.");
        if (!getUseOwnServerCredentials() && fixEmptyAndTrim(descriptor.getCredentialsId()) == null)
            throw new CheckmarxException("Please setup the credential in the global settings");
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

        private static final Logger LOG = LoggerFactory.getLogger(CheckmarxScanBuilderDescriptor.class.getName());
        @Nullable
        private String serverUrl;
        private String tenantName;
        private String baseAuthUrl;
        private boolean useAuthenticationUrl;
        private String checkmarxInstallation;
        private String credentialsId;
        @Nullable
        private String additionalOptions;

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

        public String getTenantName() {
            return tenantName;
        }

        public void setTenantName(@Nullable String tenantName) {
            this.tenantName = tenantName;
        }

        public String getCredentialsId() {
            return credentialsId;
        }

        public void setCredentialsId(String credentialsId) {
            this.credentialsId = credentialsId;
        }

        @Nullable
        public String getAdditionalOptions() {
            return this.additionalOptions;
        }

        public void setAdditionalOptions(@Nullable final String additionalOptions) {
            this.additionalOptions = additionalOptions;
        }

        public String getCheckmarxInstallation() {
            return checkmarxInstallation;
        }

        public void setCheckmarxInstallation(String checkmarxInstallation) {
            this.checkmarxInstallation = checkmarxInstallation;
        }

        public boolean hasInstallationsAvailable() {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Available Checkmarx installations: {}",
                        Arrays.stream(this.installations).map(CheckmarxInstallation::getName).collect(joining(",", "[", "]")));
            }

            return this.installations.length > 0;
        }

        public boolean configure(StaplerRequest req, JSONObject formData) {
            JSONObject pluginData = formData.getJSONObject("checkmarx");
            req.bindJSON(this, pluginData);
            save();
            return false;
        }

        @POST
        public FormValidation doCheckServerUrl(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.error("Server Url cannot be empty");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doTestConnection(@QueryParameter final String serverUrl,
                                               @QueryParameter final boolean useAuthenticationUrl,
                                               @QueryParameter final String baseAuthUrl,
                                               @QueryParameter final String tenantName,
                                               @QueryParameter final String credentialsId,
                                               @QueryParameter final String checkmarxInstallation,
                                               @AncestorInPath Item item,
                                               @AncestorInPath final Job job) {
            try {
                if (job == null) {
                    Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                } else {
                    job.checkPermission(Item.CONFIGURE);
                }

                String cxInstallationPath = getCheckmarxInstallationPath(checkmarxInstallation);
                CheckmarxApiToken checkmarxApiToken = getCheckmarxApiToken(credentialsId);

                DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = Jenkins.get().getGlobalNodeProperties();
                EnvironmentVariablesNodeProperty environmentVariablesNodeProperty = globalNodeProperties.get(hudson.slaves.EnvironmentVariablesNodeProperty.class);

                EnvVars envVars = environmentVariablesNodeProperty != null ?
                        environmentVariablesNodeProperty.getEnvVars() : new EnvVars();


                ScanConfig scanConfig = new ScanConfig();
                scanConfig.setServerUrl(envVars.expand(serverUrl));
                scanConfig.setBaseAuthUrl(useAuthenticationUrl ? envVars.expand(baseAuthUrl) : null);
                scanConfig.setTenantName(envVars.expand(tenantName));
                scanConfig.setCheckmarxToken(checkmarxApiToken);

                String message = PluginUtils.authValidate(scanConfig, cxInstallationPath);
                return FormValidation.ok(message);
            } catch (CxException e) {
                return FormValidation.error(e.getMessage());
            } catch (final Exception e) {
                return FormValidation.ok("Error: " + e.getMessage());
            }
        }

        private String getCheckmarxInstallationPath(String checkmarxInstallation) throws CheckmarxException, IOException, InterruptedException {
            if (StringUtils.isEmpty(checkmarxInstallation))
                throw new CheckmarxException("Checkmarx installation not provided");

            TaskListener taskListener = () -> System.out;
            Launcher launcher = Jenkins.get().createLauncher(taskListener);
            Computer computer = Arrays.stream(Jenkins.get().getComputers()).findFirst().orElseThrow(() -> new CheckmarxException("Error getting runner"));
            Node node = Optional.ofNullable(computer.getNode()).orElseThrow(() -> new CheckmarxException("Error getting runner"));

            CheckmarxInstallation cxInstallation = PluginUtils
                    .findCheckmarxInstallation(checkmarxInstallation)
                    .forNode(node, taskListener);

            return cxInstallation.getCheckmarxExecutable(launcher);
        }

        private CheckmarxApiToken getCheckmarxApiToken(String credentialsId) throws CheckmarxException {
            CheckmarxApiToken checkmarxCredentials =
                    CredentialsMatchers.firstOrNull(
                            lookupCredentials(CheckmarxApiToken.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList()),
                            withId(credentialsId));

            return Optional.ofNullable(checkmarxCredentials).orElseThrow(() -> new CheckmarxException("Error getting credentials"));
        }

        public FormValidation doCheckProjectName(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.error("Project Name cannot be empty");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckBranchName(@QueryParameter String value) {
            return FormValidation.warning(String.format(DEFAULT_BRANCH_WARN, GIT_BRANCH_VAR, CVS_BRANCH_VAR, SVN_REVISION_VAR));
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
                return FormValidation.error("Checkmarx credentials are required.");
            }

            if (null == CredentialsMatchers.firstOrNull(lookupCredentials(CheckmarxApiToken.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList()),
                    anyOf(withId(value), CredentialsMatchers.instanceOf(CheckmarxApiToken.class)))) {
                return FormValidation.error("Cannot find currently selected Checkmarx credentials.");
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
