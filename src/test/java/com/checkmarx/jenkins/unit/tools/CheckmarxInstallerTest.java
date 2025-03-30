package com.checkmarx.jenkins.unit.tools;

import com.checkmarx.jenkins.PluginUtils;
import com.checkmarx.jenkins.tools.CheckmarxInstaller;
import com.checkmarx.jenkins.tools.Platform;
import com.checkmarx.jenkins.tools.internal.DownloadService;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CheckmarxInstallerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private Node node;

    @Mock
    private TaskListener taskListener;

    @Mock
    private VirtualChannel virtualChannel;

    @Mock
    private PrintStream logger;

    @Mock
    private ToolInstallation toolInstallation;

    private FilePath expectedPath;
    private CheckmarxInstaller installer;

    private MockedStatic<PluginUtils> pluginUtils;
    private MockedStatic<DownloadService> downloadService;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        File tempDir = tempFolder.newFolder();
        expectedPath = new FilePath(tempDir);
        installer = new CheckmarxInstaller("test", "v1.0.0", 24L);

        pluginUtils = mockStatic(PluginUtils.class);
        downloadService = mockStatic(DownloadService.class);

        when(node.getChannel()).thenReturn(virtualChannel);
        when(node.getDisplayName()).thenReturn("test-node");
        when(taskListener.getLogger()).thenReturn(logger);
    }

    @After
    public void tearDown() {
        if (downloadService != null) {
            downloadService.close();
        }
        if (pluginUtils != null) {
            pluginUtils.close();
        }
    }

    @Test
    public void testGetVersionAndUpdateInterval() {
        String version = "1.0.0";
        Long updateInterval = 24L;
        CheckmarxInstaller installer = new CheckmarxInstaller("test", version, updateInterval);

        assertEquals(version, installer.getVersion());
        assertEquals(updateInterval, installer.getUpdatePolicyIntervalHours());
    }

    @Test
    public void testInstallerDescriptorDisplayName() {
        CheckmarxInstaller.CheckmarxInstallerDescriptor descriptor = new CheckmarxInstaller.CheckmarxInstallerDescriptor();
        assertEquals("Install from checkmarx.com", descriptor.getDisplayName());
    }

    @Test
    public void testConstructorWithNullValues() {
        CheckmarxInstaller installer = new CheckmarxInstaller(null, null, null);
        assertNull(installer.getVersion());
        assertNull(installer.getUpdatePolicyIntervalHours());
    }

    @Test
    public void testConstructorWithEmptyLabel() {
        CheckmarxInstaller installer = new CheckmarxInstaller("", "1.0.0", 24L);
        assertEquals("1.0.0", installer.getVersion());
        assertEquals(Long.valueOf(24L), installer.getUpdatePolicyIntervalHours());
    }

    @Test
    public void testConstructorWithLatestVersion() {
        CheckmarxInstaller installer = new CheckmarxInstaller("", "latest ", 24L);
        assertNotEquals("latest ", installer.getVersion());
        assertEquals(Long.valueOf(24L), installer.getUpdatePolicyIntervalHours());
    }

    @Test(expected = Exception.class)
    public void testInstallCheckmarxCliWhenNodeIsOffline() throws IOException, InterruptedException {
        when(node.getChannel()).thenReturn(null);
        installer.performInstallation(null, node, taskListener);
    }

    @Test
    public void testUpToDateInstallation() throws IOException, InterruptedException {
        // Create timestamp file indicating recent installation
        when(node.getRootPath()).thenReturn(expectedPath);
        FilePath timestampFile = expectedPath.child(".timestamp");
        timestampFile.write(String.valueOf(Instant.now().toEpochMilli()), "UTF-8");

        when(toolInstallation.getHome()).thenReturn(expectedPath.getRemote());

        FilePath result = installer.performInstallation(toolInstallation, node, taskListener);

        assertNotNull(result);
        assertEquals(expectedPath.getRemote(), result.getRemote());
        verify(virtualChannel, never()).call(any());
    }

    @Test
    public void testSuccessfulInstallation() throws IOException, InterruptedException {
        when(toolInstallation.getHome()).thenReturn(expectedPath.getRemote());
        when(node.getRootPath()).thenReturn(expectedPath);
        doReturn(Platform.LINUX).when(virtualChannel).call(any());
        pluginUtils.when(PluginUtils::getProxy).thenReturn("");
        downloadService.when(() -> DownloadService.getDownloadUrlForCli(any(), any()))
                .thenReturn(new java.net.URL("http://example.com"));
        downloadService.when(() -> DownloadService.buildFileName(any(), any())).thenCallRealMethod();

        FilePath result = installer.performInstallation(toolInstallation, node, taskListener);

        assertNotNull(result);
        assertEquals(expectedPath.getRemote(), result.getRemote());
        assertTrue(new File(expectedPath.child(".timestamp").getRemote()).exists());
        assertTrue(new File(expectedPath.child(".installedFrom").getRemote()).exists());
    }

    @Test(expected = RuntimeException.class)
    public void testInstallationFailure() throws IOException, InterruptedException {
        when(toolInstallation.getHome()).thenReturn(expectedPath.getRemote());
        doThrow(new IOException("Download failed")).when(virtualChannel).call(any());

        installer.performInstallation(toolInstallation, node, taskListener);
    }

    @Test
    public void testDescriptor() {
        CheckmarxInstaller.CheckmarxInstallerDescriptor descriptor =
                new CheckmarxInstaller.CheckmarxInstallerDescriptor();

        assertEquals("Install from checkmarx.com", descriptor.getDisplayName());
    }
}
