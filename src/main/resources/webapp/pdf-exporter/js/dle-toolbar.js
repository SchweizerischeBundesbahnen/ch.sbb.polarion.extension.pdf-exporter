/*
 * One-tag DLE toolbar injector — the simple, recommended way to add the PDF Exporter button to
 * Polarion's native document editor toolbar. Configure a single script tag:
 *
 *   scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/dle-toolbar.js"></script>
 *
 * It loads the thin starter (which pulls the shared self-healing engine) and injects the toolbar
 * button, so no separate <script>PdfExporterStarter.injectToolbar({alternate: true});</script> tag
 * is needed. The deprecated explicit-injectToolbar config keeps working; see starter.js.
 */
(function () {
    function injectToolbar() {
        window.PdfExporterStarter.injectToolbar({alternate: true});
    }

    // starter.js may already be loaded (e.g. also configured via scriptInjection.mainHead) —
    // reuse it rather than loading it again.
    if (window.PdfExporterStarter) {
        injectToolbar();
        return;
    }

    // Load starter.js relative to this script's own URL (…/polarion/<ext>/js/dle-toolbar.js),
    // so nothing here hardcodes the /polarion/<ext>/ segment.
    const starterSrc = (document.currentScript && document.currentScript.src || '')
        .replace(/dle-toolbar\.js.*$/, 'starter.js') || '/polarion/pdf-exporter/js/starter.js';
    const script = document.createElement('script');
    script.src = starterSrc;
    script.onload = function () {
        if (window.PdfExporterStarter) {
            injectToolbar();
        } else {
            console.error("pdf-exporter: starter.js loaded but PdfExporterStarter is not defined — DLE toolbar button injection skipped.");
        }
    };
    script.onerror = function () {
        console.error("pdf-exporter: failed to load starter.js — DLE toolbar button injection skipped.");
    };
    document.head.appendChild(script);
})();
