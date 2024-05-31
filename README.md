# Polarion ALM extension to convert Documents to PDF files

This Polarion extension provides possibility to convert Polarion Documents to PDF files.
This is an alternative to native Polarion's solution.
The extension uses [WeasyPrint](https://weasyprint.org/) as a PDF engine and requires it either to be installed as a system's command-line tool or to run from Docker container (as CLI or as Service, see below).

## Build

PDF exporter extension can be produced using maven:
```bash
mvn clean package
```

## Installation to Polarion

To install the extension to Polarion `ch.sbb.polarion.extension.pdf-exporter-<version>.jar`
should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.pdf-exporter/eclipse/plugins`
It can be done manually or automated using maven build:
```bash
mvn clean install -P polarion2404,install-to-local-polarion
```
For automated installation with maven env variable `POLARION_HOME` should be defined and point to folder where Polarion is installed.

Changes only take effect after restart of Polarion.

## Polarion configuration

### WeasyPrint Configuration

#### WeasyPrint CLI

To run WeasyPrint as a system's command-line tool specify following properties in file `<POLARION_HOME>/etc/polarion.properties`:

```properties
ch.sbb.polarion.extension.pdf-exporter.weasyprint.connector=cli
ch.sbb.polarion.extension.pdf-exporter.weasyprint.executable=weasyprint
ch.sbb.polarion.extension.pdf-exporter.weasyprint.pdf.variant=pdf/a-2b
```

And then install WeasyPrint. On Linux system it can be done following way:

```bash
pip install weasyprint
```

For more information on WeasyPrint Installation see [these instructions](https://doc.courtbouillon.org/weasyprint/stable/first_steps.html#installation)

#### WeasyPrint CLI in Docker

This extension supports using WeasyPrint running in Docker container. This feature can be switched on with help of following properties in file `<POLARION_HOME>/etc/polarion.properties`:

```properties
ch.sbb.polarion.extension.pdf-exporter.weasyprint.connector=cli
ch.sbb.polarion.extension.pdf-exporter.weasyprint.executable=docker run --interactive --rm docker.io/library/weasyprint:61.2
ch.sbb.polarion.extension.pdf-exporter.weasyprint.pdf.variant=pdf/a-2b
```

#### WeasyPrint as Service in Docker

This extension supports using WeasyPrint running as a REST Service in Docker container. This feature can be switched on with help of following properties in file `<POLARION_HOME>/etc/polarion.properties`:

```properties
ch.sbb.polarion.extension.pdf-exporter.weasyprint.connector=service
ch.sbb.polarion.extension.pdf-exporter.weasyprint.service=http://localhost:9080
ch.sbb.polarion.extension.pdf-exporter.weasyprint.pdf.variant=pdf/a-2b
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

### PDF exporter view to open via button in toolbar

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

### PDF exporter view to open in Live Reports

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
As a result report's toolbar will appear. Click "Edit" button in a toolbar, as a result the report will be switched into an edit mode. Add an empty region on top
of the report, place cursor there, choose "Generic" tag on "Widgets" sidebar on right hand side of the page, find "Export to PDF Button" widget there and click it
to add to the report. Then save a report clicking 💾 in a toolbar and then return to a view mode clicking "Back" button. When you click "Export to PDF" button just added
to the report, PDF Exporter view will be opened in a popup and you will be able to proceed with exporting the report to PDF. Be aware that in report's context limited
set of properties are available for configuration in PDF popup, the rest of them are relevant only in Live Document context.

### Configuring Logs

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

Cross-Origin Resource Sharing could be enabled using standard configuration of Polarion REST API. In `<POLARION_HOME>/etc/polarion.properties` the following lines should be added:
```properties
com.siemens.polarion.rest.enabled=true
com.siemens.polarion.rest.cors.allowedOrigins=http://localhost:8888,https://anotherdomain.com
```

### Debug option

This extension makes intensive HTML processing to extend similar standard Polarion functionality. There is a possibility to log
original and resulting HTML to see potential problems in this processing. This logging can be switched on (`true` value)
and off (`false` value) with help of following property in file `<POLARION_HOME>/etc/polarion.properties`:

```properties
ch.sbb.polarion.extension.pdf-exporter.debug=true
```

If HTML logging is switched on, then in standard polarion log file there will be following lines:

```text
2023-09-20 08:42:13,911 [ForkJoinPool.commonPool-worker-2] INFO  ch.sbb.polarion.extension.pdf.exporter.util.HtmlLogger - Original HTML fragment provided by Polarion was stored in file /tmp/pdf-exporter10000032892830031969/original-4734772539141140796.html
2023-09-20 08:42:13,914 [ForkJoinPool.commonPool-worker-2] INFO  ch.sbb.polarion.extension.pdf.exporter.util.HtmlLogger - Final HTML page obtained as a result of PDF exporter processing was stored in file /tmp/pdf-exporter10000032892830031969/processed-5773281490308773124.html
```

Here you can find out in which files HTML was stored.

## Extension Configuration

1. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
2. On the administration's navigation pane select `PDF Export`. There are 5 sub-menus with different configuration options for PDF Exporter.
3. For 3 of these options (Cover page, Header and Footer and Localization) `Quick Help` section available with option short description. For the rest 2
   (Style package, Stylesheets) there's no `Quick Help` section, but their content is self-evident.
4. To change configuration of PDF exporter extension just edit corresponding section and press `Save` button.

## Usage

1. Open a document in Polarion.
2. In the toolbar choose Show Sidebar ➙ Document Properties.
3. Choose desired options in the `PDF Exporter` block and click `Export to PDF`.
   For the options details please refer [plugin documentation](https://polarion.sbb.ch/polarion/#/project/mcTestInt/wiki/Specification/PDF%20Exporter?selection=MCTI-426).

## Known issues

### SVG rendering issue
#### Details
Weasyprint doesn't fully support all SVG features. One of the most frequently used feature by Polarion which isn't supported by Weasyprint is ['foreignObject' element](https://www.w3.org/TR/SVG11/extend.html#ForeignObjectElement). This leads to some visual bugs in resulting pdf (missing font colors, rich text formatting etc.)
#### Solution
Usage of `WeasyPrint as Service in Docker` (see above) is suggested. It has built-in SVG to PNG images conversion using Chromium browser (which supports more SVG features, including `foreignObjects`).

## Upgrade

### Upgrade from version 4.x.x to 5.0.0
In version 5.0.0 not only label of configuration parameter "Fit images and tables to page width" was modified to be "Fit images and tables to page",
but also underlying parameter was renamed to reflect this change. As a result if you had "Fit images and tables to page width" ticked in your configuration prior to version 5.0.0,
after installation of this version you will have to go to configuration again and re-tick property "Fit images and tables to page", both on global repository level and on level of projects.

Another change is default CSS which was modified to reflect different possible paper sizes as well as additional styling for images to jump into next page if they can't be fully displayed on current one.
Thus please either reset your saved CSS into last version if you didn't have your own CSS definitions or merge your saved version with new default version.

## Changelog

| Version | Changes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| v5.0.2  | Added CSS configuration to hide non-printing elements                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v5.0.1  | Added defaults for size and page parameters: A4 and Portrait                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| v5.0.0  | * Attribute "Fit images and tables to page width" is modified to be "Fit images and tables to page" and at the same time logic is modified to try and fit images not only to page width but also to page height (it's a breaking change, see "Upgrading instructions" section for further instructions).<br>* Exclude report parameters area from being exporting to pdf for live reports                                                                                                                                                                                                                                                                                                                                            |
| v4.5.2  | Help colorized                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| v4.5.1  | Help updated                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| v4.5.0  | PDF/A variants now supported both for weasyprint CLI and weasyprint service                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| v4.4.0  | Polarion 2404 is supported                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| v4.3.1  | Fixed bugs in about page                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v4.3.0  | * Support of WeasyPrint as a Service has been added<br/>* Fixed export to PDF from default space                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| v4.2.0  | * Added BASELINE_NAME and REVISION_AND_BASELINE_NAME placeholders<br/>* StringIndexOutOfBoundsException on LiveDoc rendering fix<br/>* Hide hint for unsupported SVG 1.1 features                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| v4.1.1  | * HtmlUtils class moved to generic application.<br/>* Styling fix.<br/>* Fixed base URL prefix.<br/>* Debug information for raw HTMl export                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| v4.1.0  | * Current configuration status to About page added.<br/>* Expanded fields for document filename template<br/>* Icon for LiveReport button changed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| v4.0.0  | * Using enums for DocumentType<br/>* Added endpoint for raw html to pdf export                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| v3.12.0 | User-friendly message about deactivated configuration properties on the Swagger UI page.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v3.11.1 | Temporary fix for starting of clearing job                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| v3.11.0 | About page supports configuration help and icon                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v3.10.1 | delivery-external maven profile does not include EuroSpec cover page                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v3.10.0 | * Fixed wiki page rendering<br/>* Updated GUI to use asynchronous PDF export API<br/>* Added cleanup async jobs                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v3.9.0  | Default font for PDF export is Arial<br/> Wiki pages now rendered using internal renderer directly                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| v3.8.1  | Fix for saving of default settings in read only transaction                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| v3.8.0  | * Added support Velocity variables and placeholders for document filename<br/>* Enabled PDF generation for live reports on global level<br/>* Added Asynchronous PDF generation API                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| v3.7.0  | * Fix for export of Wiki Page<br/>* removed wikiFolder and wikiName from ExportParams                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v3.6.0  | Evaluation of Velocity expression is extended with $page variable for LiveReports                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| v3.5.0  | Paper size is added as style package option.<br/>* Fixed the logic for searching executor in windows with WSL                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v3.4.0  | Support exporting Live Reports to PDF                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v3.3.0  | * Evaluate Velocity expressions in Cover Page and Header/Footer templates<br/>* DOCUMENT_ID placeholder has been added                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| v3.2.3  | Fix images which width is bigger than the page width but their width is not explicitly specified in any of attributes and factual value is used                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v3.2.1  | Page break styling fix                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| v3.2.0  | * Added support of HTML Presentational Hints.<br/>* Added possibility to re-define resulted file name.<br/>* Marking referenced WorkItems extracted as a configurable property                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| v3.1.1  | Configuration properties in about page                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| v3.1.0  | Added possibility to open PDF exporter view in popup (via toolbar button) implemented with help of JS-injection approach provided by Polarion                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v3.0.0  | * Cover page endpoint renamed to correspond menu item name.<br/>* Fix named configurations issue when extension is installed from the scratch                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v2.4.0  | New feature: on fly converting of cover page images located in SVN and referenced in CSS into base64 representation and embedding them into CSS directly                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v2.3.0  | * Multiple predefined templates for cover page.<br/>* Extended description in quick help sections of cover page and header/footer configurations to clarify format of special variables and document's custom fields.<br/>* If no value was obtained/calculated for special variable or document's custom fields then blank string will be inserted instead of special variable or custom field ID.<br/>* All '&amp;nbsp;' occurrences are now removed from source HTML before converting into PDF. This is done to exclude situations when they are a reason of certain strings be too wide going outside of page boundaries. As a side effect in some cases WorkItems ID and title can be rendered on different lines of document. |
| v2.2.0  | * Cover page generation added together with cover page configuration.<br/>* Custom styling of referenced Work Items.<br/>* Cut local URLs + filtering Workitem roles working together fix.<br/>* Added variables injection into CSS of document and cover page.<br/>* Named settings moved to generic application                                                                                                                                                                                                                                                                                                                                                                                                                    |
| v2.1.1  | * Custom dropdowns are used to visually mark project's scope configurations inherited from global scope.<br/>* Adjust REST endpoints and use Polarion services via PolarionService aggregator/proxy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v2.1.0  | * New feature: added DOCUMENT_STATUS variable to header/footer settings.<br/>* "Fit to page width" checkbox is renamed to "Fit images and tables to page width" to reduce confusion with real implementation.<br/>* Fixed an issue when exporter form is initialized in case when no style packages ever configured on admin site.<br/>* Added tests running real WeasyPrint instance to test if document elements didn't go out of page boundaries.                                                                                                                                                                                                                                                                                 |
| v2.0.0  | Added named settings and ability to combine them into style packages. Breaking change modifying the way settings are stored!                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| v1.2.14 | Fix too wide lines generated in PDF caused by unnecessary &nbsp; characters in source HTML                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| v1.2.13 | Document's last revision is displayed instead of "HEAD" literal in case if no revision was specified                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v1.2.12 | Added processing IDs of document's custom fields as placeholders in header/footer. Added an option to run WeasyPrint from docker image.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| v1.2.11 | Displaying Polarion's HEAD revision logic fixed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v1.2.10 | Getting document's title logic fixed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v1.2.9  | Fix for Table of Figures - arbitrary caption's label                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v1.2.8  | HTML logging added                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| v1.2.7  | Items of ToF & ToT as hyperlinks, fix for comments inside captions to figures and tables                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v1.2.6  | Fix for "page-break-inside: avoid"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| v1.2.5  | Table of content issues fixed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v1.2.4  | Non-default icons rendering fix                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v1.2.3  | Table of figures/tables fix                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| v1.2.2  | Table of figures/tables generation added                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v1.2.1  | Numbered lists processing fix                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v1.2.0  | Fixing API to call from python                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| v1.1.0  | Different refactorings                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| v1.0.0  | Initial release                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
