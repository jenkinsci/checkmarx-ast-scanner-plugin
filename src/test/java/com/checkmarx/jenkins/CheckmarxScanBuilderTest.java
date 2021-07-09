package com.checkmarx.jenkins;

import com.checkmarx.jenkins.credentials.DefaultCheckmarxApiToken;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CheckmarxScanBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void freeStyleProject_shouldFail_ifNoCheckmarxInstallationExist() throws Exception {
        DefaultCheckmarxApiToken checkmarxToken = new DefaultCheckmarxApiToken(CredentialsScope.GLOBAL, "creds-id", "", "checkmarx-token");
        CredentialsProvider.lookupStores(jenkins.getInstance()).iterator().next().addCredentials(Domain.global(), checkmarxToken);

        final FreeStyleProject freeStyleProject = this.jenkins.createFreeStyleProject("freestyle-project-without-checkmarxInstallation");
        final CheckmarxScanBuilder checkmarxBuilder = new CheckmarxScanBuilder(true,
                "serverUrl", false, "", "noTenant", "projectName", "teamName", "creds-id", "zipFileFilters",
                true, false, false, false,
                false,                "additionalOptions");
        checkmarxBuilder.setCheckmarxInstallation(null);
        freeStyleProject.getBuildersList().add(checkmarxBuilder);

        final FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();

        this.jenkins.assertBuildStatus(Result.FAILURE, build);
        this.jenkins.assertLogContains("Please configure the build properly and retry.", build);
    }
}
