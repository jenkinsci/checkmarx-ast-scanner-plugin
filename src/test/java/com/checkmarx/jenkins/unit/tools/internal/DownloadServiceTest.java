package com.checkmarx.jenkins.unit.tools.internal;

import com.checkmarx.jenkins.tools.CheckmarxInstaller;
import com.checkmarx.jenkins.tools.Platform;
import com.checkmarx.jenkins.tools.internal.DownloadService;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

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

    @Test
    public void testGetDownloadUrlForCli_withSpecificVersion() throws IOException {
        String version = "2.3.9";
        Platform platform = Platform.LINUX;
        URL expectedUrl = new URL("https://github.com/Checkmarx/ast-cli/releases/download/2.3.9/ast-cli_2.3.9_linux_x64.tar.gz");


        URL actualUrl = DownloadService.getDownloadUrlForCli(version, platform);
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    public void testGetDownloadUrlForCli_withLatestVersion() throws IOException {
        CheckmarxInstaller installer = new CheckmarxInstaller("test", "latest", 24L);
        Platform platform = Platform.LINUX;

        URL actualUrl = DownloadService.getDownloadUrlForCli(installer.getVersionNumber(), platform);
        assertNotNull(actualUrl);
    }

} 