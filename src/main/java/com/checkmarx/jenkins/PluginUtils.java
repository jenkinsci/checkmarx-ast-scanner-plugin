package com.checkmarx.jenkins;

import com.checkmarx.ast.*;
import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.model.ScanConfig;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;

public class PluginUtils {

    public static CheckmarxInstallation findCheckmarxInstallation(String checkmarxInstallation) {
        CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = Jenkins.get().getDescriptorByType(CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor.class);
        return Stream.of((descriptor).getInstallations())
                .filter(installation -> installation.getName().equals(checkmarxInstallation))
                .findFirst().orElse(null);
    }

    public static CheckmarxApiToken getCheckmarxTokenCredential (Run < ?, ?>run, String credentialsId){
        return findCredentialById(credentialsId, CheckmarxApiToken.class, run);
    }

    public static String getSourceDirectory (FilePath workspace){
        File file = new File(workspace.getRemote());
        return file.getAbsolutePath();
    }

    public static void submitScanDetailsToWrapper(ScanConfig scanConfig, String checkmarxCliExecutable, CxLoggerAdapter log) throws IOException, InterruptedException, URISyntaxException {

        log.info("Submitting the scan details to the CLI wrapper.");
        CxScanConfig scan = new CxScanConfig();
        scan.setBaseuri(scanConfig.getServerUrl());
        scan.setAuthType(CxAuthType.KEYSECRET);
        scan.setKey(scanConfig.getCheckmarxToken().getId());
        scan.setSecret(scanConfig.getCheckmarxToken().getToken().getPlainText());
        scan.setPathToExecutable(checkmarxCliExecutable);
        CxAuth wrapper = new CxAuth(scan, log);

        Map<CxParamType, String> params = new HashMap<>();
        params.put(CxParamType.D, scanConfig.getSourceDirectory());
        params.put(CxParamType.V, "");
        params.put(CxParamType.PROJECT_NAME, scanConfig.getProjectName());
        params.put(CxParamType.FILTER, scanConfig.getZipFileFilters());
        params.put(CxParamType.ADDITIONAL_PARAMETERS, scanConfig.getAdditionalOptions());
        params.put(CxParamType.PROJECT_TYPE, getScanType(scanConfig,log));

        CxScan cxScan = wrapper.cxScanCreate(params);

        log.info(cxScan.toString());
        log.info("--------------- Checkmarx execution completed ---------------");
    }

    private static String getScanType(ScanConfig scanConfig, CxLoggerAdapter log) {

        String scanType = "";
        ArrayList<String> scannerList = getEnabledScannersList(scanConfig,log);

        for(String item : scannerList)
        {
            scanType = scanType.concat(item).concat(" ");
        }
        scanType = scanType.trim();
        scanType = scanType.replace(" ", ",");
        return scanType;

    }

    public static ArrayList<String> getEnabledScannersList(ScanConfig scanConfig, CxLoggerAdapter log) {

        ArrayList<String> scannerList = new ArrayList<String>();

        if(scanConfig.isScaEnabled())
        {
            scannerList.add(ScanConfig.SCA_SCAN_TYPE);
        }
        if(scanConfig.isSastEnabled())
        {
            scannerList.add(ScanConfig.SAST_SCAN_TYPE);
        }
        if(scanConfig.isContainerScanEnabled())
        {
            log.error("Container Scan is not yet supported.");
        }
        if(scanConfig.isKicsEnabled())
        {
            scannerList.add(ScanConfig.KICS_SCAN_TYPE);
        }
        return scannerList;
    }

}
