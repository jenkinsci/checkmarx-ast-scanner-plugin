package com.checkmarx.jenkins.tools;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;

/**
 * Supported platform.
 */
public enum Platform {
    LINUX("node", "npm", "bin", "cx"),
    MAC_OS("node", "npm", "bin", "cx-mac"),
    WINDOWS("node.exe", "npm.cmd", "", "cx.exe");

    public final String nodeFileName;
    public final String npmFileName;
    public final String binFolder;
    public final String checkmarxWrapperFileName;

    Platform(final String nodeFileName, final String npmFileName, final String binFolder, final String checkmarxWrapperFileName) {
        this.nodeFileName = nodeFileName;
        this.npmFileName = npmFileName;
        this.binFolder = binFolder;
        this.checkmarxWrapperFileName = checkmarxWrapperFileName;
    }

    @Nonnull
    public static Platform current() throws ToolDetectionException {
        return Platform.detect(System.getProperties());
    }

    @Nonnull
    private static Platform detect(@Nonnull final Map<Object, Object> systemProperties) throws ToolDetectionException {
        final String arch = ((String) systemProperties.get("os.name")).toLowerCase(Locale.ENGLISH);
        if (arch.contains("linux")) {
            return Platform.LINUX;
        } else if (arch.contains("mac os x") || arch.contains("darwin") || arch.contains("osx")) {
            return Platform.MAC_OS;
        } else if (arch.contains("windows")) {
            return Platform.WINDOWS;
        }
        throw new ToolDetectionException(arch + " is not supported CPU type");
    }
}