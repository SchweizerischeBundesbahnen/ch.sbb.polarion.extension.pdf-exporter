/*
 * Live Reports dependency loader — the recommended way to enable the "Export to PDF" button in
 * Polarion Live Reports. Configure a single script tag (injected into every page):
 *
 *   scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/live-reports.js"></script>
 *
 * The "Export to PDF Button" report widget renders server-side and opens ExportPopup.js on click.
 * That popup does not ship its own styling or the micromodal library, so this script injects the
 * stylesheets and micromodal it needs. It adds NO toolbar button and does not load the DLE toolbar
 * engine. It replaces loading starter.js via mainHead (deprecated).
 *
 * The element ids match the ones starter.js uses, so nothing is injected twice if both run.
 */
(function () {
    const timestampParam = `?timestamp=${Date.now()}`;

    // Extension web-context base, derived from this script's own URL (…/polarion/<ext>/js/live-reports.js)
    // so nothing below hardcodes the /polarion/<ext>/ segment.
    const EXT_BASE = (document.currentScript && document.currentScript.src || '').replace(/js\/live-reports\.js.*$/, '') || '/polarion/pdf-exporter/';

    function injectStyle(id, href) {
        if (!top.document.getElementById(id)) {
            const link = top.document.createElement('link');
            link.id = id;
            link.rel = 'stylesheet';
            link.type = 'text/css';
            link.href = href;
            top.document.head.appendChild(link);
        }
    }

    function injectScript(id, src) {
        if (!top.document.getElementById(id)) {
            const script = top.document.createElement('script');
            script.id = id;
            script.setAttribute('src', src);
            script.setAttribute('type', 'text/javascript');
            top.document.head.appendChild(script);
        }
    }

    injectStyle('pdf-exporter-styles', `${EXT_BASE}css/pdf-exporter.css${timestampParam}`);
    injectStyle('pdf-micromodal-styles', `${EXT_BASE}ui/generic/css/micromodal.css${timestampParam}`);
    injectStyle('generic-control-tokens', `${EXT_BASE}ui/generic/css/control-tokens.css${timestampParam}`);
    injectStyle('generic-checkbox-styles', `${EXT_BASE}ui/generic/css/checkboxes.css${timestampParam}`);
    injectStyle('generic-searchable-dropdown-styles', `${EXT_BASE}ui/generic/css/searchable-dropdown.css${timestampParam}`);
    injectStyle('generic-inputs-styles', `${EXT_BASE}ui/generic/css/inputs.css${timestampParam}`);
    injectStyle('generic-alerts-styles', `${EXT_BASE}ui/generic/css/alerts.css${timestampParam}`);
    injectScript('pdf-micromodal-script', `${EXT_BASE}ui/generic/js/micromodal.min.js${timestampParam}`);
})();
