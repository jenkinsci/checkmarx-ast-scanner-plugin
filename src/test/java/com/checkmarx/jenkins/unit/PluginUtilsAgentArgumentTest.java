package com.checkmarx.jenkins.unit;

import com.checkmarx.jenkins.PluginUtils;
import com.checkmarx.jenkins.logger.CxLoggerAdapter;
import com.checkmarx.jenkins.model.ScanConfig;
import hudson.PluginManager;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Every CLI invocation the plugin builds must carry {@code --agent Jenkins_<plugin version>},
 * so that every API call the CLI makes on our behalf is attributable to this plugin version.
 */
public class PluginUtilsAgentArgumentTest {

    private static final String PLUGIN_VERSION = "2.0.13-853.v1fa_8405d5991";
    private static final String EXPECTED_AGENT = "Jenkins_" + PLUGIN_VERSION;
    private static final String EXECUTABLE = "cx";

    private MockedStatic<Jenkins> jenkinsStatic;
    private CxLoggerAdapter log;
    private ScanConfig scanConfig;

    @Before
    public void before() {
        PluginWrapper wrapper = mock(PluginWrapper.class);
        when(wrapper.getVersion()).thenReturn(PLUGIN_VERSION);

        PluginManager pluginManager = mock(PluginManager.class);
        when(pluginManager.whichPlugin(any())).thenReturn(wrapper);

        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.getPluginManager()).thenReturn(pluginManager);

        jenkinsStatic = mockStatic(Jenkins.class);
        jenkinsStatic.when(Jenkins::getInstanceOrNull).thenReturn(jenkins);

        log = new CxLoggerAdapter(new PrintStream(new ByteArrayOutputStream()));

        scanConfig = new ScanConfig();
        scanConfig.setServerUrl("https://example.checkmarx.net");
        scanConfig.setTenantName("test-tenant");
        scanConfig.setProjectName("test-project");
        scanConfig.setBranchName("main");
        scanConfig.setSourceDirectory("/tmp/workspace");
    }

    @After
    public void after() {
        if (jenkinsStatic != null) {
            jenkinsStatic.close();
        }
    }

    private static void assertCarriesAgent(List<String> arguments) {
        int index = arguments.indexOf("--agent");
        assertTrue("expected --agent in " + arguments, index >= 0);
        assertEquals(EXPECTED_AGENT, arguments.get(index + 1));
        assertEquals("--agent must not be passed twice", index, arguments.lastIndexOf("--agent"));
    }

    @Test
    public void scanCreateCarriesTheAgent() throws Exception {
        assertCarriesAgent(PluginUtils.submitScanDetailsToWrapper(scanConfig, EXECUTABLE, log));
    }

    @Test
    public void scanCancelCarriesTheAgent() throws Exception {
        assertCarriesAgent(PluginUtils.scanCancel(UUID.randomUUID(), scanConfig, EXECUTABLE, log));
    }

    @Test
    public void htmlReportCarriesTheAgent() throws Exception {
        assertCarriesAgent(PluginUtils.generateHTMLReport(UUID.randomUUID(), scanConfig, EXECUTABLE, log));
    }

    @Test
    public void jsonReportCarriesTheAgent() throws Exception {
        assertCarriesAgent(PluginUtils.generateJsonReport(UUID.randomUUID(), scanConfig, EXECUTABLE, log));
    }
}
