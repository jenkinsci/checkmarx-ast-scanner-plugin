# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Jenkins plugin (`checkmarx-ast-scanner`) that integrates the **Checkmarx One** (AST) platform into Jenkins CI/CD pipelines. The plugin is a thin wrapper around the [Checkmarx One CLI](https://checkmarx.com/resource/documents/en/34965-68620-checkmarx-one-cli-tool.html) — invoked through the [ast-cli-java-wrapper](https://github.com/Checkmarx/ast-cli-java-wrapper) — which uploads the workspace, triggers scans (SAST, SCA, IaC, Container, API, Secret Detection, Repository Health) and surfaces results inside Jenkins (summary, trends, deep-links, JSON/HTML/PDF/SBOM reports). Supports both Freestyle and Pipeline projects, custom CLI args, proxy, and policy/threshold based build-break.

**Status:** Active / maintained. Published to the Jenkins Update Center as [checkmarx-ast-scanner](https://plugins.jenkins.io/checkmarx-ast-scanner/). Part of Checkmarx CxOne Integrations.

## Technology Stack

| Component | Details |
|-----------|---------|
| Language | Java 11 (baseline) / also built against JDK 17 |
| Build tool | Apache Maven (Jenkins parent POM `org.jenkins-ci.plugins:plugin:4.88`) |
| Packaging | `hpi` (Jenkins plugin) |
| Jenkins baseline | `2.452.4` (LTS) — minimum supported runtime LTS 2.263.1 |
| Core dependency | `com.checkmarx.ast:ast-cli-java-wrapper:2.4.25` |
| CLI version (default) | `2.3.51` (see [cli.version](cli.version) and `CheckmarxInstaller.cliDefaultVersion`) |
| HTTP client | `okhttp` (via `okhttp-api` Jenkins plugin) for CLI download / proxy |
| JSON | `gson` (via `gson-api`), `jackson2-api`, `json-simple` |
| Logging | `slf4j-api` 2.0.15 wrapped by `CxLoggerAdapter` |
| Lombok | 1.18.34 (compile-time) |
| Test framework | JUnit + Jenkins Test Harness, Mockito 5.5.0, `mockwebserver` 4.11.0 |
| Coverage | JaCoCo 0.8.8 |
| CI | GitHub Actions (`.github/workflows/ci.yml`) + Jenkins pipeline ([Jenkinsfile](Jenkinsfile)) |

## Repository Structure

```
src/main/java/com/checkmarx/jenkins/
  ├── CheckmarxScanBuilder.java         Main build step (Builder + SimpleBuildStep) + Descriptor
  ├── CheckmarxScanResultsAction.java   Project/build action — exposes HTML report in UI
  ├── PluginUtils.java                  CLI invocation, scan submit/cancel, report generation
  ├── credentials/
  │   ├── CheckmarxApiToken.java        Credentials interface (Client ID / Secret)
  │   └── DefaultCheckmarxApiToken.java Default credentials impl
  ├── exception/
  │   ├── CheckmarxException.java
  │   └── ToolDetectionException.java
  ├── logger/
  │   └── CxLoggerAdapter.java          slf4j -> Jenkins TaskListener bridge
  ├── model/
  │   └── ScanConfig.java               Holds resolved scan parameters
  └── tools/
      ├── CheckmarxInstallation.java    Resolved CLI on a node
      ├── CheckmarxInstaller.java       Auto-downloads + caches the CLI per-node
      ├── Platform.java                 OS/arch detection for CLI asset selection
      ├── ProxyHttpClient.java          okhttp client honouring Jenkins ProxyConfiguration
      └── internal/
          └── DownloadService.java      Fetches CLI release assets from GitHub
src/main/resources/com/...              Jelly views, help-*.html for UI fields
src/main/webapp/images/                 Plugin icons / banners
src/test/java/com/checkmarx/jenkins/
  ├── integration/                      Tests that hit a real Checkmarx One tenant (need CX_* env)
  └── unit/                             Pure unit tests (no network)
.github/workflows/                      CI, publish, JIRA automation, Zizmor security scanning
  ├── ci.yml                            Maven build + unit/integration tests (parallel jobs, JaCoCo coverage)
  ├── scan-github-action.yml            Zizmor GitHub Actions security linter (runs on every PR)
  └── ... (other workflow files)
Jenkinsfile                             Uses jenkins-infra/pipeline-library buildPlugin (JDK 11 + 17, tests skipped)
cli.version                             Pinned default Checkmarx CLI version consumed at install time
pom.xml                                 Maven build config
```

## Development Setup

**Prerequisites:** JDK 11 (CI baseline) or 17, Maven 3.8+, Git. Internet access to `repo.jenkins-ci.org` is required for the first build.

```bash
git clone https://github.com/jenkinsci/checkmarx-ast-scanner-plugin.git
cd checkmarx-ast-scanner-plugin

mvn -B package -DskipTests                                  # build the .hpi (skip tests)
mvn test -Dtest=com.checkmarx.jenkins.unit.\*\* jacoco:report        # unit tests + coverage
mvn test -Dtest=com.checkmarx.jenkins.integration.\*\* jacoco:report # integration tests (need CX_* secrets)
mvn hpi:run                                                  # run a sandbox Jenkins at http://localhost:8080/jenkins
mvn versions:set -DnewVersion=2.0.14                         # bump plugin version
```

**Integration tests require these environment variables** (set in CI as repo secrets, see `.github/workflows/ci.yml`):

```
CX_BASE_URI         # e.g. https://ast.checkmarx.net
CX_BASE_AUTH_URI    # auth/IAM URL
CX_TENANT           # tenant name
CX_CLIENT_ID        # OAuth client id
CX_CLIENT_SECRET    # OAuth client secret
```

Built artifact: `target/checkmarx-ast-scanner.hpi` — drop into Jenkins → Manage Plugins → Advanced → Upload, or publish via `mvn deploy`.

## API / Interfaces

This plugin exposes itself to Jenkins users via three surfaces — there is **no public Java API for external callers**.

1. **Freestyle build step** — `Checkmarx AST Scan` (class [`CheckmarxScanBuilder`](src/main/java/com/checkmarx/jenkins/CheckmarxScanBuilder.java)).
2. **Pipeline step** — Symbol `checkmarxASTScanner`:
   ```groovy
   checkmarxASTScanner(
       useOwnServerCredentials: true,
       serverUrl: 'https://ast.checkmarx.net',
       tenantName: 'my-tenant',
       projectName: 'my-project',
       branchName: 'main',
       credentialsId: 'cx-creds',
       useOwnAdditionalOptions: true,
       additionalOptions: '--scan-types sast,sca --async'
   )
   ```
3. **Global / job tool installer** — `CheckmarxInstaller` (auto-downloads the CLI for the node's OS+arch from GitHub releases of [Checkmarx/ast-cli](https://github.com/Checkmarx/ast-cli)).

**Credentials**: implementations of `CheckmarxApiToken` (Client ID + Client Secret) are looked up via the standard Jenkins Credentials API. See [`DefaultCheckmarxApiToken`](src/main/java/com/checkmarx/jenkins/credentials/DefaultCheckmarxApiToken.java).

## Architecture

End-to-end flow on every build:

1. **`CheckmarxScanBuilder.perform`** resolves the configured `CheckmarxInstallation` and credentials, builds a [`ScanConfig`](src/main/java/com/checkmarx/jenkins/model/ScanConfig.java), and zips the workspace as required by the CLI.
2. **`CheckmarxInstaller`** ensures the CLI binary is present on the executor node (downloads + caches under the node's tool installation dir, keyed by `cli.version` / `cliDefaultVersion = 2.3.51`). Asset selection is driven by [`Platform`](src/main/java/com/checkmarx/jenkins/tools/Platform.java).
3. **`PluginUtils.submitScanDetailsToWrapper`** constructs a `CxConfig` and a `CxWrapper` from `ast-cli-java-wrapper`, then builds the argument list (`scan create`, `scan cancel`, etc.) passed to the CLI process.
4. The CLI streams logs back through [`CxLoggerAdapter`](src/main/java/com/checkmarx/jenkins/logger/CxLoggerAdapter.java) into the Jenkins `TaskListener`. Sensitive flag values (`--apikey`, `--client-secret`, `--token`, `--scs-repo-token`) are masked — see `CheckmarxScanBuilder.SENSITIVE_KEYS`.
5. On success, JSON + HTML reports are written into the build workspace (`checkmarx-ast-results.{json,html}`) and archived as build artifacts. `CheckmarxScanResultsAction` attaches them to the build's UI sidebar.
6. Proxy traffic (CLI download + REST calls) is routed through [`ProxyHttpClient`](src/main/java/com/checkmarx/jenkins/tools/ProxyHttpClient.java), which honours Jenkins' global `ProxyConfiguration`.

## Project Rules (Invariants)

- **Do not bump the Jenkins baseline (`jenkins.baseline` in [pom.xml](pom.xml)) without coordinating with the team.** The baseline determines the minimum LTS users must run and the BOM version that aligns transitive dependencies. A silent bump breaks older LTS users.
- **Do not change the plugin Symbol (`checkmarxASTScanner`) or the `@DataBoundConstructor` parameter order of [`CheckmarxScanBuilder`](src/main/java/com/checkmarx/jenkins/CheckmarxScanBuilder.java).** Existing pipeline scripts and `config.xml` files persisted on user controllers bind to these names — renames cause silent deserialization failures.
- **Keep `SENSITIVE_KEYS` in `CheckmarxScanBuilder` in sync with new CLI auth/token flags.** Anything that carries a secret on the command line must be masked in the console log.
- **Default CLI version lives in TWO places that must agree:** [cli.version](cli.version) and `CheckmarxInstaller.cliDefaultVersion`. The publish-plugin workflow extracts from [cli.version](cli.version) — if they drift, releases ship with the wrong default. See [.github/scripts/extract_cli_version.sh](.github/scripts/extract_cli_version.sh).
- **Bump `ast-cli-java-wrapper` and `cli.version` together.** The Java wrapper's API surface is coupled to specific CLI versions. The automation in [.github/workflows/update-java-wrapper-version.yml](.github/workflows/update-java-wrapper-version.yml) handles this — manual single-side bumps will break scan submission.
- **Jackson `jackson-bom` import in `<dependencyManagement>` MUST precede the Jenkins BOM.** It aligns `jackson-*` with the version pulled in by `ast-cli-java-wrapper` to satisfy `RequireUpperBoundDeps`. Reordering will break the build (see commit `76e89de`).
- **All long-running CLI invocations must call through the wrapper** (`CxWrapper.scanCreate` etc.) — do not `ProcessBuilder` the CLI directly. The wrapper handles exit codes, signal forwarding (for `Build Abort` → `scan cancel`), and structured error mapping to `CxException`.
- **Credentials are looked up using `findCredentialById` with the build's `Run` as the auth context.** Never read Client ID/Secret from `System.getenv()` in production paths — that path exists only for the integration test harness.

## Testing Strategy

Two distinct suites under [src/test/java/com/checkmarx/jenkins/](src/test/java/com/checkmarx/jenkins/):

- **Unit tests** ([unit/](src/test/java/com/checkmarx/jenkins/unit/)) — no network, no Jenkins controller. Use Mockito to stub `CxWrapper`, `okhttp` (via `MockWebServer` for `DownloadService` / `ProxyHttpClient`), and the Credentials API.
- **Integration tests** ([integration/](src/test/java/com/checkmarx/jenkins/integration/)) — spin up a `JenkinsRule` controller, instantiate a real `CheckmarxScanBuilder`, and hit a live Checkmarx One tenant using the `CX_*` environment variables. `CheckmarxTestBase` centralises tenant setup. **These fail locally without secrets** — run unit tests for fast feedback.

CI runs the two suites in parallel jobs (`unit-tests`, `integration-tests`) and publishes JaCoCo coverage as a workflow artifact. The Jacoco plugin excludes `exception/`, `model/`, `logger/`, `credentials/` packages from coverage (see `<excludes>` in [pom.xml](pom.xml)) — assertion-rich tests there will lift coverage of the **other** packages but won't be counted themselves.

There is **no enforced coverage gate** in CI today (coverage is computed and printed, but the build does not fail on it).

## Known Issues / Limitations

- **No coverage gate**: `ci.yml` prints the percentage but does not break the build under any threshold.
- **`Jenkinsfile` builds with `tests: [skip: true]`** — the official Jenkins infra build does not exercise the test suite; rely on the GitHub Actions `ci.yml` workflow for test signal.
- **`publish-plugin.yml` has the `validate` job disabled** (see commit `08c0072`) and `Release Drafter` / `category check` steps commented out (see `f672f77`). Releases skip the validation step until those are re-enabled.
- **CLI download** depends on GitHub release availability of [Checkmarx/ast-cli](https://github.com/Checkmarx/ast-cli) — air-gapped controllers must pre-stage the binary and point the installer at a local mirror.
- **`mvn hpi:run` requires JDK 11**; running with JDK 17 may surface warnings from `animal-sniffer` (the override block in [pom.xml](pom.xml) is intentionally commented out — Java 11 signatures are unavailable).
- **Workspace zipping is not streaming** — very large workspaces are fully read into memory before upload.

## External Integrations

- **Checkmarx/ast-cli** ([github.com/Checkmarx/ast-cli](https://github.com/Checkmarx/ast-cli)) — the binary downloaded and exec'd by every scan. Asset format depends on `Platform`.
- **Checkmarx/ast-cli-java-wrapper** (`com.checkmarx.ast:ast-cli-java-wrapper`) — Java SDK that builds CLI args and parses CLI output. Version pin in [pom.xml](pom.xml) must match the CLI version in [cli.version](cli.version).
- **Checkmarx One platform** — OAuth-authenticated REST API. Plugin never talks to it directly; all calls go through the CLI.
- **Jenkins Update Center** — release artifact is consumed at [plugins.jenkins.io/checkmarx-ast-scanner](https://plugins.jenkins.io/checkmarx-ast-scanner/).
- **JIRA** ([checkmarx.atlassian.net](https://checkmarx.atlassian.net)) — issue automation in `.github/workflows/jira_notify.yml` and `jira_close.yml` syncs GitHub issues with the AST project.
- **Zizmor** ([zizmor.sh](https://zizmor.sh)) — GitHub Actions security linter. Scans all workflows via [scan-github-action.yml](.github/workflows/scan-github-action.yml) on every PR; all workflows pass with 0 HIGH/MEDIUM severity issues. Enforces action pinning to commit SHAs, least-privilege permissions, template-injection prevention, and concurrency controls.

## Deployment

Release artefact is the `.hpi` produced by `mvn package` and published to the Jenkins Update Center.

- **Stable release**: [.github/workflows/publish-plugin.yml](.github/workflows/publish-plugin.yml) runs `mvn -B release:prepare release:perform` on a tag push to `main` and uploads to the Update Center.
- **Pre-release / dev build**: [.github/workflows/publish-pre-release.yml](.github/workflows/publish-pre-release.yml) attaches a snapshot `.hpi` to a GitHub pre-release for QA validation.
- **CLI version sync**: [.github/workflows/update-java-wrapper-version.yml](.github/workflows/update-java-wrapper-version.yml) opens a PR updating both `ast-cli-java-wrapper` and [cli.version](cli.version) when a new CLI is released.

There is no service / container deployment — the plugin runs in-process inside a Jenkins controller and ships work to executor nodes.

## Performance Considerations

- **CLI install is cached per node** under the tool installation directory. First scan on a fresh agent downloads ~30–60 MB; subsequent scans reuse the cached binary. The `.timestamp` and `.installedFrom` marker files in the install dir gate re-download — deleting either forces a fresh fetch.
- **Workspace zip is in-memory** (see `CheckmarxScanBuilder.perform` and `ArtifactArchiver` integration). For 1 GB+ workspaces consider tuning the executor heap.
- **CLI is invoked per build** — there is no long-lived daemon. Authentication tokens are acquired per scan.
- **Report generation** can request multiple formats (`JSON`, `HTML`, `PDF`, SBOMs). Each format adds a separate CLI subprocess; minimise the requested formats for fast pipelines.

## Security & Access

- **Credentials**: Client ID / Client Secret are stored as Jenkins `Credentials` (`DefaultCheckmarxApiToken`) and resolved per-build via `CredentialsProvider.findCredentialById`. Never log or echo them.
- **Command-line masking**: values for `--apikey`, `--client-secret`, `--token`, `--scs-repo-token` are stripped from console output by `CheckmarxScanBuilder.SENSITIVE_KEYS`. **Add any new auth flag here** when bumping the CLI.
- **Proxy support**: All outbound HTTP (CLI download + scan submission) honours Jenkins' global `ProxyConfiguration` via `ProxyHttpClient`.
- **Sandbox compliance**: Pipeline `@Symbol("checkmarxASTScanner")` and all `@DataBound*` setters are designed to pass the script-security sandbox check on Jenkins controllers with the default permissive whitelist.
- **No secret persistence in build records**: `ScanConfig` does NOT serialise credentials; only the credentials ID (an opaque reference) is stored.
- **Form validation endpoints** in `CheckmarxScanBuilderDescriptor` are annotated `@POST` to prevent CSRF on doCheck* / doFill* methods.

## Logging

- **`CxLoggerAdapter`** ([logger/CxLoggerAdapter.java](src/main/java/com/checkmarx/jenkins/logger/CxLoggerAdapter.java)) bridges the slf4j calls inside `ast-cli-java-wrapper` to the Jenkins `TaskListener` so users see CLI output in the build console.
- **slf4j-api 2.0.15** is bundled at compile scope to avoid the 1.x runtime on older Jenkins versions (`ast-cli-java-wrapper` requires 2.x).
- **Console output is regex-scanned** by `PluginUtils.REGEX_SCAN_ID_FROM_LOGS` to extract the scan UUID for the post-build action and deep-link UI. Changing the CLI's log format (or stripping that line) breaks the result page.
- **No file-based logging** is configured by the plugin — everything goes to the build's console / `log` file managed by Jenkins.

## Coding Standards

- **Lombok** (`@NonNull`, `@SneakyThrows`, `@Getter`) is used throughout — ensure your IDE has the Lombok plugin enabled.
- **`@DataBoundConstructor` / `@DataBoundSetter`** must be present on every field exposed to Jelly UI or pipeline DSL. Adding a private field without one means it won't round-trip through `config.xml`.
- **`@SuppressFBWarnings`** is used sparingly to silence SpotBugs — prefer fixing the underlying issue.
- **Symbol naming**: keep `@Symbol` annotations stable (see Invariants).
- **Help text** lives under `src/main/resources/com/checkmarx/jenkins/.../help-<fieldName>.html` — every UI field should have a help file.
- **Test naming**: `<Class>Test` for unit, placed under `unit/` or `integration/` mirroring the production package. CI selects tests by package via `-Dtest=com.checkmarx.jenkins.unit.\*\*` / `integration.\*\*`.

## Debugging Steps

1. **Run a real scan in a sandbox Jenkins:**
   ```bash
   mvn hpi:run -Djetty.port=8090
   # then open http://localhost:8090/jenkins and configure a Freestyle job
   ```

2. **CLI not installing on agent:** Inspect `~/.jenkins/tools/com.checkmarx.jenkins.tools.CheckmarxInstallation/<name>/`. The `.installedFrom` file shows the URL it tried; `.timestamp` controls re-download. Delete both to force a fresh fetch. Check agent's outbound access to `github.com` / `objects.githubusercontent.com`.

3. **`CxException` during scan:** the wrapper masks the underlying CLI exit code. Re-run the failing job with `additionalOptions: '--debug'` to get verbose CLI output in the console, then grep for `ERROR` lines.

4. **Pipeline step "no such DSL method `checkmarxASTScanner`":** the plugin isn't installed on the controller, or you renamed the `@Symbol`. Verify the plugin appears in Manage Plugins → Installed.

5. **Credentials not appearing in the dropdown:** the credential type isn't `CheckmarxApiToken`. Either create one via the Checkmarx credentials provider or check that `doFillCredentialsIdItems` is being called (it's `@POST`, so CSRF protection must be satisfied — usually fine via the Jenkins UI).

6. **Coverage report empty:** the `<excludes>` block in [pom.xml](pom.xml) drops `exception/`, `model/`, `logger/`, `credentials/`. If you wrote a test there, it still runs but doesn't add to the reported percentage.

7. **Release didn't publish:** check that the `validate` job re-enable status and Release Drafter steps in [.github/workflows/publish-plugin.yml](.github/workflows/publish-plugin.yml) — those were intentionally disabled in `08c0072` and `f672f77` as a temporary fix.
