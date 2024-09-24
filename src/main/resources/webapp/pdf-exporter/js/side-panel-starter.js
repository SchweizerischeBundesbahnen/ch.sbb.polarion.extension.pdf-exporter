const PdfExporterSidePanelStarter = {
    timestampParam: `?timestamp=${Date.now()}`,

    injectAll: function () {
        this.injectScript("common-script", `/polarion/pdf-exporter/ui/generic/js/common.js${this.timestampParam}`);
        this.injectScript("ExportParams-script", `/polarion/pdf-exporter/ui/js/modules/ExportParams.js${this.timestampParam}`, 'module');
        this.injectScript("export-common-script", `/polarion/pdf-exporter/ui/js/export-common.js${this.timestampParam}`);
        this.injectScript("export-pdf-script", `/polarion/pdf-exporter/ui/js/export-pdf.js${this.timestampParam}`);
    },


    injectScript: function (id, componentScriptPath, type = "text/javascript") {
        if (top.document.body && !top.document.getElementById(id)) {
            const scriptElement = document.createElement("script");
            scriptElement.id = id;
            scriptElement.setAttribute("src", componentScriptPath);
            scriptElement.setAttribute("type", type);
            top.document.body.appendChild(scriptElement);
        }
    },
}

PdfExporterSidePanelStarter.injectAll();