package com.checkmarx.jenkins.unit.tools;

import com.checkmarx.jenkins.tools.CheckmarxInstaller;
import org.junit.Test;

import static org.junit.Assert.*;

public class CheckmarxInstallerTest {

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
} 