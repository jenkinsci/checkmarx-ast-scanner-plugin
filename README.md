<img src="https://raw.githubusercontent.com/Checkmarx/ci-cd-integrations/main/.images/PluginBanner.jpg">
<br />
<div align="center">

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![Install][install-shield]][install-url]
[![APACHE License][license-shield]][license-url]

</div>
<br />
<p align="center">
  <a href="https://github.com/jenkinsci/checkmarx-ast-scanner-plugin">
    <img src="https://raw.githubusercontent.com/Checkmarx/ci-cd-integrations/main/.images/cx_x_icon.png" alt="Logo" width="80" height="80" />
  </a>

<h3 align="center">AST-JENKINS-PLUGIN </h3>

<p align="center">
    Allows the user to scan their source code using Checkmarx AST platform and provide the results as a feedback.
<br />
    <a href="https://checkmarx.com/resource/documents/en/34965-68685-checkmarx-one-jenkins-plugin.html"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://issues.jenkins-ci.org/">Report Bug</a>
    ·
    <a href="https://github.com/jenkinsci/checkmarx-ast-scanner-plugin/issues/new">Request Feature</a>
  </p>
</p>



<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
    </li>
    <li>
      <a href="#key-features">Key Features</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#initial-setup">Initial Setup</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#contribution">Contribution</a></li>
    <li><a href="#feedback">Feedback</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project

The Checkmarx One Jenkins plugin enables you to integrate the full functionality of the Checkmarx One platform into your Jenkins pipelines. You can use this plugin to trigger Checkmarx One scans as part of your CI/CD integration.

This plugin provides a wrapper around the [Checkmarx One CLI Tool](https://checkmarx.com/resource/documents/en/34965-68620-checkmarx-one-cli-tool.html) which creates a zip archive from your source code repository and uploads it to Checkmarx One for scanning. This provides easy integration with Jenkins while enabling scan customization using the full functionality and flexibility of the CLI tool.

> The plugin code can be found [here](https://github.com/jenkinsci/checkmarx-ast-scanner-plugin/).

<!-- KEY FEATURES -->
## Key Features

-   Configure Jenkins pipelines to automatically trigger scans running all Checkmarx One scanners: CxSAST, CxSCA, IaC Security, Container Security, API Security, Secret Detection and Repository Health (OSSF Scorecard).
-   Supports integrating Checkmarx One build steps into FreeStyle or Pipeline projects
-   Supports use of CLI arguments to customize scan configuration, enabling you to:
    -   Customize filters to specify which folders and files are scanned
    -   Apply preset query configurations
    -   Customize SCA scans using [SCA Resolver](https://checkmarx.com/resource/documents/en/34965-19196-checkmarx-sca-resolver.html)
    -   Set thresholds to break build
-   Send requests via a proxy server
-   Break build upon policy violation

-   View scan results summary and trends in the Jenkins environment
-   Direct links from within Jenkins to detailed Checkmarx One scan
    results
-   Generate customized scan reports in various formats (JSON, HTML, PDF
    etc.)
-   Generate SBOM reports (CycloneDX and SPDX)
-   Can be configured to automatically update to the latest CLI version

### Prerequisites

-   A Jenkins installation LTS 2.263.1 or above (Supported Operating systems: Windows and Linux)

-   You have a Checkmarx One account and you have an OAuth **Client ID** and **Client Secret** for that account. To create an OAuth client, see [Creating an OAuth Client for Checkmarx One Integrations](https://checkmarx.com/resource/documents/en/34965-118315-authentication-for-checkmarx-one-cli.html#UUID-a4e31a96-1f36-6293-e95a-97b4b9189060_UUID-4123a2ff-32d0-2287-8dd2-3c36947f675e).

### Initial Setup
-   Verify that all prerequisites are in place.

-   Install the **Checkmarx AST Scanner** plugin and configure the
    settings as described [here](https://checkmarx.com/resource/documents/en/34965-68687-checkmarx-one-jenkins-plugin---installation-and-initial-setup.html).

## Usage

To see how you can use our tool, please refer to the [Documentation](https://checkmarx.com/resource/documents/en/34965-68685-checkmarx-one-jenkins-plugin.html)


## Contribution

- Review the default [CONTRIBUTING](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md) file and make sure it is appropriate for your plugin, if not then add your own one adapted from the base file

- Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## Feedback
We’d love to hear your feedback! If you come across a bug or have a feature request, please let us know by submitting an issue in [GitHub Issues](https://github.com/jenkinsci/checkmarx-ast-scanner-plugin/issues).

<!-- LICENSE -->
## License
Distributed under the [MIT](LICENSE). See `LICENSE` for more information.


<!-- CONTACT -->
## Contact

Checkmarx - CxOne Integrations Team

Find more integrations from our team [here](https://github.com/Checkmarx/ci-cd-integrations#checkmarx-ast-integrations)


© 2024 Checkmarx Ltd. All Rights Reserved.

[contributors-shield]: https://img.shields.io/github/contributors/jenkinsci/checkmarx-ast-scanner-plugin.svg
[contributors-url]: https://github.com/jenkinsci/checkmarx-ast-scanner-plugin/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/jenkinsci/checkmarx-ast-scanner-plugin.svg
[forks-url]: https://github.com/jenkinsci/checkmarx-ast-scanner-plugin/network/members
[stars-shield]: https://img.shields.io/github/stars/jenkinsci/checkmarx-ast-scanner-plugin.svg
[stars-url]: https://github.com/jenkinsci/checkmarx-ast-scanner-plugin/stargazers
[issues-shield]: https://img.shields.io/github/issues/jenkinsci/checkmarx-ast-scanner-plugin.svg
[issues-url]: https://github.com/jenkinsci/checkmarx-ast-scanner-plugin/issues
[license-shield]: https://img.shields.io/github/license/jenkinsci/checkmarx-ast-scanner-plugin.svg
[license-url]: https://github.com/jenkinsci/checkmarx-ast-scanner-plugin/blob/main/LICENSE
[install-shield]: https://img.shields.io/jenkins/plugin/i/checkmarx-ast-scanner
[install-url]: https://plugins.jenkins.io/checkmarx-ast-scanner/
