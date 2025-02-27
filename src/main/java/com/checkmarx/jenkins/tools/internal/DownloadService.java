package com.checkmarx.jenkins.tools.internal;

import com.checkmarx.jenkins.tools.Platform;
import lombok.NonNull;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static java.lang.String.format;

public class DownloadService {

    private static final String CHECKMARX_FILE_NAME = "ast-cli";
    private static final String CHECKMARX_CLI_REPO = "https://api.github.com/repos/Checkmarx/ast-cli";
    private static final String CHECKMARX_RELEASES_TAGS = CHECKMARX_CLI_REPO +  "/releases/tags/%s";
    private static final String CHECKMARX_DOWNLOAD = "https://github.com/Checkmarx/ast-cli/releases/download/%s/%s";

    private DownloadService() {
        // squid:S1118
    }

    public static URL getDownloadUrlForCli(@NonNull String version, @NonNull final Platform platform) throws IOException {
        version = "latest.".equals(version)? readCLILatestVersionFromVersionFile() : version;
        final String jsonString = DownloadService.loadJSON(format(DownloadService.CHECKMARX_RELEASES_TAGS, version));

        final JSONObject release = JSONObject.fromObject(jsonString);
        final String tagName = (String) release.get("tag_name");

        String url = format(CHECKMARX_DOWNLOAD, tagName, buildFileName(tagName, platform));
        return new URL(url);
    }

    private static String readCLILatestVersionFromVersionFile() throws IOException {
        return IOUtils.toString(Objects.requireNonNull(DownloadService.class.getResourceAsStream("/cli-latest.version")), StandardCharsets.UTF_8);
    }


    public static String buildFileName(String tagName, Platform platform) {
        if (Objects.isNull(platform)) {
            throw new IllegalArgumentException("Platform cannot be null");
        }
        if (Objects.isNull(tagName)) {
            throw new IllegalArgumentException("Tag name cannot be null");
        }
        return String.format("%s_%s_%s", CHECKMARX_FILE_NAME, tagName, platform.packageExtension);
    }

    private static String loadJSON(final String source) throws IOException {
        URL sourceUrl = new URL(source);
        return IOUtils.toString(sourceUrl, StandardCharsets.UTF_8);
    }
}
