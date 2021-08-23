package com.checkmarx.jenkins;

import com.checkmarx.jenkins.credentials.DefaultCheckmarxApiToken;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.checkmarx.jenkins.tools.CheckmarxInstaller;
import com.checkmarx.jenkins.utils.Constants;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.FreeStyleProject;
import hudson.tools.InstallSourceProperty;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CheckmarxTestBase {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    protected String astServerUrl;
    protected String astBaseAuthUrl;
    protected String astTenantName;
    protected String astClientSecret;
    protected String astClientId;

    @Before
    public void before() throws IOException {
        this.astServerUrl =System.getenv(Constants.JT_AST_SERVER_URL);
        this.astBaseAuthUrl = System.getenv(Constants.JT_AST_BASE_AUTH_URL);
        this.astTenantName = System.getenv(Constants.JT_AST_TENANT_NAME);
        this.astClientSecret = System.getenv(Constants.JT_AST_CLIENT_SECRET);
        this.astClientId = System.getenv(Constants.JT_AST_CLIENT_ID);

        createInstallation();
        createCredentials();
    }

    protected FreeStyleProject createSimpleProject(String projectName) throws IOException {
        final FreeStyleProject project = jenkins.createFreeStyleProject(projectName);
        project.setScm(new SingleFileSCM("greeting.txt", "hello"));
        project.setScm(new SingleFileSCM("greeting.py", "test"));
        return project;
    }

    protected void createCredentials() throws IOException {
        DefaultCheckmarxApiToken checkmarxToken =
                new DefaultCheckmarxApiToken(CredentialsScope.GLOBAL, Constants.JT_TOKEN_ID, Constants.JT_TOKEN_ID, this.astClientId, this.astClientSecret);
        CredentialsProvider.lookupStores(jenkins.getInstance()).iterator().next().addCredentials(Domain.global(), checkmarxToken);
    }

    protected void createInstallation() throws IOException {
        CheckmarxInstaller checkmarxInstaller = new CheckmarxInstaller(null, Constants.JT_LATEST, 24L);

        List<CheckmarxInstaller> checkmarxInstallers = new ArrayList<>();
        checkmarxInstallers.add(checkmarxInstaller);

        InstallSourceProperty installSourceProperty =
                new InstallSourceProperty(checkmarxInstallers);


        List<InstallSourceProperty> properties = new ArrayList<>();
        properties.add(installSourceProperty);

        CheckmarxInstallation expected = new CheckmarxInstallation(Constants.JT_LATEST, "", properties);

        jenkins.getInstance()
                .getDescriptorByType(CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor.class)
                .setInstallations(expected);
    }
}
