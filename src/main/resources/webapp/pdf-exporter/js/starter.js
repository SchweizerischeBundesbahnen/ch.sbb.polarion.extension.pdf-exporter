/*
 * Thin starter — extension-specific config only. The reusable engine (DLE toolbar selectors +
 * self-healing re-injection via MutationObserver) lives in generic:
 * /polarion/pdf-exporter/ui/generic/js/dle-toolbar-starter.js
 *
 * The dleEditorHead config is unchanged: it still loads this script and calls
 * PdfExporterStarter.injectToolbar({...}); the bootstrap below pulls the engine and queues
 * the call until it is ready.
 *
 * DEPRECATED: this script is superseded by two dedicated single-tag injectors —
 * /polarion/pdf-exporter/js/dle-toolbar.js for the document-editor toolbar button (replaces
 * PdfExporterStarter.injectToolbar(...)), and /polarion/pdf-exporter/js/live-reports.js for the
 * Live Reports export button (replaces loading starter.js via scriptInjection.mainHead). It keeps
 * working for backward compatibility; removal is a future major bump.
 */
(function () {
    const timestampParam = `?timestamp=${Date.now()}`;

    // Extension web-context base, derived from this script's own URL (…/polarion/<ext>/js/starter.js)
    // so nothing below hardcodes the /polarion/<ext>/ segment.
    const EXT_BASE = (document.currentScript && document.currentScript.src || '').replace(/js\/starter\.js.*$/, '') || '/polarion/pdf-exporter/';

    // The export dialog is a React module of the pdf-exporter-app webapp, imported on click. It mounts
    // itself into a shadow root of its own, so nothing has to be injected into the page for it: the
    // micromodal library and the six generic control stylesheets the legacy popup needed are gone.
    // The timestamp is captured once per page load, so a click reuses the module the previous click
    // loaded while an updated extension is still picked up on the next page open.
    const POPUP_MODULE = `/polarion/pdf-exporter-app/ui/app/assets/export-popup.js${timestampParam}`;
    const openPopup = documentType => `import('${POPUP_MODULE}')
                                .then(module => module.openExportPopup({documentType: '${documentType}'}))
                                .catch(console.error);`;

    const TOOLBAR_HTML = `
        <table class="dleToolBarTable">
            <tr class="dleToolBarRow">
                <td class="dleToolBarTableCell" title="Export to PDF">
                    <div class="dleToolBarSingleButton dleToolBarButton"
                    onclick="${openPopup('LIVE_DOC')}">
                        <img class="polarion-MenuButton-Icon" src="/polarion/ria/images/dle/operations/actionPdfExport16.svg${timestampParam}" style="margin: 0">
                        <span style="margin: 0 5px 0 10px; font-weight: bold;">Export to PDF</span>
                    </div>
                </td>
            </tr>
        </table>`;

    const ALTERNATE_TOOLBAR_HTML = `
        <table class="dleToolBarTable">
            <tr class="dleToolBarRow">
                <td ><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
                <td><img src="/polarion/ria/images/toolbar_splitter_gray.gif${timestampParam}" class="gwt-Image polarion-dle-ToolbarPanel-separator"></td>
                <td ><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
                <td class="dleToolBarTableCell" title="Export to PDF">
                    <div class="dleToolBarSingleButton dleToolBarButton"
                    onclick="${openPopup('LIVE_DOC')}">
                        <img class="polarion-MenuButton-Icon" src="/polarion/ria/images/dle/operations/actionPdfExport16.svg${timestampParam}" style="margin: 0">
                    </div>
                </td>
            </tr>
        </table>`;

    // Current project id, parsed from the Polarion location hash (…#/project/<id>/…), same convention
    // as ExportContext. Reads the top frame's hash (this script runs in the editor iframe). Null when
    // there is no project scope (only global roles then apply).
    function getCurrentProjectId() {
        try {
            const hash = (top && top.location && top.location.hash) || window.location.hash || '';
            const match = /project\/([^/]+)\//.exec(decodeURI(hash));
            return match ? match[1] : null;
        } catch (e) {
            return null;
        }
    }

    // Engine permission endpoint: the generic engine GETs this URL, expects JSON { permitted: boolean },
    // injects the button disabled first, then enables it when permitted (fail-closed on non-OK/error).
    // Server-side authorization is still enforced (403) regardless of the button state.
    function exportPermissionUrl() {
        const projectId = getCurrentProjectId();
        return `${EXT_BASE}rest/internal/permissions/export` + (projectId ? `?projectId=${encodeURIComponent(projectId)}` : '');
    }

    // Expose the global immediately; queue injectToolbar calls until the engine is loaded.
    let starter = null, myOrder;
    const pending = [];
    window.PdfExporterStarter = {
        /**
         * @deprecated Use the single-tag injector instead:
         *   <script src="/polarion/pdf-exporter/js/dle-toolbar.js"></script>
         * Kept for backward compatibility; removal is planned for a future major version.
         * @param {{alternate: boolean}|undefined} params
         */
        injectToolbar: function (params) {
            if (myOrder === undefined) {
                // Capture config-execution order (this stub runs synchronously in dleEditorHead
                // order) so multiple toolbar buttons keep a stable left-to-right order on re-render.
                const seq = top.__genericDleToolbarSeq || (top.__genericDleToolbarSeq = { n: 0 });
                myOrder = seq.n++;
            }
            starter ? starter.injectToolbar(params) : pending.push(params);
        }
    };

    const engine = document.createElement('script');
    engine.src = `${EXT_BASE}ui/generic/js/dle-toolbar-starter.js${timestampParam}`;
    engine.onload = function () {
        const generic = window.GenericDleToolbarStarter;
        if (!generic) {
            console.error("pdf-exporter: GenericDleToolbarStarter is not available after the engine loaded — toolbar injection skipped.");
            pending.length = 0;
            return;
        }
        // No stylesheet is injected here any more: the export dialog styles itself inside its own shadow
        // root, and the button's .dleToolBar* rules (incl. the disabled state) come from generic's
        // css/dle-toolbar.css, which the toolbar engine injects itself.

        // The engine owns the disabled state: it injects the button disabled, runs permissionCheck,
        // then enables it if permitted (or keeps it disabled on failure — fail-closed), and preserves
        // that state across the toolbar's self-heal re-renders.
        starter = generic.create({
            markerId: 'pdf-exporter-toolbar-injected',
            alternateHtml: ALTERNATE_TOOLBAR_HTML,
            defaultHtml: TOOLBAR_HTML,
            order: myOrder,
            permissionCheckUrl: exportPermissionUrl()
        });
        pending.forEach(params => starter.injectToolbar(params));
        pending.length = 0;
    };
    engine.onerror = function () {
        console.error("pdf-exporter: failed to load the DLE toolbar engine — toolbar injection skipped.");
        pending.length = 0;
    };
    document.head.appendChild(engine);
})();
