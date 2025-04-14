package com.checkmarx.jenkins.tools;

import com.checkmarx.jenkins.exception.ToolDetectionException;
import com.checkmarx.jenkins.logger.CxLoggerAdapter;
import com.checkmarx.jenkins.PluginUtils;
import com.checkmarx.jenkins.exception.CheckmarxException;
import com.checkmarx.jenkins.tools.internal.DownloadService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import jenkins.security.MasterToSlaveCallable;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static hudson.Util.fixEmptyAndTrim;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;


public class CheckmarxInstaller extends ToolInstaller {

    private static final String INSTALLED_FROM = ".installedFrom";
    private static final String TIMESTAMP_FILE = ".timestamp";
    public static final String cliDefaultVersion = "2.3.18";
    private static final String cliVersionFileName = "cli.version";
    @Getter
    private String version;
    @Getter
    private final Long updatePolicyIntervalHours;
    private CxLoggerAdapter log;

    @DataBoundConstructor
    public CheckmarxInstaller(String label, String version, Long updatePolicyIntervalHours) {
        super(label);
        this.version = version;
        this.updatePolicyIntervalHours = updatePolicyIntervalHours;
    }

    @Override
    public FilePath performInstallation(ToolInstallation toolInstallation, Node node, TaskListener taskListener) throws IOException, InterruptedException {
        log = new CxLoggerAdapter(taskListener.getLogger());
        String versionToInstall;
        FilePath expected = preferredLocation(toolInstallation, node);

        if (isUpToDate(expected, log)) {
            log.info("Checkmarx installation is UP-TO-DATE");
            return expected;
        }

        if ("latest".equalsIgnoreCase(version.trim()) || version.isEmpty()) {
            versionToInstall = readCLILatestVersionFromVersionFile();
        } else versionToInstall = version;

        log.info("Installing Checkmarx AST CLI tool (version '" + fixEmptyAndTrim(version) + "')");

        return installCheckmarxCliAsSingleBinary(versionToInstall, expected, node, taskListener);
    }

    public String readCLILatestVersionFromVersionFile() {
        try {
            Path versionFilePath = findVersionFilePath().orElseThrow(() -> new ToolDetectionException("Could not find version file"));
            String fileVersion = Files.readString(versionFilePath.resolve(cliVersionFileName)).trim();
            return (StringUtils.isNotEmpty(fileVersion)) ? fileVersion : cliDefaultVersion;
        } catch (IOException e) {
            return cliDefaultVersion;
        }
    }

