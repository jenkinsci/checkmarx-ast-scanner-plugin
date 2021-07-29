package com.checkmarx.jenkins;

import com.checkmarx.jenkins.utils.Constants;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.logging.Logger;

public class CheckmarxScanBuilderTest extends CheckmarxTestBase {

    private static final Logger log = Logger.getLogger(CheckmarxScanBuilderTest.class.getName());

    @Test
    public void successCheckmarxScan() throws Exception {
        log.info("successCheckmarxScan");

        final FreeStyleProject freeStyleProject = createSimpleProject("successIntegrationJenkinsScan");

        final CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setUseOwnServerCredentials(true);
        checkmarxScanBuilder.setProjectName("successIntegrationJenkinsScan");
        checkmarxScanBuilder.setServerUrl(this.astServerUrl);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setTenantName(this.astTenantName);
        checkmarxScanBuilder.setCheckmarxInstallation(Constants.JT_LATEST);
        checkmarxScanBuilder.setCredentialsId(Constants.JT_TOKEN_ID);
        checkmarxScanBuilder.setAdditionalOptions("--scan-types sast");

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void failWrongPresetCheckmarxScan() throws Exception {
        log.info("failWrongPresetCheckmarxScan");

        final FreeStyleProject freeStyleProject = createSimpleProject("failWrongPresetCheckmarxScan");

        final CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setUseOwnServerCredentials(true);
        checkmarxScanBuilder.setProjectName("failWrongPresetCheckmarxScan");
        checkmarxScanBuilder.setServerUrl(this.astServerUrl);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setTenantName(this.astTenantName);
        checkmarxScanBuilder.setCheckmarxInstallation(Constants.JT_LATEST);
        checkmarxScanBuilder.setCredentialsId(Constants.JT_TOKEN_ID);
        checkmarxScanBuilder.setAdditionalOptions("--sast-preset-name Fake");

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.FAILURE, build);
        this.jenkins.assertLogContains("Exit code from AST-CLI: 1", build);
    }

    @Test
    public void failWrongAdditionalOption() throws Exception {
        log.info("failWrongAdditionalOption");

        final FreeStyleProject freeStyleProject = createSimpleProject("failWrongAdditionalOption");

        final CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setUseOwnServerCredentials(true);
        checkmarxScanBuilder.setProjectName("failWrongAdditionalOption");
        checkmarxScanBuilder.setServerUrl(this.astServerUrl);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setTenantName(this.astTenantName);
        checkmarxScanBuilder.setCheckmarxInstallation(Constants.JT_LATEST);
        checkmarxScanBuilder.setCredentialsId(Constants.JT_TOKEN_ID);
        checkmarxScanBuilder.setAdditionalOptions("--fakeproperty");

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void failWrongCheckmarxInstallation() throws Exception {
        log.info("failWrongCheckmarxInstallation");

        final FreeStyleProject freeStyleProject = createSimpleProject("failWrongCheckmarxInstallation");

        final CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setProjectName("failWrongCheckmarxInstallation");
        checkmarxScanBuilder.setServerUrl(this.astServerUrl);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setTenantName(this.astTenantName);
        checkmarxScanBuilder.setCredentialsId(Constants.JT_TOKEN_ID);
        checkmarxScanBuilder.setUseOwnServerCredentials(true);

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.FAILURE, build);
        this.jenkins.assertLogContains("Please configure the build properly and retry.", build);
    }

    @Test
    public void failIfProjectNameNotProvided() throws Exception {
        log.info("failIfProjectNameNotProvided");

        final FreeStyleProject freeStyleProject = createSimpleProject("failIfProjectNameNotProvided");

        final CheckmarxScanBuilder checkmarxScanBuilder = new CheckmarxScanBuilder();
        checkmarxScanBuilder.setServerUrl(this.astServerUrl);
        checkmarxScanBuilder.setUseAuthenticationUrl(StringUtils.isNotEmpty(this.astBaseAuthUrl));
        checkmarxScanBuilder.setBaseAuthUrl(this.astBaseAuthUrl);
        checkmarxScanBuilder.setTenantName(this.astTenantName);
        checkmarxScanBuilder.setCheckmarxInstallation(Constants.JT_LATEST);
        checkmarxScanBuilder.setCredentialsId(Constants.JT_TOKEN_ID);
        checkmarxScanBuilder.setUseOwnServerCredentials(true);

        freeStyleProject.getBuildersList().add(checkmarxScanBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        this.jenkins.assertBuildStatus(Result.FAILURE, build);
        this.jenkins.assertLogContains("Please provide a valid project name.", build);
    }
}
