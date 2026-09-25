# Configuration

Full configuration reference for the PDF Exporter extension. See the [README](README.md) for a quick
overview and installation, the [quick start page](QUICK_START.md) for the most important steps, and
[UPGRADE.md](UPGRADE.md) for version-specific upgrade notes.

## Table of contents

- [Polarion configuration](#polarion-configuration)
  - [WeasyPrint configuration](#weasyprint-configuration)
  - [WeasyPrint API key](#weasyprint-api-key)
  - [PDF exporter extension to appear on a Document's properties pane](#pdf-exporter-extension-to-appear-on-a-documents-properties-pane)
  - [PDF Exporter view to open via button in toolbar](#pdf-exporter-view-to-open-via-button-in-toolbar)
    - [Deprecated configuration](#deprecated-configuration)
  - [PDF Exporter view to open in Live Reports](#pdf-exporter-view-to-open-in-live-reports)
  - [PDF Exporter view to open in Classic Wiki pages](#pdf-exporter-view-to-open-in-classic-wiki-pages)
  - [Configuring logs](#configuring-logs)
  - [Enabling CORS](#enabling-cors)
  - [Enabling webhooks](#enabling-webhooks)
  - [Bulk export (merge into single PDF)](#bulk-export-merge-into-single-pdf)
    - [Bulk Processing API key](#bulk-processing-api-key)
  - [Renderable image extensions](#renderable-image-extensions)
  - [External resources](#external-resources)
  - [Debug option](#debug-option)
    - [Timing report](#timing-report)
  - [Workflow function configuration](#workflow-function-configuration)
- [Extension configuration](#extension-configuration)
  - [Hiding the style packages of the global level](#hiding-the-style-packages-of-the-global-level)
  - [CSS for booklet layout](#css-for-booklet-layout)
- [Advanced configuration](#advanced-configuration)
  - [Asynchronous PDF Export: export jobs timeout](#asynchronous-pdf-export-export-jobs-timeout)
- [Performance and resource planning](#performance-and-resource-planning)

## Polarion configuration

### WeasyPrint configuration

This extension supports the use of WeasyPrint as a REST service within a Docker container, as implemented [here](https://github.com/SchweizerischeBundesbahnen/weasyprint-service).
To change WeasyPrint Service URL, adjust the following property in the `polarion.properties` file:

```properties
ch.sbb.polarion.extension.pdf-exporter.weasyprint.service=http://localhost:9080
```

### WeasyPrint API key

The WeasyPrint service can require an API key, which it does as soon as it is started with `API_KEY` set.
The extension then has to send that key, and it reads it from Polarion's secrets manager.

The key itself is never written into `polarion.properties`. The property holds the **name** of a secret:

```properties
ch.sbb.polarion.extension.pdf-exporter.weasyprint.apiKeySecret=weasyprint-api-key
```

Store the key under that name in the secrets manager of the Polarion installation. The properties file,
its backups and the About page then carry a name, not a credential.

An unset or empty property sends no key, which is what a service started without `API_KEY` expects.

**The key is only sent over https.** Where a key is configured and `weasyprint.service` names a plain
`http` address, the export is refused instead: a key is a reusable credential, and on plain http
everyone on the path keeps a copy of it. Give the service an `https` address, or clear the property
where the service needs no key. Without a key nothing changes, `http` keeps working as before, and
the rule holds for `localhost` too, since a certificate is what proves the transport rather than the
address. The About page reports this combination before anyone exports, because the version endpoint
carries no key and would otherwise look healthy.

Five failures are reported apart, since each one has a different fix:

| What the export says | What to do |
| --- | --- |
| requires an API key, none is configured | name the secret in the property above |
| could not read the WeasyPrint API key from the Polarion secret | check that the secret is readable for the Polarion process |
| is empty or does not exist | store a non-empty key under that secret name |
| rejected the configured API key | check the secret holds the key the service was started with |
| not sent over plain http | name the service with an https address |

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

Alternatively you can configure PDF Exporter such a way that a button to open the PDF Exporter view appears in the document editor's toolbar.

1. Open "Default Repository".
2. On the top of its navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Global administration page will be opened.
3. On the administration's navigation pane select Configuration Properties.
4. In editor of opened page add following line:
   ```properties
   scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/dle-toolbar.js"></script>
   ```
   This adds the button into Polarion's native document toolbar. The button is re-injected automatically when Polarion re-renders the toolbar (for example when the document is saved), so it stays in place.
5. Save changes by clicking 💾 Save

> [!TIP]
> **Adding more than one toolbar button.** `scriptInjection.dleEditorHead` is a single Polarion-wide property that holds exactly one value. To show several buttons in the document editor (for example the PDF, DOCX and StrictDoc exporters together, or this button alongside any other injected script), do **not** add separate `scriptInjection.dleEditorHead=` lines — the last one overrides the rest. Concatenate all the `<script>` tags into that single value instead:
> ```properties
> scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/dle-toolbar.js"></script><script src="/polarion/docx-exporter/js/dle-toolbar.js"></script><script src="/polarion/strictdoc-exporter/js/dle-toolbar.js"></script>
> ```

#### Deprecated configuration

The explicit `PdfExporterStarter.injectToolbar(...)` configuration still works but is **deprecated** in favor of the single-tag `dle-toolbar.js` form above (removal is planned for a future major version):

```properties
# Button in Polarion's native document toolbar — equivalent to the recommended dle-toolbar.js form above.
scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/starter.js"></script><script>PdfExporterStarter.injectToolbar({alternate: true});</script>
```
```properties
# A separate toolbar with the button placed above the document editing area.
scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/starter.js"></script><script>PdfExporterStarter.injectToolbar();</script>
```

### PDF Exporter view to open in Live Reports

Live Reports also can be converted to PDF with help of this extension.

First of all you need to inject appropriate JavaScript code into Polarion:

1. Open "Default Repository".
2. On the top of its navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Global administration page will be opened.
3. On the administration's navigation pane select Configuration Properties.
4. In editor of opened page add following line:
   ```properties
   scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/live-reports.js"></script>
   ```
5. Save changes by clicking 💾 Save

> [!NOTE]
> `scriptInjection.mainHead` is a single Polarion-wide property that holds exactly one value. If you also inject other scripts through it, concatenate all the `<script>` tags into that one value rather than adding separate `scriptInjection.mainHead=` lines — the last one overrides the rest.

The explicit `starter.js` form still works but is **deprecated** in favor of the single-tag `live-reports.js` form above (removal is planned for a future major version):

```properties
scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/starter.js"></script>
```

With the script injected, open a project and the Live Report you wish to export, then click "Expand Tools" on top of the page. An "Export to PDF" button appears in the report's toolbar (view mode only) — click it to open the PDF Exporter view in a popup and proceed with exporting the report. Be aware that in report's context limited set of properties are available for configuration in PDF popup, the rest of them are relevant only in Live Document context.

If a Bulk PDF Export widget on the report has rows selected, the button first asks whether to export the report or the selected items. See [Bulk PDF Export](USER_GUIDE.md#bulk-pdf-export) in the user guide.

Polarion collapses the report toolbar again on every page open. To keep it always expanded (so the "Export to PDF" button is permanently visible), opt in with the `data-expand-tools` attribute:

```properties
scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/live-reports.js" data-expand-tools="true"></script>
```

Alternatively (the pre-v13.5 way, still supported), the "Export to PDF Button" widget can be embedded into the report itself: click "Edit" in the report's toolbar, add an empty region on top of the report, place the cursor there, choose "PDF Export" tag on "Widgets" sidebar on right hand side of the page, find "Export to PDF Button" widget there and click it to add to the report. Then save the report clicking 💾 in a toolbar and return to view mode clicking "Back". The widget's button opens the same popup as the toolbar button.

### PDF Exporter view to open in Classic Wiki pages

Classic Wiki pages can be converted to PDF too. Inject the `classic-wiki.js` script the same way as for Live Reports, in step 4 above:

```properties
scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/classic-wiki.js"></script>
```

Open a Classic Wiki page and click "Expand Tools" on top of the page. An "Export to PDF" button appears in the page's toolbar. It opens the same popup as the Live Report button.

To keep the wiki toolbar always expanded, add `data-expand-tools="true"`, as for Live Reports. Both scripts share the one `scriptInjection.mainHead` value:

```properties
scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/live-reports.js" data-expand-tools="true"></script><script src="/polarion/pdf-exporter/js/classic-wiki.js" data-expand-tools="true"></script>
```

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

### Bulk export (merge into single PDF)

Bulk export allows merging multiple Polarion documents into a single PDF file. This feature requires the [Bulk Processing Service](https://github.com/SchweizerischeBundesbahnen/bulk-processing-service) to be running and accessible.

The "Merge all documents into a single PDF" checkbox in the bulk export popup is only visible when the Bulk Processing Service is available.

To configure the Bulk Processing Service URL, add the following line to `polarion.properties`:
```properties
ch.sbb.polarion.extension.pdf-exporter.bulk.processing.service=http://localhost:9070
```

Default value: empty. When the property is not set, bulk export (merge into a single PDF) is disabled and the "Merge all documents into a single PDF" checkbox is hidden.

The Bulk Processing Service must be deployed in the same Docker network as Polarion and the WeasyPrint service. Example:
```bash
docker run --detach \
  --name bulk-processing-service \
  --publish 9070:9070 \
  --env WEASYPRINT_SERVICE_URL=http://weasyprint-service:9080 \
  ghcr.io/schweizerischebundesbahnen/bulk-processing-service:latest
```

<a id="bulk-processing-api-key"></a>
#### Bulk Processing API key

The bulk processing service can require an API key, which it does as soon as it is started with `API_KEY` set.
As with the WeasyPrint service, the extension reads the key from Polarion's secrets manager, and the property
holds the **name** of a secret, never the key itself:

```properties
ch.sbb.polarion.extension.pdf-exporter.bulk.processing.apiKeySecret=bulk-processing-api-key
```

An unset or empty property sends no key. The key is only sent over `https`: where a key is configured and
`bulk.processing.service` names a plain `http` address, the export is refused rather than putting the
credential on the wire. A `401` from the service is reported apart from other errors, since it means either
that no key is configured or that the configured key was rejected, each with a different fix.

To serve the bulk processing service over `https` (required whenever an API key is used), start it with
`TLS_CERT_FILE` and `TLS_KEY_FILE` (and `TLS_KEY_PASSWORD` where the key is encrypted), then name it with an
`https` address in `bulk.processing.service`. See the [HTTPS section of the service's
README](https://github.com/SchweizerischeBundesbahnen/bulk-processing-service#https) for the full setup.

### Renderable image extensions

The exporter can embed certain file types as full-size images (raster formats, SVG, convertible diagrams like Visio).
For these file extensions the `thumbnail` query parameter is stripped so Polarion returns the full-size resource instead of an icon preview.
All other attachment types (spreadsheets, documents, archives, unknown formats) keep `thumbnail` so Polarion returns a small icon that can be shown inside an `<img>` tag.

The default set of renderable image extensions is: `png, jpg, jpeg, gif, bmp, svg, webp, avif, ico, cur, tif, tiff, vsdx`.

To override this list, adjust the following property in the `polarion.properties` file:

```properties
ch.sbb.polarion.extension.pdf-exporter.renderable.image.extensions=png, jpg, jpeg, gif, bmp, svg, webp, avif, ico, cur, tif, tiff, vsdx
```

### External resources

A document can reference an image, a font or a stylesheet by an absolute URL. The extension loads such a
resource and embeds it into the exported PDF. Because a document editor controls that URL, the request is
restricted. By default the extension rejects every address which is not public: loopback, private ranges,
link local addresses including the cloud metadata address `169.254.169.254`, and their IPv6 equivalents.
The Polarion server itself, as configured by `base.url`, always stays reachable.

To load resources from an internal host, list its origin explicitly:
```properties
ch.sbb.polarion.extension.pdf-exporter.externalResources.allowedOrigins=cdn.intranet,https://images.intranet:8443
```

An entry is written `[scheme://]host[:port]`, and what it leaves out is not compared:

| Entry | What it allows |
| --- | --- |
| `cdn.intranet` | that host under either scheme, on any port |
| `cdn.intranet:8443` | that host on port 8443, under either scheme |
| `https://cdn.intranet` | that host under https, on port 443 |
| `https://cdn.intranet:8443` | that host under https, on port 8443 |

A reference written `//host/path` takes the scheme of `base.url` first and the other one after, so it
reaches the host under whichever scheme an entry names.

The policy itself can be changed. The value is one of these three names, written exactly so:
```properties
# BLOCK_INTERNAL (default) - public addresses, the Polarion server and the allowed origins
# ALLOWLIST_ONLY           - only the Polarion server and the allowed origins
# ALLOW_ALL                - no restriction, this exposes the server's network to document editors
ch.sbb.polarion.extension.pdf-exporter.externalResources.policy=ALLOWLIST_ONLY
```

A loaded resource must be served as an image, a font or a stylesheet. Where the sender says nothing about
the content, or calls it `application/octet-stream`, the content itself decides and must be recognizable as
one of the three: a text of any other kind is refused, and so is a body nothing could be detected in.

A resource may not exceed 16 MB. To change the size limit:
```properties
ch.sbb.polarion.extension.pdf-exporter.externalResources.maxSizeMB=32
```

A resource which is not embedded is reported, not passed on silently. The Polarion log names it with the
reason, and the export writes it into the result of the conversion, which is what the message at the end of
an export shows. An image the policy refused becomes a transparent placeholder in the PDF.

Every conversion endpoint reports the same way, the one which takes raw HTML among them: the answer carries
`Blocked-Resources-Count` and `Blocked-Resources`, and carries neither when nothing was refused.

A stylesheet keeps its declarations whatever happens to its resources. An address nothing in the stylesheet
accounts for is replaced by `about:invalid`, so that the conversion service reads none of them: everything
else the stylesheet says still applies. A custom property holding an address, `--api: https://service.example`,
is such a case. Only an address written in CSS escapes which nothing accounts for still drops the whole
stylesheet: it names no place in the text to replace.

A CSS `@import` never survives, whatever it names and wherever it stands: it is removed where the stylesheet
was read, and renamed to an at-rule no renderer knows where it was not. An at-rule cannot be embedded, so
WeasyPrint would have to load it itself, past every check above. Reference such a stylesheet with a
`<link rel="stylesheet">` instead, the extension loads and embeds that one.

A configured JVM proxy (`http.proxyHost` and friends) is used for these requests. A proxy resolves the
host name itself, so a request routed through one cannot be pinned to a checked address. Such a request
is therefore only made for a host the configuration trusts as such: the Polarion server and the origins
listed above. Everything else is loaded directly, with the address check binding, or not at all. Hosts
in `http.nonProxyHosts` are loaded directly and keep the address check.

Note what this costs: where a proxy is configured for every destination, `BLOCK_INTERNAL` behaves
as `ALLOWLIST_ONLY`, since a proxied request is made only for the Polarion server and the origins
listed above.

A SOCKS proxy needs no such gate. It is no route the client plans, the JVM applies it at the socket, and
the socket is connected to the address the check approved, not to a host name the proxy would resolve.
So a request does traverse the SOCKS proxy, and its destination is still the vetted address.

Blocked resources are reported in the Polarion log. The document is exported without them.

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
2023-09-20 08:42:14,015 [ForkJoinPool.commonPool-worker-2] INFO  util.ch.sbb.polarion.extension.pdf_exporter.util.HtmlLogger - Timing report was stored in file /tmp/pdf-exporter10000032892830031969/timing-report-1234567890.txt
```

Here you can find out in which files HTML and timing report were stored.

#### Timing report

When debug mode is enabled, a detailed timing report is generated showing the duration of each PDF generation stage.
This helps identify performance bottlenecks. The report includes:

- Summary statistics (HTML size, PDF size, page count, throughput)
- Timing breakdown with visual progress bars
- Time by category (HTML Processing, WeasyPrint Conversion, PDF Post-processing, Cover Page)
- Slowest stages with performance indicators
- Execution timeline

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
4. Sections Cover page, Header and Footer, and Filename template use either the default values (can't be edited) or the custom ones. The custom values are always saved,
   and the choice above them decides which ones an export uses. The tab of the values not in use is disabled. `Copy from default` fills the custom values with the default ones. On the Cover page section, `Copy`
   fills them with the predefined template selected next to it. `Compare with default` shows the default and the custom values side by side, and the section tells when
   the default values changed since the custom ones were copied from them. `Mark as reviewed` takes the current default values as reviewed, to be saved
   with the configuration. A cover page remembers the predefined template it was copied from, and compares and reviews against that template. An empty custom filename template uses the default one.
   Section CSS appends the custom CSS to the default CSS, so a newer default CSS reaches every export. `Use custom CSS only` uses the custom CSS alone, and the section
   then offers to put the default CSS in front of it.
5. To change configuration of PDF Exporter extension just edit corresponding section and press `Save` button.

### Hiding the style packages of the global level

By default a project offers its own style packages plus the ones defined on the global level. Section
`Style Package` of the administration pages carries a `Change visibility` button next to `Add new`. It opens
a dialog which explains the two levels, offers the switch `Hide style packages defined on the global level`
and stores it with `Change`. The switch is off - the behavior described above - unless an administrator
ticks it. It is a configuration of its own, above all style packages of the scope, so it belongs to no
single style package.

With the switch on for a project:

1. Only the style packages defined on that project are listed, on the export dialogs, on the side panel and
   on the administration pages `Style Package` and `Style Package Weights`.
2. A style package of the global level can no longer be used by its name either, for example in the
   [workflow function](#workflow-function-configuration). The export reports it as unavailable.
3. Documents of several projects exported at once (Bulk PDF Export) can only share the style packages of the
   global level, so as soon as one of the projects hides them only `Default` is offered.

The style package named `Default` can never be missing. While a project that hides the global level has no
`Default` of its own, that name stands for the built-in values of the extension - not for the `Default` of the
global level, which is hidden like every other style package there. Save a `Default` on the project to decide
what it contains.

Ticked on the global scope the switch applies to every project which does not set it itself, which makes it
the installation-wide default. The global scope keeps listing its own style packages.

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

## Performance and resource planning

The weasyprint-service container requires a minimum of **2 GB** of memory. Actual consumption depends on document size, images, and concurrency.

**Benchmarks** (generated Polarion documents):

| Document | Work Items | Images | Peak RAM | PDF Size |
|----------|-----------|--------|----------|----------|
| Small with images | 500 | 0–2 random SVG per WI (100–300 px) | 1.8 GB | 1.0 MB |
| Large without images | ~2000 | none | 2.6 GB | 4.3 MB |
| Large with images | ~2000 | 1 SVG per WI (1920×1080 px) | 2.8 GB | 18 MB |

Each concurrent conversion requires additional memory proportional to the values above. For example, 3 concurrent conversions of large documents with images: approximately `1.7 + 3 × 1.2 ≈ 5.3 GB`.

After a conversion completes, container memory (RSS) may not decrease — this is normal Python/glibc behavior, not a leak. To reclaim memory after traffic spikes, enable `RECLAIM_MEMORY_AFTER_CONVERSION=true` in weasyprint-service (runs `gc.collect` + `malloc_trim` after each conversion). See [weasyprint-service README](https://github.com/SchweizerischeBundesbahnen/weasyprint-service#post-conversion-memory-reclamation) for details.
