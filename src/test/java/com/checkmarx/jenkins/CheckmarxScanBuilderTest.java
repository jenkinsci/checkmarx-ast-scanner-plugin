package com.checkmarx.jenkins;
import java.util.List;
import java.util.Arrays;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import jenkins.model.ArtifactManager;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.logging.Logger;

public class CheckmarxScanBuilderTest extends CheckmarxTestBase {

    private static final String CX_BASE_URI_ENV_VAR = String.format("${%s}", CheckmarxTestBase.CX_BASE_URI);
    private static final String CX_TENANT_ENV_VAR = String.format("${%s}", CheckmarxTestBase.CX_TENANT);
    private static final String CX_PROJECT_NAME_ENV_VAR = String.format("${%s}", CheckmarxTestBase.CX_PROJECT_NAME);
    private static final String CX_BRANCH_NAME_ENV_VAR = String.format("${%s}", CheckmarxTestBase.CX_BRANCH);

    private static final Logger log = Logger.getLogger(CheckmarxScanBuilderTest.class.getName());

    @Test
    public void successCheckmarxScan() throws Exception {
        log.info("successCheckmarxScan");
        this.runSuccessCheckmarxScan(this.astServerUrl, "JenkinsNormalScan", astTenantName, CheckmarxTestBase.BRANCH_MAIN);
    }

    @Test
    public void successCheckmarxScanWithEnvironmentVariables() throws Exception {
        log.info("successCheckmarxScanWithEnvironmentVariables");
        this.runSuccessCheckmarxScan(CX_BASE_URI_ENV_VAR, CX_PROJECT_NAME_ENV_VAR, CX_TENANT_ENV_VAR, CX_BRANCH_NAME_ENV_VAR);
    }

