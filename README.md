<!-- PROJECT LOGO -->
<br />
<p align="center">
  <a href="">
    <img src="./logo.png" alt="Logo" width="80" height="80">
  </a>

<h3 align="center">AST-JENKINS-PLUGIN </h3>

<p align="center">
    Allows the user to scan their source code using Checkmarx AST platform and provide the results as a feedback.
<br />
    <a href="https://checkmarx.atlassian.net/wiki/spaces/AST/pages/2966164587/Jenkins+Plugin"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://issues.jenkins-ci.org/">Report Bug</a>
    ·
    <a href="https://github.com/CheckmarxDev/checkmarx-ast-scanner-plugin/issues/new">Request Feature</a>
  </p>
</p>



<!-- TABLE OF CONTENTS -->
<details open="open">
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

The CxAST Jenkins Plugin allows the user to trigger CxSAST, CxSCA and KICS scans directly from a Jenkins workflow. 
It provides a wrapper around the CxAST CLI Tool which creates a zip archive from your source code repository and uploads 
it to CxAST for scanning. The plugin provides easy integration into Jenkins while enabling scan customization using the 
full functionality and flexibility of the CLI tool.

<!-- GETTING STARTED -->
## Getting Started

The plugin can be configured as build step within the Job Configuration.

### Prerequisites

- A Jenkins installation v2.263.1 or above

- Access to a CxAST account (user credentials or an API Key)

### Setting Up
To set the plugin up, follow this [Instructions](https://checkmarx.atlassian.net/wiki/spaces/AST/pages/3221226473/Installing+the+Jenkins+CxAST+Plugin)

## Usage

To see how you can use our tool, please refer to the [Documentation](https://checkmarx.atlassian.net/wiki/spaces/AST/pages/2966164587/Jenkins+Plugin)


## Contribution

- Review the default [CONTRIBUTING](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md) file and make sure it is appropriate for your plugin, if not then add your own one adapted from the base file

- Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

** **

<!-- LICENSE -->
## License
See `LICENSE` for more information.



<!-- CONTACT -->
## Contact

Checkmarx - AST Integrations Team

Project Link: [https://github.com/CheckmarxDev/checkmarx-ast-scanner-plugin](https://github.com/CheckmarxDev/checkmarx-ast-scanner-plugin)


© 2021 Checkmarx Ltd. All Rights Reserved.
