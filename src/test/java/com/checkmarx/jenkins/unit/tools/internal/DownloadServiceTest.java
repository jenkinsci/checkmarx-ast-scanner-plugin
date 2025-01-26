package com.checkmarx.jenkins.unit.tools.internal;

import com.checkmarx.jenkins.tools.Platform;
import com.checkmarx.jenkins.tools.internal.DownloadService;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class DownloadServiceTest {

    @Test
    public void testBuildFileName() {
        String tagName = "v2.0.0";
        Platform platform = Platform.LINUX;
        String expectedFileName = String.format("%s_%s_%s", "ast-cli", tagName, platform.packageExtension);
        
        String actualFileName = DownloadService.buildFileName(tagName, platform);
        assertEquals(expectedFileName, actualFileName);
    }

    @Test
    public void testBuildFileNameForWindows() {
        String tagName = "v2.0.0";
        Platform platform = Platform.WINDOWS;
        String expectedFileName = String.format("%s_%s_%s", "ast-cli", tagName, platform.packageExtension);
        
        String actualFileName = DownloadService.buildFileName(tagName, platform);
        assertEquals(expectedFileName, actualFileName);
    }

    @Test
    public void testBuildFileNameForMac() {
        String tagName = "v2.0.0";
        Platform platform = Platform.MAC_OS;
        String expectedFileName = String.format("%s_%s_%s", "ast-cli", tagName, platform.packageExtension);
        
        String actualFileName = DownloadService.buildFileName(tagName, platform);
        assertEquals(expectedFileName, actualFileName);
    }

    @Test(expected = Exception.class)
    public void testBuildFileNameWithNullTagName() {
        DownloadService.buildFileName(null, Platform.LINUX);
    }

    @Test(expected = Exception.class)
    public void testBuildFileNameWithNullPlatform() {
        DownloadService.buildFileName("v2.0.0", null);
    }

    @Test(expected = Exception.class)
    public void testGetDownloadUrlForCliWithNullVersion() throws IOException {
        DownloadService.getDownloadUrlForCli(null, Platform.LINUX);
    }

    @Test(expected = Exception.class)
    public void testGetDownloadUrlForCliWithNullPlatform() throws IOException {
        DownloadService.getDownloadUrlForCli("v2.0.0", null);
    }
} 