    @Test
    public void failWrongPresetCheckmarxScan() throws Exception {
        log.info("failWrongPresetCheckmarxScan");

        String projectName = "JenkinsScanWithInvalidPreset";
        final FreeStyleProject freeStyleProject = createSimpleProject(projectName);

        final CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setUseOwnServerCredentials(true);
        checkmarxScanBuilder.setProjectName(projectName);
        checkmarxScanBuilder.setServerUrl(this.astServerUrl);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setTenantName(this.astTenantName);
        checkmarxScanBuilder.setBranchName(CheckmarxTestBase.BRANCH_MAIN);
        checkmarxScanBuilder.setCheckmarxInstallation(CheckmarxTestBase.JT_LATEST);
        checkmarxScanBuilder.setCredentialsId(CheckmarxTestBase.JENKINS_CREDENTIALS_TOKEN_ID);
        checkmarxScanBuilder.setAdditionalOptions("--test");
        checkmarxScanBuilder.setUseOwnAdditionalOptions(true);

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.FAILURE, build);
        this.jenkins.assertLogContains("Exit code from AST-CLI: 1", build);
    }

    @Test
    public void failWrongAdditionalOption() throws Exception {
        log.info("failWrongAdditionalOption");

        String projectName = "JenkinsScanWithInvalidAdditionalOptions";
        final FreeStyleProject freeStyleProject = createSimpleProject(projectName);

        final CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setUseOwnServerCredentials(true);
        checkmarxScanBuilder.setProjectName(projectName);
        checkmarxScanBuilder.setServerUrl(this.astServerUrl);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setTenantName(this.astTenantName);
        checkmarxScanBuilder.setCheckmarxInstallation(CheckmarxTestBase.JT_LATEST);
        checkmarxScanBuilder.setCredentialsId(CheckmarxTestBase.JENKINS_CREDENTIALS_TOKEN_ID);
        checkmarxScanBuilder.setAdditionalOptions("--fakeproperty");
        checkmarxScanBuilder.setUseOwnAdditionalOptions(true);

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void failWrongCheckmarxInstallation() throws Exception {
        log.info("failWrongCheckmarxInstallation");

        String projectName = "JenkinsScanWithInvalidCheckmarxInstallation";
        final FreeStyleProject freeStyleProject = createSimpleProject(projectName);

        final CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setProjectName(projectName);
        checkmarxScanBuilder.setServerUrl(this.astServerUrl);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setTenantName(this.astTenantName);
        checkmarxScanBuilder.setCredentialsId(CheckmarxTestBase.JENKINS_CREDENTIALS_TOKEN_ID);
        checkmarxScanBuilder.setUseOwnServerCredentials(true);

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.FAILURE, build);
        this.jenkins.assertLogContains("Please configure the build properly and retry.", build);
    }
    @Test
    public void CheckmarxScanIsSuccessful_withReportFormat_ArtifactsShouldBeVerified() throws Exception {
        log.info("successCheckmarxScanAndVerifyArtifacts");

        final FreeStyleProject freeStyleProject = createSimpleProject("JenkinsNormalScanWithReport");
        final CheckmarxScanBuilder checkmarxScanBuilder = configureCheckmarxScanBuilder(this.astServerUrl, "JenkinsNormalScan", this.astTenantName, CheckmarxTestBase.BRANCH_MAIN);
        checkmarxScanBuilder.setAdditionalOptions("--scan-types iac-security --report-format sarif --output-name reportTest");

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.SUCCESS, build);

        ArtifactManager artifactManager = build.getArtifactManager();
        assertNotNull("ArtifactManager should not be null", artifactManager);

        List<String> expectedArtifacts = Arrays.asList("reportTest.sarif");
        for (String artifact : expectedArtifacts) {
            assertTrue("Artifact " + artifact + " should be present", artifactManager.root().child(artifact).exists());
        }
    }

    @Test
    public void failIfProjectNameNotProvided() throws Exception {
        log.info("failIfProjectNameNotProvided");

        final FreeStyleProject freeStyleProject = createSimpleProject("ScanWithoutProjectName");

        final CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setServerUrl(this.astServerUrl);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setTenantName(this.astTenantName);
        checkmarxScanBuilder.setCheckmarxInstallation(CheckmarxTestBase.JT_LATEST);
        checkmarxScanBuilder.setCredentialsId(CheckmarxTestBase.JENKINS_CREDENTIALS_TOKEN_ID);
        checkmarxScanBuilder.setUseOwnServerCredentials(true);

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.FAILURE, build);
        this.jenkins.assertLogContains("Please provide a valid project name.", build);
    }

    /**
     * Runs a checkmarx scan successfully
     *
     * @param serverUrl   - ast server url
     * @param projectName - ast project name
     * @param tenant      - ast tenant
     * @param branch      - ast branch
     * @throws Exception
     */
    private void runSuccessCheckmarxScan(String serverUrl, String projectName, String tenant, String branch) throws Exception {
        final FreeStyleProject freeStyleProject = createSimpleProject("JenkinsNormalScan");
        final CheckmarxScanBuilder checkmarxScanBuilder = configureCheckmarxScanBuilder(serverUrl, projectName, tenant, branch);
        checkmarxScanBuilder.setAdditionalOptions("--scan-types iac-security");

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    private CheckmarxScanBuilder configureCheckmarxScanBuilder(String serverUrl, String projectName, String tenant, String branch) {
        CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setUseOwnServerCredentials(true);
        checkmarxScanBuilder.setProjectName(projectName);
        checkmarxScanBuilder.setServerUrl(serverUrl);
        checkmarxScanBuilder.setTenantName(tenant);
        checkmarxScanBuilder.setBranchName(branch);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setCheckmarxInstallation(CheckmarxTestBase.JT_LATEST);
        checkmarxScanBuilder.setCredentialsId(CheckmarxTestBase.JENKINS_CREDENTIALS_TOKEN_ID);
        checkmarxScanBuilder.setUseOwnAdditionalOptions(true);

        return checkmarxScanBuilder;
    }
}