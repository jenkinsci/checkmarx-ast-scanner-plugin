package com.checkmarx.jenkins;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.join;
import static org.apache.commons.lang.StringUtils.chomp;

class CustomBuildToolPathCallable implements FilePath.FileCallable<String> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CustomBuildToolPathCallable.class.getName());
    private static final String TOOLS_DIRECTORY = "tools";

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "spotbugs issue (java 11)")
    @Override
    public String invoke(final File checkmarxToolDirectory, final VirtualChannel channel) {
        final String oldPath = System.getenv("PATH");
        final String home = checkmarxToolDirectory.getAbsolutePath();
        if (!home.contains(CustomBuildToolPathCallable.TOOLS_DIRECTORY)) {
          CustomBuildToolPathCallable.LOG.info("env.PATH will be not modified, because there are no configured global tools");
            return oldPath;
        }
        final String toolsDirectory = home.substring(0, home.indexOf(CustomBuildToolPathCallable.TOOLS_DIRECTORY) - 1) + File.separator + CustomBuildToolPathCallable.TOOLS_DIRECTORY;

        try (final Stream<Path> toolsSubDirectories = Files.walk(Paths.get(toolsDirectory))) {
            final List<String> toolsPaths = new ArrayList<>();
            toolsSubDirectories.filter(Files::isDirectory)
                    .filter(path -> !path.toString().contains("CheckmarxInstallation"))
                    .filter(path -> path.toString().endsWith("bin"))
                    .forEach(entry -> toolsPaths.add(chomp(entry.toAbsolutePath().toString())));

            final String customBuildToolPath = join(File.pathSeparator, toolsPaths);
            return oldPath + File.pathSeparator + customBuildToolPath;
        } catch (final IOException ex) {
          CustomBuildToolPathCallable.LOG.error("Could not iterate sub-directories in tools directory", ex);
            return oldPath;
        }
    }

    @Override
    public void checkRoles(final RoleChecker roleChecker) {
        //squid:S1186
    }
}
