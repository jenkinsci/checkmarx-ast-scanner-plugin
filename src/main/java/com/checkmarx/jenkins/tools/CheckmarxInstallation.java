package com.checkmarx.jenkins.tools;

import com.checkmarx.jenkins.CheckmarxScanBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class CheckmarxInstallation extends ToolInstallation implements EnvironmentSpecific<CheckmarxInstallation>, NodeSpecific<CheckmarxInstallation> {

    @DataBoundConstructor
    public CheckmarxInstallation(@Nonnull final String name, @Nonnull final String home, final List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    @Override
    public CheckmarxInstallation forEnvironment(final EnvVars envVars) {
        return new CheckmarxInstallation(this.getName(), envVars.expand(this.getHome()), this.getProperties().toList());
    }

    @Override
    public CheckmarxInstallation forNode(@NonNull final Node node, final TaskListener taskListener) throws IOException, InterruptedException {
        return new CheckmarxInstallation(this.getName(), this.translateFor(node, taskListener), this.getProperties().toList());
    }


    public String getCheckmarxExecutable(@Nonnull final Launcher launcher) throws IOException, InterruptedException {
        if (this.getProperties().size() == 0) return this.getHome();

        final VirtualChannel channel = launcher.getChannel();
        return channel == null ? null : channel.call(new MasterToSlaveCallable<String, IOException>() {
            @Override
            public String call() throws IOException {
                return CheckmarxInstallation.this.resolveExecutable("cx", Platform.current());
            }
        });
    }

    private String resolveExecutable(final String file, final Platform platform) throws IOException {
        final String root = this.getHome();
        if (root == null) {
            return null;
        }
        final String wrapperFileName = platform.checkmarxWrapperFileName;
        Path executable = Paths.get(root).resolve(wrapperFileName);
        if (!executable.toFile().exists()) {
            throw new IOException(format("Could not find executable <%s>", wrapperFileName));
        }

        return executable.toAbsolutePath().toString();
    }

    @Extension
    @Symbol("checkmarx")
    public static class CheckmarxInstallationDescriptor extends ToolDescriptor<CheckmarxInstallation> {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Checkmarx";
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new CheckmarxInstaller(null, null, null));
        }

        public CheckmarxInstallation[] getInstallations() {
            final Jenkins instance = Jenkins.get();
            return instance.getDescriptorByType(CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor.class).getInstallations();
        }

        public void setInstallations(final CheckmarxInstallation... installations) {
            final Jenkins instance = Jenkins.get();
            instance.getDescriptorByType(CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor.class).setInstallations(installations);
        }
    }
}
