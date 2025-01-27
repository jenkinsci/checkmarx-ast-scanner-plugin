package com.checkmarx.jenkins.integration;

import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import com.checkmarx.jenkins.tools.CheckmarxInstaller;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CheckmarxInstallerTest extends CheckmarxTestBase {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private CheckmarxInstaller installer;
    private TaskListener taskListener;
    private Node node;
    private CheckmarxInstallation toolInstallation;

    @Before
    public void setUp() throws Exception {
        // Create installer with specific version and update policy
        installer = new CheckmarxInstaller(null, "latest", 24L);
        
        // Mock TaskListener
        taskListener = mock(TaskListener.class);
        when(taskListener.getLogger()).thenReturn(System.out);
        
        // Create a real tool installation
        toolInstallation = new CheckmarxInstallation(
            "test-installation",
            tempFolder.getRoot().getAbsolutePath(),
            Collections.emptyList()
        );
        
        // Use the Jenkins node from the base class
        node = jenkins.getInstance();
    }

    @Test
    public void testPerformInstallationWithExistingMarker() throws Exception {
        // Create a temporary directory for the installation
        File installDir = tempFolder.newFolder("checkmarx-install");
        FilePath installPath = new FilePath(installDir);
        
        // Create marker files to simulate an existing installation
        FilePath timestampFile = installPath.child(".timestamp");
        long currentTime = System.currentTimeMillis();
        timestampFile.write(String.valueOf(currentTime), "UTF-8");
        
        FilePath installedFromFile = installPath.child(".installedFrom");
        installedFromFile.write("https://download.checkmarx.com/cli/latest", "UTF-8");
        
        // Perform the installation
        FilePath result = installer.performInstallation(toolInstallation, node, taskListener);
        
        // Verify the result
        assertNotNull("Installation result should not be null", result);
        assertTrue("Installation directory should exist", result.exists());
        assertTrue("Timestamp file should exist", timestampFile.exists());
        assertTrue("InstalledFrom file should exist", installedFromFile.exists());
        
        // Since the installation is current, the timestamp should not be updated
        assertEquals("Timestamp should not be updated for current installation", 
                    currentTime, 
                    Long.parseLong(timestampFile.readToString().trim()));
    }

} 