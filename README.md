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
The extension uses [WeasyPrint](https://weasyprint.org/) as a PDF engine and requires it to run in [Docker as Service](#weasyprint-configuration).

> [!IMPORTANT]
> Starting from version 8.0.0 only latest version of Polarion is supported.
> Right now it is Polarion 2506.

> [!IMPORTANT]
> Please, read our [disclaimer](DISCLAIMER.md) before using this extension.

## Quick start

Please see separate [quick start page](QUICK_START.md) where briefly summarized all most important and applicable steps and configurations.

If you need deeper knowledge about all possible steps, configurations and their descriptions, please see sections below.

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
mvn clean install -P install-to-local-polarion
```
For automated installation with maven env variable `POLARION_HOME` should be defined and point to folder where Polarion is installed.

Changes only take effect after restart of Polarion.

## Polarion configuration

### WeasyPrint configuration

This extension supports the use of WeasyPrint as a REST service within a Docker container, as implemented [here](https://github.com/SchweizerischeBundesbahnen/weasyprint-service).
To change WeasyPrint Service URL, adjust the following property in the `polarion.properties` file:

```properties
ch.sbb.polarion.extension.pdf-exporter.weasyprint.service=http://localhost:9080
```

### PDF exporter extension to appear on a Document's properties pane

1. Open a project where you wish PDF Exporter to be available
2. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
3. On the administration's navigation pane select Documents & Pages ➙ Document Properties Sidebar.
4. In opened Edit Project Configuration editor find `sections`-element:
   ```xml
   …
   <sections>
     <section id="fields"/>
     …
   </sections>
   …
   ```
5. And insert following new line inside this element:
   ```xml
   …
   <extension id="pdf-exporter" label="PDF Exporter" />
   …
   ```
6. Save changes by clicking 💾 Save

### PDF Exporter view to open via button in toolbar

Alternatively you can configure PDF Exporter such a way that additional toolbar will appear in document's editor with a button to open a popup with PDF Exporter view.

1. Open "Default Repository".
2. On the top of its navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Global administration page will be opened.
3. On the administration's navigation pane select Configuration Properties.
4. In editor of opened page add following line:
   ```properties
   scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/starter.js"></script><script>PdfExporterStarter.injectToolbar();</script>
   ```
   There's an alternate approach adding PDF Exporter button into native Polarion's toolbar, which has a drawback at the moment -
   button disappears in some cases (for example when document is saved), so using this approach is not advisable:
   ```properties
   scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/starter.js"></script><script>PdfExporterStarter.injectToolbar({alternate: true});</script>
   ```
5. Save changes by clicking 💾 Save

### PDF Exporter view to open in Live Reports

Live Reports also can be converted to PDF with help of this extension.

First of all you need to inject appropriate JavaScript code into Polarion:

1. Open "Default Repository".
2. On the top of its navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Global administration page will be opened.
3. On the administration's navigation pane select Configuration Properties.
4. In editor of opened page add following line:
   ```properties
   scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/starter.js"></script>
   ```
5. Save changes by clicking 💾 Save

Then open a project, its Live Report you wish to export, and click "Expand Tools" on top of the page.
As a result report's toolbar will appear. Click "Edit" button in a toolbar, as a result the report will be switched into an edit mode. Add an empty region on top of the report, place cursor there, choose "PDF Export" tag on "Widgets" sidebar on right hand side of the page, find "Export to PDF Button" widget there and click it to add to the report. Then save a report clicking 💾 in a toolbar and then return to a view mode clicking "Back" button. When you click "Export to PDF" button just added to the report, PDF Exporter view will be opened in a popup and you will be able to proceed with exporting the report to PDF. Be aware that in report's context limited set of properties are available for configuration in PDF popup, the rest of them are relevant only in Live Document context.

### Configuring logs

For better problem analyses extended logging can be configured in Polarion. By default, Polarion log level is set to INFO. It can be changed to debug in `log4j2.xml` file.
Find `/opt/polarion/polarion/plugins/com.polarion.core.util_<version>/log4j2.xml` file and add the following line into `Loggers`section:
```xml
<Logger name="ch.sbb.polarion.extension" level="debug"/>
```

It is also possible to write all messages of SBB extensions info separate log file which can be useful to report a problem. In this case new `Appender` should be added:
```xml
<RollingFile name="SBB" fileName="${sys:logDir}/log4j-sbb${fileNameSuffix}" filePattern="${sys:logDir}/log4j-sbb${filePatternSuffix}">
    <PatternLayout pattern="${layoutPattern}"/>
    <Policies>
        <TimeBasedTriggeringPolicy interval="1"/>
    </Policies>
</RollingFile>
```
and the following `Logger`:
```xml
<Logger name="ch.sbb.polarion.extension" level="debug">
    <AppenderRef ref="SBB"/>
</Logger>
```

### Enabling CORS

Cross-Origin Resource Sharing could be enabled using standard configuration of Polarion REST API. In `polarion.properties` the following lines should be added:
```properties
com.siemens.polarion.rest.enabled=true
com.siemens.polarion.rest.cors.allowedOrigins=http://localhost:8888,https://anotherdomain.com
```

### Enabling webhooks

By default, webhooks functionality is not enabled in PDF Exporter. If you want to make it available the following line should be added in `polarion.properties`:
```properties
ch.sbb.polarion.extension.pdf-exporter.webhooks.enabled=true
```

### Debug option

This extension makes intensive HTML processing to extend similar standard Polarion functionality. There is a possibility to log
original and resulting HTML to see potential problems in this processing. This logging can be switched on (`true` value)
and off (`false` value) with help of following property in file `polarion.properties`:

```properties
ch.sbb.polarion.extension.pdf-exporter.debug=true
```

If HTML logging is switched on, then in standard polarion log file there will be following lines:

```text
2023-09-20 08:42:13,911 [ForkJoinPool.commonPool-worker-2] INFO  util.ch.sbb.polarion.extension.pdf_exporter.util.HtmlLogger - Original HTML fragment provided by Polarion was stored in file /tmp/pdf-exporter10000032892830031969/original-4734772539141140796.html
2023-09-20 08:42:13,914 [ForkJoinPool.commonPool-worker-2] INFO  util.ch.sbb.polarion.extension.pdf_exporter.util.HtmlLogger - Final HTML page obtained as a result of PDF exporter processing was stored in file /tmp/pdf-exporter10000032892830031969/processed-5773281490308773124.html
```

Here you can find out in which files HTML was stored.

### Workflow function configuration
It is possible to configure the workflow function which exports a PDF file and attaches it to a newly created or already existing work item.

To create workflow functions do following:
1. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration pane will be opened.
2. On the administration's navigation pane select Documents & Pages ➙ Document Workflow.
3. On the opened page you will see a list of document types with their actions. Find type you are interested in and click `Edit` or `Create` button for it.
4. On the opened page (Workflow Designer) find the section Actions, appropriate action in it, e.g. `archive` (or create a new one) and click `Edit` for it.
5. A popup will be opened with title 'Details for Action: Archive', select 'PDF Export' in 'Function' dropdown of 'Functions' section and then click
   pencil button. Another popup will be opened with title 'Parameter for: PDF Export', add appropriate parameters in table of this popup, then click `Close`.
   Then again `Close` on previous popup and finally `Save` when you will be back on Workflow Designer page.

Supported function parameters:

| Parameter             | Required | Description                                                                 | Default value                                                                                                                     |
|-----------------------|----------|-----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| existing_wi_id        | yes (*)  | Workitem ID to reuse                                                        | -                                                                                                                                 |
| create_wi_type_id     | yes (*)  | Type ID of workitem to create                                               | -                                                                                                                                 |
| create_wi_title       | no       | Value to set as a workitem title (used only with 'create_wi_type_id')       | Value like "modified document title with space -> target status name" (e.g., "Specification / Product Specification -> Archived") |
| create_wi_description | no       | Value to set as a workitem description (used only with 'create_wi_type_id') | "This item was created automatically. Check 'Attachments' section for the generated PDF document."                                |
| project_id            | no       | Project ID where to create or search for the target work item               | Project ID of the modified document                                                                                               |
| attachment_title      | no       | The title of the attached file                                              | The name of the generated file (without '.pdf' at the end)                                                                        |
| style_package         | no       | The name of the style package to use                                        | Default                                                                                                                           |
| prefer_last_baseline  | no       | Use the last baseline revision instead of the last document's revision      | false                                                                                                                             |
(*) - either 'existing_wi_id' or 'create_wi_type_id' parameter required.
Providing the first one means reuse already existing workitem to attach the file whereas the second will create a new workitem with the specified type.
In case if both of them specified 'existing_wi_id' has higher priority.

## Extension configuration

1. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
2. On the administration's navigation pane select `PDF Export`. There are expandable sub-menus with different configuration options for PDF Exporter.
3. For some of these options (Cover page, Header and Footer, Localization, Webhooks and Filename template) `Quick Help` section available with short description of appropriate option. For the rest
   (Style package, Style package weights, CSS) there's no `Quick Help` section as their content is self-evident.
4. Sections Cover page, Header and Footer, and Filename template have possibility to use default values (can't be edited) or custom. Section CSS has different approach -
   its area for custom values is always enabled, but default values can be enabled or disabled. Therefore, either only custom CSS is used or default CSS is combined with custom.
5. To change configuration of PDF Exporter extension just edit corresponding section and press `Save` button.

### CSS for booklet layout

If you export PDF to be printed as a booklet, then you may need to alternate blocks in header/footer depending on the fact if it's even or odd page.
This can be achieved (since version 8.1.0) by CSS modification. Let us give you an example. Find following definition in standard CSS of the extension:

```css
@page :left {
    @top-left {
        content: element(top-left);
    }
    @top-right {
        content: element(top-right);
    }
    @bottom-left {
        content: element(bottom-left);
    }
    @bottom-right {
        content: element(bottom-right);
    }
}
```

...and replace it by this code:

```css
@page :left {
    @top-left {
        content: element(top-right);
    }
    @top-right {
        content: element(top-left);
    }
    @bottom-left {
        content: element(bottom-right);
    }
    @bottom-right {
        content: element(bottom-left);
    }
}
```

As a result blocks in header and footer which in normal case are displayed at right side of the header/footer will be displayed at left side and vice versa.
This is only an example to illustrate an idea, if your use case is different feel free to modify this code according to your requirements.


## Usage

1. Open a document in Polarion.
2. In the toolbar choose Show Sidebar ➙ Document Properties.
3. Choose desired options in the `PDF Exporter` block and click `Export to PDF`.
   For the options details please refer [user guide](USER_GUIDE.md).

## REST API
This extension provides REST API. OpenAPI Specification can be obtained [here](docs/openapi.json).

## Advanced configuration

### Asynchronous PDF Export: export jobs timeout
This extension provides REST API to export PDF asynchronously. Using this API, it is possible to start export job, observe their status and get result.
Finished (succeed or failed) and in-progress export jobs will be preserved in memory until configured timeout. To change this timeout, adjust the following property in the local `pdf-converter-jobs.properties` file:
```properties
# Timeout in minutes to keep finished async conversion jobs results in memory
jobs.timeout.finished.minutes=30
# Timeout in minutes to wait until async conversion jobs is finished
jobs.timeout.in-progress.minutes=60
```

## Known issues

### PDF/A-*A variants with icon fonts (FontAwesome)

PDF/A "A" (accessible) variants require that characters from Unicode Private Use Area (PUA) have `ActualText` entries for accessibility.
Icon fonts like FontAwesome use PUA codepoints, which causes validation failures with VeraPDF for:
- `pdf/a-1a`
- `pdf/a-2a`
- `pdf/a-3a`

**Workaround:** If your documents contain icon fonts (e.g., FontAwesome icons from Polarion), use "B" or "U" variants instead:
- `pdf/a-1b` instead of `pdf/a-1a`
- `pdf/a-2b` or `pdf/a-2u` instead of `pdf/a-2a`
- `pdf/a-3b` or `pdf/a-3u` instead of `pdf/a-3a`

### Native PDF annotations (sticky notes) incompatible with PDF/A

When exporting documents with comments rendered as native PDF annotations (sticky notes), the resulting PDF will fail PDF/A validation. This affects all PDF/A variants (`pdf/a-1*`, `pdf/a-2*`, `pdf/a-3*`, `pdf/a-4*`).

VeraPDF reports the following errors for PDF/A-2b:
- `6.2.10-2` - Annotation appearance stream missing
- `6.1.3-1` - Info dictionary issues
- `6.2.4.3-2` - OutputIntent issues
- `6.3.2-1` - Font embedding issues
- `6.6.2.1-1` - Annotation flags issues

This happens because PDF/A standards require all annotations to have complete appearance streams (AP entry with N key), which WeasyPrint-generated sticky notes do not provide.

**Workaround:** If you need PDF/A-compliant documents with comments, use inline comment rendering (disable "as sticky notes" option). Inline comments are rendered as regular HTML content and do not affect PDF/A compliance.

### PDF/UA-2 incomplete support

WeasyPrint 67.0 has incomplete support for PDF/UA-2 (ISO 14289-2:2024). The following issues are known:

| Issue | Description | Status |
|-------|-------------|--------|
| Structure destinations | Internal links must use structure destinations instead of page destinations (clause 8.8) | Requires WeasyPrint fix |
| PDF 2.0 namespace | Document element requires PDF 2.0 namespace (clause 8.2.5.2) | Requires WeasyPrint fix |
| Document-Span restriction | Document shall not contain Span directly (ISO 32005:2023) | Requires WeasyPrint fix |
| ListNumbering attribute | Lists with Lbl elements require ListNumbering attribute (clause 8.2.5.25) | Requires WeasyPrint fix |

The `pdfuaid:rev` metadata issue (four-digit year requirement) is fixed automatically via post-processing.

**Workaround:** Use `pdf/ua-1` instead of `pdf/ua-2` if full PDF/UA compliance is required.

## Upgrade

### Upgrade to weasyprint-service 67.0.0

WeasyPrint 67.0 introduced breaking changes in PDF variant support:

**Removed variant:**
- `pdf/a-4b` - no longer supported by WeasyPrint 67.0

**New variants added:**
- `pdf/a-1a` - Accessible PDF/A-1 (tagged, Unicode)
- `pdf/a-2a` - Accessible PDF/A-2 (tagged, Unicode, modern features)
- `pdf/a-3a` - Accessible PDF/A-3 (tagged, Unicode, file attachments)
- `pdf/a-4e` - PDF/A-4 for engineering documents (allows 3D, RichMedia)
- `pdf/a-4f` - PDF/A-4 with embedded files (requires attachments in document)
- `pdf/ua-2` - Accessible PDF for assistive technologies (ISO 14289-2:2024) - **partial support, see Known issues**

**Post-processing applied automatically:**

The extension applies post-processing to ensure PDF/A and PDF/UA compliance:

| PDF Variant | Post-processing |
|-------------|-----------------|
| PDF/A-1a, PDF/A-1b | Removes transparency masks (SMask, Mask) from images; adds RoleMap for TBody, THead, TFoot structure types; conformance level passed to processor |
| PDF/A-4e, PDF/A-4f, PDF/A-4u | Sets PDF version to 2.0; fixes `pdfaid:rev` to "2020"; ensures OutputIntent; handles conformance level |
| PDF/UA-2 | Fixes `pdfuaid:rev` to "2024" |

**Migration steps:**
1. If you were using `pdf/a-4b` variant, switch to `pdf/a-4f` or `pdf/a-4e` instead
2. Update any style packages that reference `PDF_A_4B` to use `PDF_A_4F` or `PDF_A_4E`
3. Update weasyprint-service Docker image to version 67.0.0 or later
4. Note: `pdf/a-4f` requires documents to have attachments (embedded files) per ISO 19005-4:2020 clause 6.9

### Upgrade from version 9.0.0 to 9.x.x

Due to a conflict of HTML elements' IDs in default cover page templates, their HTML/CSS were modified
to make IDs more unique and to replace IDs by classes where relevant. If you ever created your custom cover page templates
based on default ones, you can be interested in revising these changes and replicate them in your custom templates. Otherwise,
if you always used either default template(-s) or/and custom templates not inherited from default ones, no additional actions required.

### Upgrade from version 8.x.x to 9.0.0

In version 9.0.0 support for Polarion 2410 and older has been dropped. This extension supports only Polarion 2506.

PDF variant configuration has been moved to style package instead of using configuration properties.

### Upgrade from version 8.1.3 to 8.2.0

Sections Cover page, CSS, Header and Footer, Filename template were modified to split custom values (provided by user) and default values (provided by application, which can't be modified by user).
For a smooth migration from previous version we consider all already stored settings as custom, even if they contained default values, not to erase all your already stored settings.
Be aware that you'll need to tidy this up, storing explicitly either your custom values or switching to use default values.

### Upgrade from version 7.x.x to 8.0.0

In version 8.0.0 support for Polarion 2404 and older has been dropped. This extension supports only Polarion 2410.
Recommended version of WeasyPrint Service is 63.1.0.

### Upgrade from version 6.x.x to 7.0.0

In version 7.0.0 `/export-filename` REST API endpoint changed. As a result, if the endpoint has been used, it's required to adjust the calls accordingly.

`DocumentType` enum in `ExportParams` has been changed. As a result, if enum values have been used, it's required to adjust the calls accordingly.

Main package has been renamed from `ch.sbb.polarion.extension.pdf.exporter` to `ch.sbb.polarion.extension.pdf_exporter`. As a result, if the extension has been used in another OSGi bundles, it's required to adjust the package imports accordingly.

There was also added a CSS fragment for better display of Test Run pages in PDF, please add this fragment to your CSS definitions if they differ from default one, or update your CSS definitions via UI clicking button "Default" and later saving it. Here is this fragment:
```css
#polarion-rp-widget-content > .polarion-TestRunOverviewWidget-table > tbody > tr > td:first-child {
   width: 46% !important;
}
.polarion-TestRunOverviewWidget-buttonName {
   padding-top: 20px;
}
```

In version 7.1.0 the property `ch.sbb.polarion.extension.pdf-exporter.internalizeExternalCss` has been removed. `polarion.properties` should be updated accordingly.


### Upgrade from version 5.x.x to 6.0.0

In version 6.0.0 WeasyPrint CLI support was removed. As a result, if WeasyPrint CLI has been using to generate PDFs, it's required to switch to [WeasyPrint Service](#weasyprint-configuration).

The configuration properties `ch.sbb.polarion.extension.pdf-exporter.weasyprint.connector` and `ch.sbb.polarion.extension.pdf-exporter.weasyprint.executable` have been removed due to the removal of WeasyPrint CLI support. `polarion.properties` should be updated accordingly.

### Upgrade from version 4.x.x to 5.0.0
In version 5.0.0 not only label of configuration parameter "Fit images and tables to page width" was modified to be "Fit images and tables to page",
but also underlying parameter was renamed to reflect this change. As a result if you had "Fit images and tables to page width" ticked in your configuration prior to version 5.0.0,
after installation of this version you will have to go to configuration again and re-tick property "Fit images and tables to page", both on global repository level and on level of projects.

Another change is default CSS which was modified to reflect different possible paper sizes as well as additional styling for images to jump into next page if they can't be fully displayed on current one.
Thus please either reset your saved CSS into last version if you didn't have your own CSS definitions or merge your saved version with new default version.
