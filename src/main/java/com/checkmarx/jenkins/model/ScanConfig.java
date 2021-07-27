package com.checkmarx.jenkins.model;

import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ScanConfig implements Serializable {

    public static final String PROJECT_SOURCE_UPLOAD = "upload";

    private String serverUrl;
    private String baseAuthUrl;
    private String tenantName;
    private CheckmarxApiToken checkmarxToken;
    private String projectName;
    private String branchName;
    private String teamName;
    private String zipFileFilters;
    private String additionalOptions;
    private String sourceDirectory;
}
