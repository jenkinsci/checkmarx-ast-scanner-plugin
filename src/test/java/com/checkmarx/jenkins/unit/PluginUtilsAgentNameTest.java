package com.checkmarx.jenkins.unit;

import com.checkmarx.jenkins.PluginUtils;
import hudson.PluginManager;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Covers the value the plugin sends to the CLI via {@code --agent}, which the CLI turns into
 * the {@code User-Agent} header of every API call it performs.
 */
public class PluginUtilsAgentNameTest {

    private MockedStatic<Jenkins> jenkinsStatic;
    private Jenkins jenkins;
    private PluginManager pluginManager;

    @Before
    public void before() {
        jenkins = mock(Jenkins.class);
        pluginManager = mock(PluginManager.class);
        when(jenkins.getPluginManager()).thenReturn(pluginManager);

        jenkinsStatic = mockStatic(Jenkins.class);
        jenkinsStatic.when(Jenkins::getInstanceOrNull).thenReturn(jenkins);
    }

    @After
    public void after() {
        if (jenkinsStatic != null) {
            jenkinsStatic.close();
        }
    }

    private void givenPluginVersion(String version) {
        PluginWrapper wrapper = mock(PluginWrapper.class);
        when(wrapper.getVersion()).thenReturn(version);
        when(pluginManager.whichPlugin(any())).thenReturn(wrapper);
    }

    @Test
    public void appendsPluginVersionToTheAgentName() {
        givenPluginVersion("2.0.13");

        assertEquals("Jenkins_2.0.13", PluginUtils.getAgentName());
    }

    @Test
    public void stripsTheBuildQualifierOfLocallyBuiltPlugins() {
        givenPluginVersion("2.0.13-SNAPSHOT (private-f979c194-dev)");

        assertEquals("Jenkins_2.0.13-SNAPSHOT", PluginUtils.getAgentName());
    }

    @Test
    public void fallsBackToPlainAgentNameWhenThePluginIsNotResolvable() {
        when(pluginManager.whichPlugin(any())).thenReturn(null);
        when(pluginManager.getPlugin("checkmarx-ast-scanner")).thenReturn(null);

        assertEquals("Jenkins", PluginUtils.getAgentName());
    }

    @Test
    public void fallsBackToPlainAgentNameWhenTheVersionIsMissing() {
        givenPluginVersion(null);

        assertEquals("Jenkins", PluginUtils.getAgentName());
    }

    @Test
    public void fallsBackToPlainAgentNameOutsideOfARunningJenkins() {
        jenkinsStatic.when(Jenkins::getInstanceOrNull).thenReturn(null);

        assertEquals("Jenkins", PluginUtils.getAgentName());
    }
}
