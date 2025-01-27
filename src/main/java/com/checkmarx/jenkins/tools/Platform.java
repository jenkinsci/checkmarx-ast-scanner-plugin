package com.checkmarx.jenkins.tools;

import com.checkmarx.jenkins.exception.ToolDetectionException;
import lombok.NonNull;
import java.util.Locale;
import java.util.Map;

public enum Platform {
    LINUX( "linux_x64.tar.gz", "cx"),
    MAC_OS( "darwin_x64.tar.gz", "cx"),
    WINDOWS( "windows_x64.zip", "cx.exe");

    public final String packageExtension;
    public final String checkmarxWrapperFileName;

    Platform(final String packageExtension, final String checkmarxWrapperFileName) {
        this.packageExtension = packageExtension;
        this.checkmarxWrapperFileName = checkmarxWrapperFileName;
    }

    @NonNull
    public static Platform current() throws ToolDetectionException {
        return Platform.detect(System.getProperties());
    }

    @NonNull
    private static Platform detect(@NonNull final Map<Object, Object> systemProperties) throws ToolDetectionException {
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