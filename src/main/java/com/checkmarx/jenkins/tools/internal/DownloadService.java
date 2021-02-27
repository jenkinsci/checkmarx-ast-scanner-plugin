package com.checkmarx.jenkins.tools.internal;

import com.checkmarx.jenkins.tools.Platform;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;

public class DownloadService {

    private static final String CHECKMARX_CLI_RELEASES_LATEST = "https://api.github.com/repos/CheckmarxDev/ast-cli/releases/latest";
    private static final String CHECKMARX_RELEASES_TAGS = "https://api.github.com/repos/CheckmarxDev/ast-cli/releases/tags/v%s";

    private DownloadService() {
        // squid:S1118
    }

    public static URL getDownloadUrlForCli(@Nonnull final String version, @Nonnull final Platform platform) throws IOException {
        final String jsonString;
        final String tagName;

        // latest version needed different url
        if ("latest".equals(version)) {
            jsonString = DownloadService.loadJSON(DownloadService.CHECKMARX_CLI_RELEASES_LATEST);
        } else {
            jsonString = DownloadService.loadJSON(format(DownloadService.CHECKMARX_RELEASES_TAGS, version));
        }
        final JSONObject release = JSONObject.fromObject(jsonString);
        tagName = (String) release.get("tag_name");
        return new URL(format("https://github.com/CheckmarxDev/ast-cli/releases/download/%s/%s", tagName, platform.checkmarxWrapperFileName));
    }

    private static String loadJSON(final String source) throws IOException {
        URL sourceUrl = new URL(source);
        return IOUtils.toString(sourceUrl, StandardCharsets.UTF_8);
    }
}
