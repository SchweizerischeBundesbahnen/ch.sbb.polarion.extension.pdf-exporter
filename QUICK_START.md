# Quick start

### Run WeasyPrint in Docker

Start WeasyPrint as a REST service within a Docker container, as described [here](https://github.com/SchweizerischeBundesbahnen/weasyprint-service).

## Deploy PDF Exporter to Polarion

Take file `ch.sbb.polarion.extension.pdf-exporter-<version>.jar` from page of [releases](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/releases)
and copy it to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.pdf-exporter/eclipse/plugins` folder.

## Specify required properties in polarion.properties file

Add following properties to file `polarion.properties`:

```properties
com.siemens.polarion.rest.enabled=true
com.siemens.polarion.rest.swaggerUi.enabled=true
ch.sbb.polarion.extension.pdf-exporter.weasyprint.service=http://localhost:9080
```

## Restart Polarion

Stop Polarion.  
Delete the `<polarion_home>/data/workspace/.config` folder.  
Start Polarion.

## Configure PDF Exporter for Live Reports

On admin pane of Default Repository select menu Configuration Properties, add following line and save your changes:

```properties
scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/starter.js"></script>
```

## Configure PDF Exporter for Live Documents

On admin pane of appropriate project select menu "Documents & Pages ➙ Document Properties Sidebar", insert following new line inside `sections`-element and save your changes:

```xml
<sections>
  …
  <extension id="pdf-exporter" label="PDF Exporter" />
</sections>
```

## Ready to go

PDF Exporter is now installed and configured. You can now open a Live Document and on Documents Sidebar you will see PDF Exporter section. Also open About page of PDF Exporter on admin pane
and make sure that there are no errors in Extension configuration status table.