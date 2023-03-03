<img src="https://raw.githubusercontent.com/Checkmarx/ci-cd-integrations/main/.images/banner.png">
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
    <img src="https://raw.githubusercontent.com/Checkmarx/ci-cd-integrations/main/.images/logo.png" alt="Logo" width="80" height="80" />
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
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#setting-up">Setting Up</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project

The CxOne/CxAST Jenkins Plugin allows the user to trigger SAST, SCA and IaC and API Security scans directly from a Jenkins workflow. 
It provides a wrapper around the CxOne CLI Tool which creates a zip archive from your source code repository and uploads 
it to CxOne for scanning. The plugin provides easy integration into Jenkins while enabling scan customization using the 
full functionality and flexibility of the CLI tool.

<!-- GETTING STARTED -->
## Getting Started

The plugin can be configured as build step within the Job Configuration.

### Prerequisites

- A Jenkins installation v2.263.1 or above

- Access to a CxOne account (user credentials or an API Key)

### Setting Up
To set the plugin up, follow this [Instructions](https://checkmarx.com/resource/documents/en/34965-68687-checkmarx-one-jenkins-plugin---installation-and-initial-setup.html)

## Usage

To see how you can use our tool, please refer to the [Documentation](https://checkmarx.com/resource/documents/en/34965-68685-checkmarx-one-jenkins-plugin.html)


## Contribution

- Review the default [CONTRIBUTING](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md) file and make sure it is appropriate for your plugin, if not then add your own one adapted from the base file

- Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)


<!-- LICENSE -->
## License
Distributed under the [MIT](LICENSE). See `LICENSE` for more information.


<!-- CONTACT -->
## Contact

Checkmarx - CxOne Integrations Team

Find more integrations from our team [here](https://github.com/Checkmarx/ci-cd-integrations#checkmarx-ast-integrations)


© 2022 Checkmarx Ltd. All Rights Reserved.

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
