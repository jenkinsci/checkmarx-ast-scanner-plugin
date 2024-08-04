package com.checkmarx.jenkins;

import com.checkmarx.ast.results.ResultsSummary;
import com.checkmarx.jenkins.exception.CheckmarxException;
import com.checkmarx.jenkins.tools.ProxyHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class CheckmarxScanResultsAction implements RunAction2 {

    private transient Run<?, ?> run;

    public Run getRun() {
        return run;
    }

    @Override
    public void onAttached(final Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(final Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/checkmarx-ast-scanner/images/CxIcon24x24.png";
    }

    @Override
    public String getDisplayName() {
        return "Checkmarx Scan Results";
    }

    @Override
    public String getUrlName() {
        return "scanResults";
    }

    public ResultsSummary getResultsSummary() {
        for (Object artifact : run.getArtifacts()) {
            if (artifact instanceof Run.Artifact && ((Run.Artifact) artifact).getFileName().contains(PluginUtils.CHECKMARX_AST_RESULTS_JSON)) {
                try {
                    String artifactHref = ((Run.Artifact) artifact).getHref();
                    String serverUrl = Jenkins.get().getRootUrl();
                    String fullUrl = serverUrl + run.getUrl() + "artifact/" + artifactHref;
                    OkHttpClient client = new ProxyHttpClient().getHttpClient(PluginUtils.getProxy(), 10000, 10000);
                    Request request = new Request.Builder().url(fullUrl).build();
                    Response response = client.newCall(request).execute();
                    ResponseBody responseBody = response.body();
                    InputStream stream = responseBody.byteStream();
                    String json = IOUtils.toString(stream);
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(json, ResultsSummary.class);
                } catch (IOException | URISyntaxException | CheckmarxException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}


