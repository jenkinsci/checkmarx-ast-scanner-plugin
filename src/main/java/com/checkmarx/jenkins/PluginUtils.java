package com.checkmarx.jenkins;

import com.checkmarx.ast.scans.*;
import com.checkmarx.ast.exceptions.CxException;
import com.checkmarx.ast.results.*;
import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.model.ScanConfig;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import hudson.FilePath;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;
import static hudson.Util.fixEmptyAndTrim;
import static java.nio.charset.StandardCharsets.UTF_8;

public class PluginUtils {

    private static final String JENKINS = "Jenkins";
    private static final String RESULTS_OVERVIEW_URL = "{serverUrl}/#/projects/{projectId}/overview";
    public static final String CHECKMARX_AST_RESULTS_HTML = "checkmarx-ast-results.html";

    public static CheckmarxInstallation findCheckmarxInstallation(final String checkmarxInstallation) {
        final CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = Jenkins.get().getDescriptorByType(CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor.class);
        return Stream.of((descriptor).getInstallations())
                .filter(installation -> installation.getName().equals(checkmarxInstallation))
                .findFirst().orElse(null);
    }

    public static CheckmarxApiToken getCheckmarxTokenCredential(final Run<?, ?> run, final String credentialsId) {
        return findCredentialById(credentialsId, CheckmarxApiToken.class, run);
    }

    public static String getSourceDirectory(final FilePath workspace) {
        final File file = new File(workspace.getRemote());

        return file.getAbsolutePath();
    }

    public static CxScan submitScanDetailsToWrapper(final ScanConfig scanConfig, final String checkmarxCliExecutable, final CxLoggerAdapter log) throws IOException, InterruptedException, URISyntaxException {
        log.info("Submitting the scan details to the CLI wrapper.");

        final CxAuth wrapper = initiateWrapperObject(scanConfig, checkmarxCliExecutable, log);

        final Map<CxParamType, String> params = new HashMap<>();
        params.put(CxParamType.AGENT, PluginUtils.JENKINS);
        params.put(CxParamType.S, scanConfig.getSourceDirectory());
        params.put(CxParamType.PROJECT_NAME, scanConfig.getProjectName());

        if (StringUtils.isNotEmpty(scanConfig.getAdditionalOptions())) {
            params.put(CxParamType.ADDITIONAL_PARAMETERS, scanConfig.getAdditionalOptions());
        }

        if (StringUtils.isNotEmpty(scanConfig.getBranchName())) {
            params.put(CxParamType.BRANCH, scanConfig.getBranchName());
        }

        final CxCommandOutput cxScan = wrapper.cxScanCreate(params);

        /**
         * Return the object and pass the scan ID to generate report
         */
        log.info("--------------- Checkmarx execution completed ---------------");


        return  ((cxScan.getExitCode() == 0) && (cxScan.getScanObjectList() != null)) ? cxScan.getScanObjectList().get(0) : null;
    }

    public static String getCheckmarxResultsOverviewUrl() {
        return String.format(RESULTS_OVERVIEW_URL);
    }

    public static void generateHTMLReport(FilePath workspace, String scanId, final ScanConfig scanConfig, final String checkmarxCliExecutable, final CxLoggerAdapter log) throws IOException, InterruptedException, CxException, URISyntaxException {
        CxAuth auth = initiateWrapperObject(scanConfig, checkmarxCliExecutable, log);
        String htmlData = auth.cxGetResultsSummary(scanId, "", "");
        workspace.child(workspace.getName() + "_" + CHECKMARX_AST_RESULTS_HTML).write(htmlData, UTF_8.name());
    }

    private static CxAuth initiateWrapperObject(final ScanConfig scanConfig, final String checkmarxCliExecutable, final CxLoggerAdapter log) throws IOException, InterruptedException, CxException, URISyntaxException {
        final CxScanConfig scan = new CxScanConfig();
        scan.setBaseUri(scanConfig.getServerUrl());
        scan.setBaseAuthUri(scanConfig.getBaseAuthUrl());
        if(fixEmptyAndTrim(scanConfig.getTenantName())!= null) {
            scan.setTenant(scanConfig.getTenantName());
        }
        scan.setApiKey(scanConfig.getCheckmarxToken().getToken().getPlainText());
        scan.setPathToExecutable(checkmarxCliExecutable);

        return new CxAuth(scan, log);
    }

}
