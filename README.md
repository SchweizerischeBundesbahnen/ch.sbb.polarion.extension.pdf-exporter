# Polarion ALM extension to convert Documents to PDF files

This Polarion extension provides possibility to convert Polarion Documents to PDF files.
This is an alternative to native Polarion's solution.
The extension uses [WeasyPrint](https://weasyprint.org/) as a PDF engine and requires it to run in [Docker as Service](#weasyprint-configuration).

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

### WeasyPrint Configuration

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

Cross-Origin Resource Sharing could be enabled using standard configuration of Polarion REST API. In `polarion.properties` the following lines should be added:
```properties
com.siemens.polarion.rest.enabled=true
com.siemens.polarion.rest.cors.allowedOrigins=http://localhost:8888,https://anotherdomain.com
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
2023-09-20 08:42:13,911 [ForkJoinPool.commonPool-worker-2] INFO  ch.sbb.polarion.extension.pdf.exporter.util.HtmlLogger - Original HTML fragment provided by Polarion was stored in file /tmp/pdf-exporter10000032892830031969/original-4734772539141140796.html
2023-09-20 08:42:13,914 [ForkJoinPool.commonPool-worker-2] INFO  ch.sbb.polarion.extension.pdf.exporter.util.HtmlLogger - Final HTML page obtained as a result of PDF exporter processing was stored in file /tmp/pdf-exporter10000032892830031969/processed-5773281490308773124.html
```

Here you can find out in which files HTML was stored.

### Enabling Internalization of CSS Links

The converting HTML can contain some external CSS links referencing Polarion Server, like:

```html
<link rel="stylesheet" href="/polarion/diff-tool-app/ui/app/_next/static/css/3c374f9daffd361a.css" data-precedence="next">
```

In case the Polarion Server is not reachable from the WeasyPrint Service, such links cannot be successfully resolved during the WeasyPrint PDF transformation. The solution is to replace external CSS <link> elements with internal CSS <style> tags containing the CSS content embedded into the HTML document. By default, CSS link internalization is disabled. To enable internalization of CSS links, it is necessary to activate the following property in file `polarion.properties`:

```properties
ch.sbb.polarion.extension.pdf-exporter.internalizeExternalCss=true
```

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
   For the options details please refer [plugin documentation](docs/pdf-exporter.pdf).

## REST API
This extension provides REST API. OpenAPI Specification can be obtained [here](docs/openapi.json).

## Known issues

All good so far.

## Upgrade

### Upgrade from version 5.x.x to 6.0.0

In version 6.0.0 WeasyPrint CLI support was removed. As a result, if WeasyPrint CLI has been using to generate PDFs, it's required to switch to [WeasyPrint Service](#weasyprint-configuration).

### Upgrade from version 4.x.x to 5.0.0
In version 5.0.0 not only label of configuration parameter "Fit images and tables to page width" was modified to be "Fit images and tables to page",
but also underlying parameter was renamed to reflect this change. As a result if you had "Fit images and tables to page width" ticked in your configuration prior to version 5.0.0,
after installation of this version you will have to go to configuration again and re-tick property "Fit images and tables to page", both on global repository level and on level of projects.

Another change is default CSS which was modified to reflect different possible paper sizes as well as additional styling for images to jump into next page if they can't be fully displayed on current one.
Thus please either reset your saved CSS into last version if you didn't have your own CSS definitions or merge your saved version with new default version.
