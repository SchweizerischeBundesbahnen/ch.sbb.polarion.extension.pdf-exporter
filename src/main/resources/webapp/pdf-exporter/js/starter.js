/*
 * DEPRECATED backward-compatibility shim.
 *
 * The supported configuration is the single-tag injector:
 *   scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/dle-toolbar.js"></script>
 *
 * An older configuration loads this script and then calls
 *   <script>PdfExporterStarter.injectToolbar({alternate: true});</script>
 * so the global stays, and the call still puts the button in the document editor toolbar. What it no
 * longer does is choose a placement: the engine draws one toolbar button, in the toolbar row, and the
 * `alternate` flag that used to select the floating variant is gone with the variant itself. The flag is
 * accepted and ignored rather than rejected, so an unchanged configuration keeps working.
 *
 * Removal is planned for a future major version; migrate the configuration to dle-toolbar.js.
 */
(function () {
    const timestampParam = `?timestamp=${Date.now()}`;

    // This script's own webapp context (…/polarion/<ext>/js/starter.js), so nothing hardcodes it.
    const EXT_BASE = (document.currentScript && document.currentScript.src || '').replace(/js\/starter\.js.*$/, '') || '/polarion/pdf-exporter/';

    let injected = false;

    window.PdfExporterStarter = {
        /**
         * @deprecated Configure dle-toolbar.js instead.
         * @param {{alternate: boolean}|undefined} _params `alternate` is accepted and ignored.
         */
        injectToolbar: function (_params) {
            if (injected) {
                return; // the engine is idempotent per marker, but there is no reason to load it twice
            }
            injected = true;
            const injector = document.createElement('script');
            injector.src = `${EXT_BASE}js/dle-toolbar.js${timestampParam}`;
            injector.onerror = function () {
                console.error('pdf-exporter: failed to load dle-toolbar.js — toolbar injection skipped.');
            };
            document.head.appendChild(injector);
        }
    };
})();
