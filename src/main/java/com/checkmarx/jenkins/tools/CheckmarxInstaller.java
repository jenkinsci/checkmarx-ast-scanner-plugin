package com.checkmarx.jenkins.tools;

import com.checkmarx.jenkins.CxLoggerAdapter;
import com.checkmarx.jenkins.tools.internal.DownloadService;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.ArgumentListBuilder;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static hudson.Util.fixEmptyAndTrim;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;


public class CheckmarxInstaller extends ToolInstaller {

    private static final String INSTALLED_FROM = ".installedFrom";
    private static final String TIMESTAMP_FILE = ".timestamp";
    private final String version;
    private final Long updatePolicyIntervalHours;
    private CxLoggerAdapter log;

    @DataBoundConstructor
    public CheckmarxInstaller(final String label, final String version, final Long updatePolicyIntervalHours) {
        super(label);
        this.version = version;
        this.updatePolicyIntervalHours = updatePolicyIntervalHours;
    }

    @Override
    public FilePath performInstallation(final ToolInstallation toolInstallation, final Node node, final TaskListener taskListener) throws IOException, InterruptedException {
        this.log = new CxLoggerAdapter(taskListener.getLogger());

        final FilePath expected = this.preferredLocation(this.tool, node);

        if (this.isUpToDate(expected, this.log)) {
            this.log.info("Checkmarx installation is UP-TO-DATE");
            return expected;
        }
        this.log.info("Installing Checkmarx AST CLI tool (version '" + fixEmptyAndTrim(this.version) + "')");
        if (this.isNpmAvailable(node, taskListener)) {
            return this.installCheckmarxCliAsNpmPackage(expected, node, taskListener);
        } else {
            return this.installCheckmarxCliAsSingleBinary(expected, node, taskListener);
        }
    }

    private boolean isUpToDate(final FilePath expectedLocation, final CxLoggerAdapter log) throws IOException, InterruptedException {
        final FilePath marker = expectedLocation.child(CheckmarxInstaller.TIMESTAMP_FILE);
        if (!marker.exists()) {
            return false;
        }

        final String content = StringUtils.chomp(marker.readToString());
        long timestampFromFile;
        try {
            timestampFromFile = Long.parseLong(content);
        } catch (final NumberFormatException ex) {
            // corrupt of modified .timestamp file => force new installation
            log.error(".timestamp file is corrupt and cannot be read and will be reset to 0.");
            timestampFromFile = 0;
        }
        final long timestampNow = Instant.now().toEpochMilli();

        final long timestampDifference = timestampNow - timestampFromFile;
        if (timestampDifference <= 0) {
            return true;
        }
        final long updateInterval = TimeUnit.HOURS.toMillis(this.updatePolicyIntervalHours);
        return timestampDifference < updateInterval;
    }

    private boolean isNpmAvailable(final Node node, final TaskListener log) {
        final Launcher launcher = node.createLauncher(log);
        final Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.quiet(true).cmds("npm", "--version");

        try {
            final int exitCode = launcher.launch(ps).join();
            return exitCode == 0;
        } catch (final Exception ex) {
            log.getLogger().println("NPM not available.");
            //     LOG.info("NPM is not available on the node: '{}'", node.getDisplayName());
            //     LOG.debug("'npm --version' command failed", ex);
            return false;
        }
    }

    private FilePath installCheckmarxCliAsNpmPackage(final FilePath expected, final Node node, final TaskListener log) throws ToolDetectionException {
        // LOG.info("Install Checkmarx CLI version '{}' as NPM package on node '{}'", version, node.getDisplayName());

        final ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("npm", "install", "--prefix", expected.getRemote(), "checkmarx@" + fixEmptyAndTrim(this.version));
        final Launcher launcher = node.createLauncher(log);
        final Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.quiet(true).cmds(args);

        try {
            final int exitCode = launcher.launch(ps).join();
            if (exitCode != 0) {
                log.getLogger().println("Checkmarx installation was not successful. Exit code: " + exitCode);
                return expected;
            }
            expected.child(CheckmarxInstaller.TIMESTAMP_FILE).write(valueOf(Instant.now().toEpochMilli()), UTF_8.name());
        } catch (final Exception ex) {
            log.getLogger().println("Checkmarx AST CLI could not installed: " + ex.getMessage());
            throw new ToolDetectionException("Could not install Checkmarx CLI with npm", ex);
        }
        return expected;
    }

