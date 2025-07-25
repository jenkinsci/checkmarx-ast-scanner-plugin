package com.checkmarx.jenkins;

import com.checkmarx.ast.results.ReportFormat;
import com.checkmarx.ast.wrapper.CxConfig;
import com.checkmarx.ast.wrapper.CxConstants;
import com.checkmarx.ast.wrapper.CxException;
import com.checkmarx.ast.wrapper.CxWrapper;
import com.checkmarx.jenkins.logger.CxLoggerAdapter;
import com.checkmarx.jenkins.model.ScanConfig;
import com.checkmarx.jenkins.tools.CheckmarxInstallation;
import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class PluginUtils {
    public static final String CHECKMARX_AST_RESULTS = "checkmarx-ast-results";
    public static final String CHECKMARX_AST_RESULTS_HTML = "checkmarx-ast-results.html";
    public static final String CHECKMARX_AST_RESULTS_JSON = "checkmarx-ast-results.json";
    public static final String REGEX_SCAN_ID_FROM_LOGS = "\"ID\":\"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\"";
    private static final String JENKINS = "Jenkins";
    static final String CX_CLIENT_ID_ENV_KEY = "CX_CLIENT_ID";
    static final String CX_CLIENT_SECRET_ENV_KEY = "CX_CLIENT_SECRET";
    public static final String HTTP_PROXY = "HTTP_PROXY";
    public static final String defaultOutputName = "cx_result";

    public static CheckmarxInstallation findCheckmarxInstallation(final String checkmarxInstallation) {
        final CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor descriptor = Jenkins.get().getDescriptorByType(CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor.class);
        return Stream.of((descriptor).getInstallations())
                .filter(installation -> installation.getName().equals(checkmarxInstallation))
                .findFirst().orElse(null);
    }

    public static List<String> submitScanDetailsToWrapper(final ScanConfig scanConfig, final String checkmarxCliExecutable, final CxLoggerAdapter log) throws IOException {
        log.info("Submitting the scan details to the CLI wrapper.");

        final CxConfig cxConfig = initiateWrapperObject(scanConfig, checkmarxCliExecutable);

        final Map<String, String> params = new HashMap<>();
        params.put(CxConstants.AGENT, JENKINS);
        params.put(CxConstants.SOURCE, scanConfig.getSourceDirectory());
        params.put(CxConstants.PROJECT_NAME, scanConfig.getProjectName());
        params.put(CxConstants.BRANCH, scanConfig.getBranchName());

        final CxWrapper cxWrapper = new CxWrapper(cxConfig, log);
        return cxWrapper.buildScanCreateArguments(params, scanConfig.getAdditionalOptions());
    }

    public static List<String> scanCancel(UUID scanId, final ScanConfig scanConfig, final String checkmarxCliExecutable, final CxLoggerAdapter log) throws IOException, InterruptedException {
        final CxConfig cxConfig = initiateWrapperObject(scanConfig, checkmarxCliExecutable);

        final CxWrapper cxWrapper = new CxWrapper(cxConfig, log);
        return cxWrapper.buildScanCancelArguments(scanId);
    }

    public static List<String> generateHTMLReport(UUID scanId, final ScanConfig scanConfig, final String checkmarxCliExecutable, final CxLoggerAdapter log) throws IOException {
        final CxConfig cxConfig = initiateWrapperObject(scanConfig, checkmarxCliExecutable);

        final CxWrapper cxWrapper = new CxWrapper(cxConfig, log);
        return cxWrapper.buildResultsArguments(scanId, ReportFormat.summaryHTML);
    }

    public static List<String> generateJsonReport(UUID scanId, final ScanConfig scanConfig, final String checkmarxCliExecutable, final CxLoggerAdapter log) throws IOException {
        final CxConfig cxConfig = initiateWrapperObject(scanConfig, checkmarxCliExecutable);

        final CxWrapper cxWrapper = new CxWrapper(cxConfig, log);
        return cxWrapper.buildResultsArguments(scanId, ReportFormat.summaryJSON);
    }

    public static String authValidate(final ScanConfig scanConfig, final String checkmarxCliExecutable) throws IOException, InterruptedException, CxException {
        final CxConfig cxConfig = initiateWrapperObject(scanConfig, checkmarxCliExecutable);
        cxConfig.setClientId(scanConfig.getCheckmarxToken().getClientId());
        cxConfig.setClientSecret(scanConfig.getCheckmarxToken().getToken().getPlainText());
        final CxWrapper cxWrapper = new CxWrapper(cxConfig);
        return cxWrapper.authValidate();
    }

    private static CxConfig initiateWrapperObject(final ScanConfig scanConfig, final String checkmarxCliExecutable) {
        return CxConfig.builder()
                .baseUri(scanConfig.getServerUrl())
                .baseAuthUri(scanConfig.getBaseAuthUrl())
                .tenant(scanConfig.getTenantName())
                .additionalParameters(null)
                .pathToExecutable(checkmarxCliExecutable)
                .build();
    }

    public static String getScanIdFromLogFile(String logs) {
        final String regex = REGEX_SCAN_ID_FROM_LOGS;
        final Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(logs);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static boolean isPolicyViolated(String logs) {
            // Checking if policy violation message exists in the logs
            final String policyViolationRegex = "Policy Management Violation[:\\s-].*";
            final Pattern policyPattern = Pattern.compile(policyViolationRegex);
            Matcher policyMatcher = policyPattern.matcher(logs);

            if (policyMatcher.find()) {
                // Checks if "Break Build: true"
                final String breakBuildRegex = "Break Build:\\s*true";
                final Pattern breakBuildPattern = Pattern.compile(breakBuildRegex, Pattern.CASE_INSENSITIVE);
                Matcher breakBuildMatcher = breakBuildPattern.matcher(logs);

                return breakBuildMatcher.find();
            }

            return false;
        }

    public static void insertSecretsAsEnvVars(ScanConfig scanConfig, EnvVars envVars) throws IOException, InterruptedException {
        envVars.put(CX_CLIENT_ID_ENV_KEY,scanConfig.getCheckmarxToken().getClientId());
        envVars.put(CX_CLIENT_SECRET_ENV_KEY, scanConfig.getCheckmarxToken().getToken().getPlainText());
    }
    public static String getProxy() {
        EnvVars envVars = getEnvVars();
        String httpProxyStr = envVars.get(HTTP_PROXY);
        return httpProxyStr;
    }

    private static EnvVars getEnvVars() {
        EnvironmentVariablesNodeProperty environmentVariablesNodeProperty =
                Jenkins.get().getGlobalNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        return environmentVariablesNodeProperty != null ? environmentVariablesNodeProperty.getEnvVars() : new EnvVars();
    }
}
