[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=bugs)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=coverage)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.pdf-exporter)

# Polarion ALM extension to convert Documents to PDF files

This Polarion extension provides possibility to convert Polarion Documents to PDF files.
This is an alternative to native Polarion's solution.
The extension uses [WeasyPrint](https://weasyprint.org/) as a PDF engine and requires it to run in [Docker as Service](CONFIGURATION.md#weasyprint-configuration).

> [!IMPORTANT]
> Starting from version 8.0.0 only latest version of Polarion is supported.
> Right now it is Polarion 2606.

> [!IMPORTANT]
> Please, read our [disclaimer](DISCLAIMER.md) before using this extension.

## Documentation

| Page | Contents |
| --- | --- |
| [Quick start](QUICK_START.md) | The most important steps and configurations, briefly summarized |
| [Configuration](CONFIGURATION.md) | Full configuration reference: WeasyPrint, toolbar and Live Report injection, bulk export, external resources, logging, workflow function, style packages and more |
| [User guide](USER_GUIDE.md) | The export options in the PDF Exporter dialog |
| [Limitations and workarounds](LIMITATIONS.md) | By-design behaviour (rendering and PDF/A · PDF/UA compliance) and how to adjust it |
| [Upgrade notes](UPGRADE.md) | Version-specific upgrade instructions |
| [Performance & resource planning](CONFIGURATION.md#performance-and-resource-planning) | Memory requirements and benchmarks |
| [REST API](docs/openapi.json) | OpenAPI specification |

## Build

This extension can be produced using maven:
```bash
mvn clean package
```

## Installation to Polarion

To install the extension to Polarion `ch.sbb.polarion.extension.pdf-exporter-<version>.jar`
should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.pdf-exporter/eclipse/plugins`
It can be done manually or automated using maven build:
```bash
mvn clean install -P local-install-into-polarion
```
For automated installation with maven env variable `POLARION_HOME` should be defined and point to folder where Polarion is installed.

Changes only take effect after restart of Polarion.

## Usage

1. Open a document in Polarion.
2. In the toolbar choose Show Sidebar ➙ Document Properties.
3. Choose desired options in the `PDF Exporter` block and click `Export to PDF`.
   For the options details please refer [user guide](USER_GUIDE.md).

## REST API
This extension provides REST API. OpenAPI Specification can be obtained [here](docs/openapi.json).

For asynchronous export and job-timeout tuning, see [Advanced configuration](CONFIGURATION.md#advanced-configuration).
