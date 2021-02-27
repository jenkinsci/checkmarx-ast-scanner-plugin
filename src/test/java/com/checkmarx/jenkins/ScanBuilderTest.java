package com.checkmarx.jenkins;

import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

public class ScanBuilderTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testPresenceOfPlugin() throws IOException {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList();
    }

}
