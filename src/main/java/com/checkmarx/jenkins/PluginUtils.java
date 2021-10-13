package com.checkmarx.jenkins;

import com.checkmarx.ast.results.ReportFormat;
import com.checkmarx.ast.scan.Scan;
import com.checkmarx.ast.wrapper.CxConfig;
import com.checkmarx.ast.wrapper.CxConstants;
import com.checkmarx.ast.wrapper.CxException;
import com.checkmarx.ast.wrapper.CxWrapper;
import com.checkmarx.jenkins.credentials.CheckmarxApiToken;
import com.checkmarx.jenkins.model.ScanConfig;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import hudson.FilePath;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;
import static java.nio.charset.StandardCharsets.UTF_8;

public class PluginUtils {

    public static final String CHECKMARX_AST_RESULTS_HTML = "checkmarx-ast-results.html";
    private static final String JENKINS = "Jenkins";

    public static CheckmarxInstallation findCheckmarxInstallation(final String checkmarxInstallation) {
        final CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = Jenkins.get().getDescriptorByType(CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor.class);
        return Stream.of((descriptor).getInstallations())
                .filter(installation -> installation.getName().equals(checkmarxInstallation))
                .findFirst().orElse(null);
    }

    public static Scan submitScanDetailsToWrapper(final ScanConfig scanConfig, final String checkmarxCliExecutable, final CxLoggerAdapter log) throws IOException, InterruptedException, URISyntaxException, CxConfig.InvalidCLIConfigException, CxException {
        log.info("Submitting the scan details to the CLI wrapper.");

        final CxConfig cxConfig = initiateWrapperObject(scanConfig, checkmarxCliExecutable);
        cxConfig.setAdditionalParameters(scanConfig.getAdditionalOptions());

        final Map<String, String> params = new HashMap<>();
        params.put(CxConstants.AGENT, PluginUtils.JENKINS);
        params.put(CxConstants.SOURCE, scanConfig.getSourceDirectory());
        params.put(CxConstants.PROJECT_NAME, scanConfig.getProjectName());
        params.put(CxConstants.BRANCH, scanConfig.getBranchName());

        final CxWrapper cxWrapper = new CxWrapper(cxConfig, log);
        return cxWrapper.scanCreate(params, scanConfig.getAdditionalOptions());
    }

    public static void generateHTMLReport(FilePath workspace, UUID scanId, final ScanConfig scanConfig, final String checkmarxCliExecutable, final CxLoggerAdapter log) throws IOException, InterruptedException, CxException, URISyntaxException, CxConfig.InvalidCLIConfigException {
        final CxConfig cxConfig = initiateWrapperObject(scanConfig, checkmarxCliExecutable);

        final CxWrapper cxWrapper = new CxWrapper(cxConfig, log);
        final String summaryHtml = cxWrapper.results(scanId, ReportFormat.summaryHTML);
        workspace.child(workspace.getName() + "_" + CHECKMARX_AST_RESULTS_HTML).write(summaryHtml, UTF_8.name());
    }

    public static String authValidate(final ScanConfig scanConfig, final String checkmarxCliExecutable) throws IOException, InterruptedException, CxConfig.InvalidCLIConfigException, URISyntaxException, CxException {
        final CxConfig cxConfig = initiateWrapperObject(scanConfig, checkmarxCliExecutable);

        final CxWrapper cxWrapper = new CxWrapper(cxConfig);
        return cxWrapper.authValidate();
    }

    private static CxConfig initiateWrapperObject(final ScanConfig scanConfig, final String checkmarxCliExecutable) throws IOException, InterruptedException {
        return CxConfig.builder()
                .baseUri(scanConfig.getServerUrl())
                .baseAuthUri(scanConfig.getBaseAuthUrl())
                .clientId(scanConfig.getCheckmarxToken().getClientId())
                .clientSecret(scanConfig.getCheckmarxToken().getToken().getPlainText())
                .tenant(scanConfig.getTenantName())
                .additionalParameters(null)
                .pathToExecutable(checkmarxCliExecutable)
                .build();
    }
}
