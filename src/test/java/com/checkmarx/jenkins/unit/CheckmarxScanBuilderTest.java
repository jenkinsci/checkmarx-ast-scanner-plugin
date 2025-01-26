package com.checkmarx.jenkins.unit;

import com.checkmarx.jenkins.CheckmarxScanBuilder;
import com.checkmarx.jenkins.CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.FormValidation;
import hudson.tasks.Builder;
import hudson.tools.ToolProperty;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

public class CheckmarxScanBuilderTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private CheckmarxScanBuilder scanBuilder;
    private CheckmarxScanBuilderDescriptor descriptor;

    @Before
    public void before() throws IOException {
        scanBuilder = new CheckmarxScanBuilder();
        descriptor = new CheckmarxScanBuilderDescriptor();
        j.jenkins.getDescriptorList(Builder.class).add(descriptor);

        // Initialize descriptor with test values
        descriptor.setServerUrl("http://example.com");
        descriptor.setTenantName("test-tenant");
        descriptor.setAdditionalOptions("--test-option");
        descriptor.setCredentialsId("test-cred");

        // Create test installation
        CheckmarxInstallation installation = new CheckmarxInstallation("test-installation", "test-home", Collections.<ToolProperty<?>>emptyList());
        descriptor.setInstallations(new CheckmarxInstallation[]{installation});
    }

    @Test
    public void testConstructor_withAllParameters_ExpectAllFieldsSet() {
        scanBuilder = new CheckmarxScanBuilder();
        scanBuilder.setProjectName("TestProject");
        scanBuilder.setServerUrl("http://example.com");
        scanBuilder.setTenantName("TestTenant");
        scanBuilder.setCredentialsId("TestCred");
        scanBuilder.setAdditionalOptions("--test-option");

        assertEquals("TestProject", scanBuilder.getProjectName());
        assertEquals("http://example.com", scanBuilder.getServerUrl());
        assertEquals("TestTenant", scanBuilder.getTenantName());
        assertEquals("TestCred", scanBuilder.getCredentialsId());
        assertEquals("--test-option", scanBuilder.getAdditionalOptions());
    }

    @Test
    public void testDescriptorDisplayName_ExpectCorrectName() {
        assertEquals("Execute Checkmarx AST Scan", descriptor.getDisplayName());
    }

    @Test
    public void testDescriptorIsApplicable_withFreeStyleProject_ExpectTrue() {
        assertTrue(descriptor.isApplicable(FreeStyleProject.class));
    }

    @Test
    public void testDescriptorFormValidation_withEmptyServerUrl_ExpectError() {
        FormValidation result = descriptor.doCheckServerUrl("");
        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertEquals("Server Url cannot be empty", result.getMessage());
    }

    @Test
    public void testDescriptorFormValidation_withValidServerUrl_ExpectOk() {
        FormValidation result = descriptor.doCheckServerUrl("http://example.com");
        assertEquals(FormValidation.Kind.OK, result.kind);
    }

    @Test
    public void testDescriptorHasInstallations_withNoInstallations_ExpectFalse() {
        descriptor.setInstallations();
        assertFalse(descriptor.hasInstallationsAvailable());
    }

    @Test
    public void testDescriptorHasInstallations_withInstallations_ExpectTrue() {
        CheckmarxInstallation installation = new CheckmarxInstallation("test-install", "test-home", Collections.<ToolProperty<?>>emptyList());
        descriptor.setInstallations(installation);
        assertTrue(descriptor.hasInstallationsAvailable());
    }

    @Test
    public void testDescriptorSetGetServerUrl_withValidUrl_ExpectMatch() {
        descriptor.setServerUrl("http://example.com");
        assertEquals("http://example.com", descriptor.getServerUrl());
    }

    @Test
    public void testDescriptorSetGetTenantName_withValidName_ExpectMatch() {
        descriptor.setTenantName("test-tenant");
        assertEquals("test-tenant", descriptor.getTenantName());
    }

    @Test
    public void testDescriptorSetGetCredentialsId_withValidId_ExpectMatch() {
        descriptor.setCredentialsId("test-cred");
        assertEquals("test-cred", descriptor.getCredentialsId());
    }

    @Test
    public void testDescriptorSetGetAdditionalOptions_withValidOptions_ExpectMatch() {
        descriptor.setAdditionalOptions("--test-option");
        assertEquals("--test-option", descriptor.getAdditionalOptions());
    }

    @Test
    public void testDescriptorSetGetCheckmarxInstallation_withValidInstallation_ExpectMatch() {
        descriptor.setCheckmarxInstallation("test-install");
        assertEquals("test-install", descriptor.getCheckmarxInstallation());
    }

    @Test
    public void testPerform_withMissingGlobalConfig_ExpectBuildFailure() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        CheckmarxScanBuilder builder = new CheckmarxScanBuilder();
        project.getBuildersList().add(builder);

        j.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));
    }

    @Test
    public void testServerConfiguration_withGlobalAndLocalSettings_ExpectCorrectPrecedence() {
        CheckmarxScanBuilder builder = new CheckmarxScanBuilder();
        
        // Test with global settings
        builder.setUseOwnServerCredentials(false);
        builder.setServerUrl("http://local.example.com");
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = builder.getDescriptor();
        descriptor.setServerUrl("http://global.example.com");
        
        assertEquals("http://global.example.com", descriptor.getServerUrl());
        
        // Test with local settings
        builder.setUseOwnServerCredentials(true);
        assertEquals("http://local.example.com", builder.getServerUrl());
    }

    @Test
    public void testGetCredentialsDescription_withNullServerUrl_ExpectNotSet() {
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
        descriptor.setServerUrl(null);
        assertEquals("not set", descriptor.getCredentialsDescription());
    }

    @Test
    public void testGetCredentialsDescription_withEmptyServerUrl_ExpectNotSet() {
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
        descriptor.setServerUrl("");
        assertEquals("not set", descriptor.getCredentialsDescription());
    }

    @Test
    public void testGetCredentialsDescription_withValidServerUrl_ExpectServerUrlDescription() {
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
        descriptor.setServerUrl("http://example.com");
        assertEquals("Server URL: http://example.com", descriptor.getCredentialsDescription());
    }

    @Test
    public void testCheckCredentialsId_withNullValue_ExpectErrorMessage() {
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
        assertEquals("Checkmarx credentials are required.", descriptor.doCheckCredentialsId(null, null).getMessage());
    }

    @Test
    public void testCheckCredentialsId_withEmptyValue_ExpectErrorMessage() {
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
        assertEquals("Checkmarx credentials are required.", descriptor.doCheckCredentialsId(null, "").getMessage());
    }

    @Test
    public void testAdditionalOptionsHandling() {
        String options = "--preset Default --test-param value";
        scanBuilder.setAdditionalOptions(options);
        scanBuilder.setUseOwnAdditionalOptions(true);
        
        String retrievedOptions = scanBuilder.getAdditionalOptions();
        assertTrue("Should contain test parameter", retrievedOptions.contains("--test-param value"));
        assertEquals("Additional options should match", options, retrievedOptions);
    }

    @Test
    public void testBranchHandling() {
        // Test with explicit branch name
        String branchName = "feature/test-branch";
        scanBuilder.setBranchName(branchName);
        assertEquals("Branch name should match", branchName, scanBuilder.getBranchName());
        
        // Test with empty branch name (should use environment variables)
        scanBuilder.setBranchName("");
        FormValidation validation = descriptor.doCheckBranchName("");
        assertEquals(FormValidation.Kind.WARNING, validation.kind);
        assertTrue(validation.getMessage().contains(CheckmarxScanBuilder.GIT_BRANCH));
    }

    @Test
    public void testServerConfiguration() {
        // Test server URL configuration
        String serverUrl = "https://checkmarx.example.com";
        descriptor.setServerUrl(serverUrl);
        assertEquals("Server URL should match", serverUrl, descriptor.getServerUrl());
        
        // Test authentication URL configuration
        String authUrl = "https://auth.example.com";
        descriptor.setBaseAuthUrl(authUrl);
        descriptor.setUseAuthenticationUrl(true);
        assertEquals("Auth URL should match", authUrl, descriptor.getBaseAuthUrl());
        assertTrue("Should use authentication URL", descriptor.getUseAuthenticationUrl());
    }
} 