package com.checkmarx.jenkins.model;

import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.Serializable;

@Getter
@ToString
@EqualsAndHashCode
@Builder
public class ScanConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constant indicating upload of project sources
     */
    public static final String PROJECT_SOURCE_UPLOAD = "upload";

    @NonNull
    private final String serverUrl;

    @NonNull
    private final String authenticationBaseUrl;

    private final String tenantName;

    @NonNull
    private final CheckmarxApiToken apiToken;

    @NonNull
    private final String projectName;

    private final String branchName;

    private final String additionalOptions;

    @NonNull
    private final String sourceDirectory;
}
