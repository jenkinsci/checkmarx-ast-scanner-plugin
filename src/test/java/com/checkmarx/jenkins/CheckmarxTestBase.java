package com.checkmarx.jenkins;

import com.checkmarx.jenkins.credentials.DefaultCheckmarxApiToken;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.checkmarx.jenkins.tools.CheckmarxInstaller;
import com.checkmarx.jenkins.utils.Constants;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tools.InstallSourceProperty;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CheckmarxTestBase {

    public static final String TEST_CX_BASE_URI = "TEST_CX_BASE_URI";
    public static final String TEST_CX_TENANT = "TEST_CX_TENANT";
    public static final String TEST_CX_PROJECT_NAME = "TEST_CX_PROJECT_NAME";
    public static final String TEST_CX_BRANCH_NAME = "TEST_CX_BRANCH_NAME";

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

        setJenkinsEnvironmentVariables();
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

    /**
     * Defines some jenkins environment variables
     */
    private void setJenkinsEnvironmentVariables(){

        EnvironmentVariablesNodeProperty.Entry cx_base_uri = new EnvironmentVariablesNodeProperty.Entry(TEST_CX_BASE_URI, this.astServerUrl);
        EnvironmentVariablesNodeProperty cx_base_uri_var = new EnvironmentVariablesNodeProperty(cx_base_uri);
        jenkins.getInstance().getGlobalNodeProperties().add(cx_base_uri_var);

        EnvVars envVars = ((EnvironmentVariablesNodeProperty) Jenkins.get().getGlobalNodeProperties().get(0)).getEnvVars();
        envVars.put(TEST_CX_TENANT, this.astTenantName);
        envVars.put(TEST_CX_PROJECT_NAME, "jenkins_project_with_env_vars");
        envVars.put(TEST_CX_BRANCH_NAME, "master");
    }
}
