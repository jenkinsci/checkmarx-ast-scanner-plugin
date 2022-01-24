package com.checkmarx.jenkins;

import com.checkmarx.ast.results.ResultsSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Run;
import jenkins.model.RunAction2;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

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
                    byte[] encoded = Files.readAllBytes(Paths.get(((Run.Artifact) artifact).getFile().getCanonicalPath()));
                    String json = new String(encoded, Charset.defaultCharset());
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(json, ResultsSummary.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}


