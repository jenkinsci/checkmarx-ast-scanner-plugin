package com.checkmarx.jenkins;

import hudson.model.Run;
import jenkins.model.RunAction2;
import jodd.jerry.Jerry;
import jodd.jerry.JerryParser;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class CheckmarxScanResultsAction implements RunAction2 {
    private static final JerryParser parser = Objects.requireNonNull(Jerry.create());
    private transient Run run;

    public CheckmarxScanResultsAction(@Nonnull final Run<?, ?> run) {
        this.run = run;
    }

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

    public String getReportHtml() {
        Jerry document = getHtmlDocument();
        return document != null ? document.s("body").html() : "";
    }

    public String getReportCss() {
        Jerry document = getHtmlDocument();
        return document != null ? document.s("style").text() : "";
    }

    public String getReportScript() {
        Jerry document = getHtmlDocument();
        return document != null ? document.s("script").text() : "";
    }

    @SneakyThrows
    private Jerry getHtmlDocument() {
        for (Object artifact : run.getArtifacts()) {
            if (artifact instanceof Run.Artifact && ((Run.Artifact) artifact).getFileName().contains(PluginUtils.CHECKMARX_AST_RESULTS_HTML)) {

                byte[] encoded = Files.readAllBytes(Paths.get(((Run.Artifact) artifact).getFile().getCanonicalPath()));
                String htmlData = new String(encoded, Charset.defaultCharset());
                return CheckmarxScanResultsAction.parser.parse(htmlData);
            }
        }
        return null;
    }
}