    private FilePath installCheckmarxCliAsSingleBinary(final FilePath expected, final Node node, final TaskListener log) throws IOException, InterruptedException {

        //   LOG.info("Install Checkmarx version '{}' as single binary on node '{}'", version, node.getDisplayName());

        VirtualChannel nodeChannel = node.getChannel();
        if (nodeChannel == null) {
            throw new IOException(format("Node '%s' is offline", node.getDisplayName()));
        }

        final Platform platform = nodeChannel.call(new GetPlatform(node.getDisplayName()));

        try {
            final URL checkmarxDownloadUrl = DownloadService.getDownloadUrlForCli(this.version, platform);

            expected.mkdirs();
            nodeChannel.call(new Downloader(checkmarxDownloadUrl, expected.child(platform.checkmarxWrapperFileName)));

            expected.child(CheckmarxInstaller.INSTALLED_FROM).write(checkmarxDownloadUrl.toString(), UTF_8.name());
            expected.child(CheckmarxInstaller.TIMESTAMP_FILE).write(valueOf(Instant.now().toEpochMilli()), UTF_8.name());
        } catch (final Exception ex) {
            log.getLogger().println("Checkmarx Security tool could not installed: " + ex.getMessage());
            throw new ToolDetectionException("Could not install Checkmarx CLI from binary", ex);
        }

        return expected;
    }

    @SuppressWarnings("unused")
    public String getVersion() {
        return this.version;
    }

    @SuppressWarnings("unused")
    public Long getUpdatePolicyIntervalHours() {
        return this.updatePolicyIntervalHours;
    }

    @Extension
    public static final class CheckmarxInstallerDescriptor extends ToolInstallerDescriptor<CheckmarxInstaller> {

        @Override
        public String getDisplayName() {
            return "Install from checkmarx.com";
        }

        @Override
        public boolean isApplicable(final Class<? extends ToolInstallation> toolType) {
            return toolType == CheckmarxInstallation.class;
        }
    }

    private static class GetPlatform extends MasterToSlaveCallable<Platform, IOException> {
        private static final long serialVersionUID = 1L;

        private final String nodeDisplayName;

        GetPlatform(final String nodeDisplayName) {
            this.nodeDisplayName = nodeDisplayName;
        }

        @Override
        public Platform call() throws IOException {
            try {
                return Platform.current();
            } catch (final ToolDetectionException ex) {
                throw new IOException(format("Could not determine platform on node %s", this.nodeDisplayName));
            }
        }
    }

    private static class Downloader extends MasterToSlaveCallable<Void, IOException> {
        private static final long serialVersionUID = 1L;

        private final URL downloadUrl;
        private final FilePath output;

        Downloader(final URL downloadUrl, final FilePath output) {
            this.downloadUrl = downloadUrl;
            this.output = output;
        }

        @Override
        public Void call() throws IOException {
            File downloadedFile = new File(this.output.getRemote());
            FileUtils.copyURLToFile(this.downloadUrl, downloadedFile, 10000, 10000);
            // set execute permission
            if (!Functions.isWindows() && downloadedFile.isFile()) {
                final boolean result = downloadedFile.setExecutable(true, false);
                if (!result) {
                    throw new IOException(format("Could not set executable flag for the file: %s", downloadedFile.getAbsolutePath()));
                }
            }
            return null;
        }
    }
}
