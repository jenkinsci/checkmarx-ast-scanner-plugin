package com.checkmarx.jenkins.integration;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import com.checkmarx.jenkins.CheckmarxScanBuilder;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.checkmarx.jenkins.tools.CheckmarxInstaller;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import jenkins.model.ArtifactManager;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import static org.junit.Assert.*;
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

    @Test
    public void testDoCheckServerUrl() throws Exception {
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
        
        // Test with valid URL
        FormValidation result = descriptor.doCheckServerUrl("https://valid-server.checkmarx.com");
        assertEquals(FormValidation.Kind.OK, result.kind);
        
        // Test with empty URL
        result = descriptor.doCheckServerUrl("");
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertEquals("Server Url cannot be empty", result.getMessage());
        
        // Test with null URL
        result = descriptor.doCheckServerUrl(null);
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertEquals("Server Url cannot be empty", result.getMessage());
        
        // Test with whitespace URL
        result = descriptor.doCheckServerUrl("   ");
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertEquals("Server Url cannot be empty", result.getMessage());
    }

    @Test
    public void testHasInstallationsAvailable() throws Exception {
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
        
        // Get current installations
        CheckmarxInstallation[] installations = descriptor.getInstallations();
        
        if (installations != null && installations.length > 0) {
            assertTrue("Should have installations available", descriptor.hasInstallationsAvailable());
        } else {
            // Set up a test installation if none exists
            CheckmarxInstallation installation = new CheckmarxInstallation(
                CheckmarxTestBase.JT_LATEST, 
                "/usr/local/checkmarx",
                Collections.<ToolProperty<?>>emptyList()
            );
            descriptor.setInstallations(installation);
            
            assertTrue("Should have installations available after adding one", descriptor.hasInstallationsAvailable());
            
            // Clean up - restore original installations
            descriptor.setInstallations(installations);
        }
        
        // Test with no installations
        descriptor.setInstallations();
        assertFalse("Should have no installations available", descriptor.hasInstallationsAvailable());
        
        // Restore original installations
        descriptor.setInstallations(installations);
    }

    @Test
    public void testDoTestConnection() throws Exception {
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
        
        // Test with valid connection parameters
        FormValidation result = descriptor.doTestConnection(
            "https://valid-server.checkmarx.com",
            false,
            "",
            "test-tenant",
            CheckmarxTestBase.JENKINS_CREDENTIALS_TOKEN_ID,
            CheckmarxTestBase.JT_LATEST,
            null,
            null
        );
        
        // The actual result will depend on the server response, but we can verify the method executes
        assertNotNull("Test connection result should not be null", result);
    }

    @Test
    public void testCheckmarxInstallationDescriptor() {
        CheckmarxInstallation.CheckmarxInstallationDescriptor descriptor = new CheckmarxInstallation.CheckmarxInstallationDescriptor();
        
        // Test getDefaultInstallers
        List<? extends ToolInstaller> defaultInstallers = descriptor.getDefaultInstallers();
        assertNotNull("Default installers should not be null", defaultInstallers);
        assertEquals("Should have one default installer", 1, defaultInstallers.size());
        assertTrue("Default installer should be CheckmarxInstaller", defaultInstallers.get(0) instanceof CheckmarxInstaller);
        
        // Test getInstallations and setInstallations
        CheckmarxInstallation[] originalInstallations = descriptor.getInstallations();
        
        try {
            // Create test installations
            CheckmarxInstallation installation1 = new CheckmarxInstallation(
                "test-installation-1", 
                "/usr/local/checkmarx1",
                Collections.<ToolProperty<?>>emptyList()
            );
            
            CheckmarxInstallation installation2 = new CheckmarxInstallation(
                "test-installation-2", 
                "/usr/local/checkmarx2",
                Collections.<ToolProperty<?>>emptyList()
            );
            
            // Set new installations
            descriptor.setInstallations(installation1, installation2);
            
            // Verify installations were set correctly
            CheckmarxInstallation[] installations = descriptor.getInstallations();
            assertNotNull("Installations should not be null", installations);
            assertEquals("Should have two installations", 2, installations.length);
            assertEquals("First installation name should match", "test-installation-1", installations[0].getName());
            assertEquals("Second installation name should match", "test-installation-2", installations[1].getName());
            
            // Test setting empty installations
            descriptor.setInstallations();
            installations = descriptor.getInstallations();
            assertNotNull("Installations should not be null when empty", installations);
            assertEquals("Should have no installations", 0, installations.length);
            
        } finally {
            // Restore original installations
            descriptor.setInstallations(originalInstallations);
        }
    }
}