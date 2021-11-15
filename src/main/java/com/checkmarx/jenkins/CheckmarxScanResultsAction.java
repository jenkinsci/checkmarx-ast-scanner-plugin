package com.checkmarx.jenkins;

import hudson.model.Run;
import hudson.util.Secret;
import jenkins.model.RunAction2;

public class CheckmarxScanResultsAction implements RunAction2 {

    private final String content;

    public CheckmarxScanResultsAction(Secret content) {
        this.content = content.getEncryptedValue();
    }

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
        Secret decryptedValue = Secret.decrypt(content);
        return decryptedValue != null ? decryptedValue.getPlainText() : "";
    }
}

