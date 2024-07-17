const TOOLBAR_HTML = `
    <table class="dleToolBarTable">
        <tr class="dleToolBarRow">
            <td class="dleToolBarTableCell" title="Export to PDF">
                <div class="dleToolBarSingleButton dleToolBarButton" onclick="PdfExporter.openPopup()">
                    <img class="polarion-MenuButton-Icon" src="/polarion/ria/images/dle/operations/actionPdfExport16.svg{BUNDLE_TIMESTAMP}" style="margin: 0">
                    <span style="margin: 0 5px 0 10px; font-weight: bold;">Export to PDF</span>
                </div>
            </td>
        </tr>
    </table>
`;

const ALTERNATE_TOOLBAR_HTML = `
    <table class="dleToolBarTable">
        <tr class="dleToolBarRow">
            <td ><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
            <td><img src="/polarion/ria/images/toolbar_splitter_gray.gif{BUNDLE_TIMESTAMP}" class="gwt-Image polarion-dle-ToolbarPanel-separator"></td>
            <td ><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
            <td class="dleToolBarTableCell" title="Export to PDF">
                <div class="dleToolBarSingleButton dleToolBarButton" onclick="PdfExporter.openPopup()">
                    <img class="polarion-MenuButton-Icon" src="/polarion/ria/images/dle/operations/actionPdfExport16.svg{BUNDLE_TIMESTAMP}" style="margin: 0">
                </div>
            </td>
        </tr>
    </table>
`;

const PdfExporterStarter = {
    bundleTimestamp: null,

    inject: function () {
        this.loadExtensionVersion()
            .then((response) => {
                this.bundleTimestamp = response?.bundleBuildTimestampDigitsOnly;
                this.injectAll(this.bundleTimestamp ? `?bundle=${this.bundleTimestamp}` : "");
            }).catch(() => {
                // Fallback to load resources without timestamp in case of error
                this.injectAll("");
            });
    },

    injectAll: function (bundleTimestampParam) {
        this.injectStyles("pdf-exporter-styles", `/polarion/pdf-exporter/css/pdf-exporter.css${bundleTimestampParam}`);
        this.injectStyles("pdf-micromodal-styles", `/polarion/pdf-exporter/css/micromodal.css${bundleTimestampParam}`);

        this.injectScript("pdf-micromodal-script", `/polarion/pdf-exporter/js/micromodal.min.js${bundleTimestampParam}`);
        this.injectScript("pdf-exporter-script", `/polarion/pdf-exporter/js/pdf-exporter.js${bundleTimestampParam}`);
    },

    injectStyles: function (id, stylesPath) {
        if (!top.document.getElementById(id)) {
            const styleElement = document.createElement("link");
            styleElement.id = id;
            styleElement.rel = "stylesheet";
            styleElement.type = "text/css";
            styleElement.href = stylesPath;
            top.document.head.appendChild(styleElement);
        }
    },

    injectScript: function (id, componentScriptPath) {
        if (!top.document.getElementById(id)) {
            const scriptElement = document.createElement("script");
            scriptElement.id = id;
            scriptElement.setAttribute("src", componentScriptPath);
            top.document.body.appendChild(scriptElement);
        }
    },

    injectToolbar: function (params) {
        const bundleTimestampParam = this.bundleTimestamp ? `?bundle=${this.bundleTimestamp}` : "";

        if (params?.alternate) {
            const toolbarParent = top.document.querySelector('div.polarion-content-container div.polarion-Container div.polarion-dle-Container > div.polarion-dle-Wrapper > div.polarion-dle-RpcPanel > div.polarion-dle-MainDockPanel div.polarion-rte-ToolbarPanelWrapper table.polarion-dle-ToolbarPanel tr');
            const toolbarContainer = document.createElement('td');
            toolbarContainer.innerHTML = ALTERNATE_TOOLBAR_HTML.replaceAll("{BUNDLE_TIMESTAMP}", bundleTimestampParam);
            toolbarParent.insertBefore(toolbarContainer, toolbarParent.querySelector('td[width="100%"]'));
        } else {
            const documentFrame = top.document.querySelector('div.polarion-content-container div.polarion-Container div.polarion-dle-Container>div.polarion-dle-Wrapper>div.polarion-dle-RpcPanel>div.polarion-dle-MainDockPanel div.polarion-dle-SplitPanel:last-child .polarion-dle-RichTextArea');
            const toolbarContainer = document.createElement('div');
            toolbarContainer.classList.add("dleToolBarContainer");
            toolbarContainer.innerHTML = TOOLBAR_HTML.replaceAll("{BUNDLE_TIMESTAMP}", bundleTimestampParam);
            documentFrame.parentNode.parentNode.prepend(toolbarContainer);
        }
    },

    loadExtensionVersion: function () {
        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open("GET", "/polarion/pdf-exporter/rest/internal/version", true);
            xhr.responseType = "json";
            xhr.send();

            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200 || xhr.status === 204) {
                        resolve(xhr.response);
                    } else {
                        reject(xhr)
                    }
                }
            };
            xhr.onerror = function () {
                reject();
            };
        });
    },
}

PdfExporterStarter.inject();