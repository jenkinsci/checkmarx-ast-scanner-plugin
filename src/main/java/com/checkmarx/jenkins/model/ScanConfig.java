package com.checkmarx.jenkins.model;

import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ScanConfig implements Serializable {

    private String serverUrl;
    private String iamUrl;
    private CheckmarxApiToken token;
    private String projectName;
    private String teamName;
    private String presetName;
    private boolean sastEnabled;
    private boolean scaEnabled;
    private boolean incrementalScan;
    private String zipFileFilters;
    private String sastScanFilters;
    private String scaScanFilters;
    private String additionalOptions;


}