    public static Optional<Path> findVersionFilePath() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve(cliVersionFileName))) { // Change "pom.xml" to your marker file
                return Optional.of(dir);
            }
            dir = dir.getParent();
        }
        return Optional.empty();
    }


    private boolean isUpToDate(FilePath expectedLocation, CxLoggerAdapter log) throws IOException, InterruptedException {
        FilePath marker = expectedLocation.child(TIMESTAMP_FILE);
        if (!marker.exists()) {
            return false;
        }

        String content = StringUtils.chomp(marker.readToString());
        long timestampFromFile;
        try {
            timestampFromFile = Long.parseLong(content);
        } catch (NumberFormatException ex) {
            // corrupt of modified .timestamp file => force new installation
            log.error(".timestamp file is corrupt and cannot be read and will be reset to 0.");
            timestampFromFile = 0;
        }
        long timestampNow = Instant.now().toEpochMilli();

        long timestampDifference = timestampNow - timestampFromFile;
        if (timestampDifference <= 0) {
            return true;
        }
        long updateInterval = TimeUnit.HOURS.toMillis(updatePolicyIntervalHours);
        return timestampDifference < updateInterval;
    }

    private FilePath installCheckmarxCliAsSingleBinary(String version, FilePath expected, Node node, TaskListener log) throws IOException, InterruptedException {
        final VirtualChannel nodeChannel = node.getChannel();
        if (nodeChannel == null) {
            throw new IOException(format("Node '%s' is offline", node.getDisplayName()));
        }

        Platform platform = nodeChannel.call(new GetPlatform(node.getDisplayName()));

        try {
            String proxyStr = PluginUtils.getProxy();
            if (StringUtils.isNotEmpty(proxyStr)) {
                log.getLogger().println("Installer using proxy: " + proxyStr);
            }
            URL checkmarxDownloadUrl = DownloadService.getDownloadUrlForCli(version, platform);

            expected.mkdirs();
            nodeChannel.call(new Downloader(checkmarxDownloadUrl, proxyStr,
                    expected.child(DownloadService.buildFileName(version, platform)),
                    expected.child(platform.checkmarxWrapperFileName)
            ));

            expected.child(INSTALLED_FROM).write(checkmarxDownloadUrl.toString(), UTF_8.name());
            expected.child(TIMESTAMP_FILE).write(valueOf(Instant.now().toEpochMilli()), UTF_8.name());
        } catch (Exception ex) {
            log.getLogger().println("Checkmarx Security tool could not installed: " + ex.getMessage());
            throw new ToolDetectionException("Could not install Checkmarx CLI from binary", ex);
        }

        return expected;
    }

    @Extension
    public static final class CheckmarxInstallerDescriptor extends ToolInstallerDescriptor<CheckmarxInstaller> {

        @Override
        public String getDisplayName() {
            return "Install from checkmarx.com";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == CheckmarxInstallation.class;
        }
    }

    private static class GetPlatform extends MasterToSlaveCallable<Platform, IOException> {
        private static final long serialVersionUID = 1L;

        private final String nodeDisplayName;

        GetPlatform(String nodeDisplayName) {
            this.nodeDisplayName = nodeDisplayName;
        }

        @Override
        public Platform call() throws IOException {
            try {
                return Platform.current();
            } catch (ToolDetectionException ex) {
                throw new IOException(format("Could not determine platform on node %s", nodeDisplayName));
            }
        }
    }

    private static class Downloader extends MasterToSlaveCallable<Void, IOException> {
        private static final long serialVersionUID = 1L;

        private final URL downloadUrl;
        private final FilePath output;
        private final FilePath executableFile;
        private final String proxy;

        Downloader(URL downloadUrl, String proxy, FilePath output, FilePath executableFile) {
            this.downloadUrl = downloadUrl;
            this.output = output;
            this.executableFile = executableFile;
            this.proxy = proxy;
        }

        @Override
        public Void call() throws IOException {
            final File downloadedFile = new File(output.getRemote());
            try {
                copyURLToFile(downloadUrl, proxy, downloadedFile, 10000, 10000);
                extract(downloadedFile.getAbsolutePath(), downloadedFile.getParent());
            } catch (ArchiveException | CompressorException e) {
                throw new IOException(format("Could not extract cli: %s", downloadedFile.getAbsolutePath()));
            } catch (CheckmarxException | URISyntaxException e) {
                throw new RuntimeException(e);
            }

            final File cxExecutable = new File(executableFile.getRemote());
            // set execute permission
            if (!Functions.isWindows() && cxExecutable.isFile()) {
                boolean result = cxExecutable.setExecutable(true, false);

                if (!result) {
                    throw new IOException(format("Could not set executable flag for the file: %s", downloadedFile.getAbsolutePath()));
                }
            }
            return null;
        }

        @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        public static void copyURLToFile(URL source, String proxyStr, File destination, int connectionTimeoutMillis, int readTimeoutMillis) throws IOException, URISyntaxException, CheckmarxException {
            OkHttpClient client = new ProxyHttpClient().getHttpClient(proxyStr, connectionTimeoutMillis, readTimeoutMillis);
            Request request = new Request.Builder().url(source).build();
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            InputStream stream = responseBody.byteStream();
            try {
                FileUtils.copyInputStreamToFile(stream, destination);
            } catch (Throwable e) {
                throw new ToolDetectionException("failed to download file by URL", e);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }

        public static void extract(String srcFile, String dest) throws ArchiveException, IOException, CompressorException {
            FileInputStream fileInputStream = new FileInputStream(srcFile);
            ArchiveInputStream archiveInputStream = generateArchiveInputStream(fileInputStream, srcFile);

            File outputFile = new File(dest);
            boolean outputFileExisted = outputFile.exists() || outputFile.mkdirs();
            if (!outputFileExisted) {
                throw new IOException("Unable to create path");
            }

            ArchiveEntry nextEntry;
            while ((nextEntry = archiveInputStream.getNextEntry()) != null) {
                File tempFile = new File(dest, nextEntry.getName());
                if (nextEntry.isDirectory()) {
                    boolean folderExisted = tempFile.exists() || tempFile.mkdirs();
                    if (!folderExisted) {
                        throw new IOException("Unable to create path");
                    }
                } else {
                    FileOutputStream fos = FileUtils.openOutputStream(tempFile);
                    IOUtils.copy(archiveInputStream, fos);
                    fos.close();
                }
            }

            archiveInputStream.close();
            fileInputStream.close();
        }

        private static ArchiveInputStream generateArchiveInputStream(FileInputStream fis, String srcFile) throws ArchiveException, CompressorException {
            String extension = FilenameUtils.getExtension(srcFile);
            ArchiveStreamFactory asf = new ArchiveStreamFactory();

            if (extension.toLowerCase().endsWith("tgz") || extension.toLowerCase().endsWith("gz")) {
                CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, new BufferedInputStream(fis));

                return asf.createArchiveInputStream(new BufferedInputStream(cis));
            }

            return asf.createArchiveInputStream(new BufferedInputStream(fis));
        }
    }
}
