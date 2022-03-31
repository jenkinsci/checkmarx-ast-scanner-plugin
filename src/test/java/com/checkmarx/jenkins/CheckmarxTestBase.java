package com.checkmarx.jenkins;

import com.checkmarx.jenkins.credentials.DefaultCheckmarxApiToken;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.checkmarx.jenkins.tools.CheckmarxInstaller;
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

    public static final String CX_BASE_URI = "CX_BASE_URI";
    public static final String CX_BASE_AUTH_URI = "CX_BASE_AUTH_URI";
    public static final String CX_TENANT = "CX_TENANT";
    public static final String CX_CLIENT_SECRET = "CX_CLIENT_SECRET";
    public static final String CX_CLIENT_ID = "CX_CLIENT_ID";
    public static final String CX_PROJECT_NAME = "CX_PROJECT_NAME";
    public static final String CX_BRANCH = "CX_BRANCH";
    public static final String JENKINS_CREDENTIALS_TOKEN_ID = "JENKINS_CREDENTIALS_TOKEN_ID";
    public static final String JT_LATEST = "latest";
    public static final String BRANCH_MAIN = "main";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    protected String astServerUrl;
    protected String astBaseAuthUrl;
    protected String astTenantName;
    protected String astClientSecret;
    protected String astClientId;

    @Before
    public void before() throws IOException {
        this.astServerUrl = System.getenv(CX_BASE_URI);
        this.astBaseAuthUrl = System.getenv(CX_BASE_AUTH_URI);
        this.astTenantName = System.getenv(CX_TENANT);
        this.astClientSecret = System.getenv(CX_CLIENT_SECRET);
        this.astClientId = System.getenv(CX_CLIENT_ID);

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
                new DefaultCheckmarxApiToken(CredentialsScope.GLOBAL, JENKINS_CREDENTIALS_TOKEN_ID, JENKINS_CREDENTIALS_TOKEN_ID, this.astClientId, this.astClientSecret);
        CredentialsProvider.lookupStores(jenkins.getInstance()).iterator().next().addCredentials(Domain.global(), checkmarxToken);
    }

    protected void createInstallation() throws IOException {
        CheckmarxInstaller checkmarxInstaller = new CheckmarxInstaller(null, JT_LATEST, 24L);

        List<CheckmarxInstaller> checkmarxInstallers = new ArrayList<>();
        checkmarxInstallers.add(checkmarxInstaller);

        InstallSourceProperty installSourceProperty =
                new InstallSourceProperty(checkmarxInstallers);


        List<InstallSourceProperty> properties = new ArrayList<>();
        properties.add(installSourceProperty);

        CheckmarxInstallation expected = new CheckmarxInstallation(JT_LATEST, "", properties);

        jenkins.getInstance()
                .getDescriptorByType(CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor.class)
                .setInstallations(expected);
    }

    /**
     * Defines some jenkins environment variables
     */
    private void setJenkinsEnvironmentVariables() {

        EnvironmentVariablesNodeProperty.Entry cx_base_uri = new EnvironmentVariablesNodeProperty.Entry(CX_BASE_URI, this.astServerUrl);
        EnvironmentVariablesNodeProperty cx_base_uri_var = new EnvironmentVariablesNodeProperty(cx_base_uri);
        jenkins.getInstance().getGlobalNodeProperties().add(cx_base_uri_var);

        EnvVars envVars = ((EnvironmentVariablesNodeProperty) Jenkins.get().getGlobalNodeProperties().get(0)).getEnvVars();
        envVars.put(CX_TENANT, this.astTenantName);
        envVars.put(CX_PROJECT_NAME, "jenkins_project_with_env_vars");
        envVars.put(CX_BRANCH, BRANCH_MAIN);
    }
}
