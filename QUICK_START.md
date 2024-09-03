# PDF Exporter quick start

### Run WeasyPrint in Docker

Start WeasyPrint as a REST service within a Docker container, as described [here](https://github.com/SchweizerischeBundesbahnen/weasyprint-service).

## Deploy PDF Exporter to Polarion

Take file `ch.sbb.polarion.extension.pdf-exporter-<version>.jar` from page of [releases](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/releases)
and copy it to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.pdf-exporter/eclipse/plugins` folder.

## Specify required properties in polarion.properties file

Open file `<polarion_home>/etc/polarion.properties` in a file editor of your choice and add following properties to it:

```properties
com.siemens.polarion.rest.enabled=true
com.siemens.polarion.rest.cors.allowedOrigins=<ANY ORIGINS WHICH ALLOWED TO REACH REST ENDPOINTS, COMMA SEPARATED>
ch.sbb.polarion.extension.pdf-exporter.weasyprint.service=http://localhost:9080
```

## Restart Polarion

Restart Polarion services, either yourself if you have such rights, or ask a system administrator.

## Configure PDF Exporter for Live Reports

On admin pane of Default Repository select menu Configuration Properties, add following line and save your changes:

```properties
scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/starter.js"></script>
```

## Configure PDF Exporter for Live Documents

On admin pane of appropriate project select menu "Documents & Pages ➙ Document Properties Sidebar", insert following new line inside `sections`-element and save your changes:

```xml
…
<extension id="pdf-exporter" label="PDF Exporter" />
…
```

## Ready to go

You are now ready to use PDF Exporter in your Polarion.