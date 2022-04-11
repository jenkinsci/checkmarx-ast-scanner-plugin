package com.checkmarx.jenkins;

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;

import java.util.logging.Logger;

public class CheckmarxScanPipelineTest extends CheckmarxTestBase {

    private static final Logger log = Logger.getLogger(CheckmarxScanBuilderTest.class.getName());

    @Test
    public void doSuccessCheckmarxPipelineScan() throws Exception {
        log.info("successCheckmarxPipelineScan");

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "  writeFile file: 'source.py', text: 'overwrite me' \n" +
                "  checkmarxASTScanner additionalOptions: '--scan-types sast',  branchName: 'main', useOwnAdditionalOptions: true, useOwnServerCredentials: true, baseAuthUrl: '" + this.astBaseAuthUrl + "', checkmarxInstallation: '" + CheckmarxTestBase.JT_LATEST + "', credentialsId: '" + CheckmarxTestBase.JENKINS_CREDENTIALS_TOKEN_ID + "', projectName: 'successIntegrationJenkinsScan', serverUrl: '" + this.astServerUrl + "', tenantName: '" + this.astTenantName +
                "'}", true));

        WorkflowRun workflowRun = project.scheduleBuild2(0).waitForStart();
        jenkins.waitForCompletion(workflowRun);
        jenkins.assertBuildStatus(Result.SUCCESS, workflowRun);
    }

    @Test
    public void doFailWhenUseOwnServerCredentialButNotConfigured() throws Exception {
        log.info("doFailWhenUseOwnServerCredentialButNotConfigured");

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "  writeFile file: 'source.py', text: 'overwrite me' \n" +
                "  checkmarxASTScanner additionalOptions: '--scan-types sast', branchName: 'main', useOwnAdditionalOptions: true, useOwnServerCredentials: false, checkmarxInstallation: '" + CheckmarxTestBase.JT_LATEST + "',  credentialsId: '" + CheckmarxTestBase.JENKINS_CREDENTIALS_TOKEN_ID + "', projectName: 'doFailWhenUseOwnServerCredentialButNotConfigured', serverUrl: '" + this.astServerUrl + "', tenantName: '" + this.astTenantName +
                "'}", true));

        WorkflowRun workflowRun = project.scheduleBuild2(0).waitForStart();
        jenkins.waitForCompletion(workflowRun);
        jenkins.assertBuildStatus(Result.FAILURE, workflowRun);
        jenkins.assertLogContains("Please setup the server url in the global settings.", workflowRun);
    }
}
