package com.checkmarx.jenkins;

import hudson.model.Run;
import jenkins.model.RunAction2;
import jenkins.util.VirtualFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

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

    @SuppressWarnings("unused")
    public String getArtifactContent() {
        VirtualFile artifact = getArtifact();
        return readFile(artifact);
    }

    private VirtualFile getArtifact() {
        return run.getArtifacts().stream()
                .filter(a -> a.getFileName().contains(PluginUtils.CHECKMARX_AST_RESULTS_HTML))
                .map(a -> run.getArtifactManager().root().child(a.relativePath))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find artifact."));
    }

    private String readFile(VirtualFile file) {
        try (
                InputStream is = file.open();
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr)
        ) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

