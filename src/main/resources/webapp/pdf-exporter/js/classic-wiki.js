/*
 * Classic Wiki injector - enables PDF export of Polarion Classic Wiki pages.
 * Configure a single script tag (injected into every page):
 *
 *   scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/classic-wiki.js"></script>
 *
 * It adds an "Export to PDF" button to the toolbar of a Classic Wiki page (the one behind "Expand
 * Tools"). To keep that toolbar always expanded, opt in with:
 *
 *   scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/classic-wiki.js" data-expand-tools="true"></script>
 *
 * It combines with live-reports.js: put both tags into the one scriptInjection.mainHead value.
 *
 * A Classic Wiki page is not part of the main page. Polarion renders it server-side into an iframe of
 * the same origin, /polarion/wiki/bin/view/..., and no scriptInjection property reaches that iframe. So
 * this script runs in the main page and injects into the iframe from outside. Polarion keeps the iframe
 * while the user moves between wiki pages and reloads it, and replaces it when the user leaves the wiki
 * and comes back. Hence a load listener per iframe and an observer that finds new iframes.
 *
 * The toolbar is static markup, not a GWT widget, so nothing re-renders it after a load and the shared
 * self-healing engine that live-reports.js uses is not needed here.
 *
 * The click handler is a function of this script, not an inline onclick. It runs in the main page, so
 * the export dialog opens over the whole page and reads the location hash of the main page, which is
 * what addresses the wiki page. The iframe's own URL does not.
 */
(function () {
    const timestampParam = `?timestamp=${Date.now()}`;

    // The export dialog is a React module of the pdf-exporter-app webapp, imported on click. It mounts
    // itself into a shadow root of its own, so nothing has to be injected into the page for it.
    const POPUP_MODULE = `/polarion/pdf-exporter-app/ui/app/assets/export-popup.js${timestampParam}`;

    const MARKER_ID = 'pdf-exporter-wiki-toolbar-injected';
    const WIKI_VIEW_PATH = '/polarion/wiki/bin/view/';
    // The main page's hash of a wiki page, in a project, in the global repository or in a baseline. Polarion
    // renders plan and test run pages of the Classic Wiki kind with the same toolbar, but their hash
    // addresses no wiki page, so the dialog could not export them as one.
    const WIKI_HASH = /^#\/(?:baseline\/[^/]+\/)?(?:project\/[^/]+\/)?wiki\//;
    // Set on each iframe this script listens to, so a second observer callback does not add a second listener.
    const WATCHED = '__pdfExporterWikiWatched';

    const expandTools = !!(document.currentScript && document.currentScript.dataset.expandTools === 'true');

    // The wiki's own separator, as it stands before its refresh button, followed by a replica of the
    // native wiki toolbar buttons (Edit, Extract Work Item). The wiki's own stylesheets inside the iframe
    // give both the native look. The separator sets the button apart like on the Live Report toolbar.
    const BUTTON_HTML = `
        <table cellspacing="0" cellpadding="0" border="0"><tbody><tr>
            <td style="padding: 0 6px;"><img src="/polarion/wiki/skins/sidecar/separatorbig.gif" alt=""></td>
            <td>
                <div class="enab" role="button" tabindex="0" title="Export to PDF" style="cursor: pointer;">
                    <table cellspacing="0" cellpadding="0" border="0" class="com_polarion_reina_web_js_widgets_JSPopupButton_Button">
                        <tbody><tr>
                            <td class="bt-icon"><img src="/polarion/ria/images/dle/operations/actionPdfExport16.svg" alt="" style="width: 16px; height: 16px;"></td>
                            <td class="bt-icon-label">Export to PDF</td>
                        </tr></tbody>
                    </table>
                </div>
            </td>
        </tr></tbody></table>`;

    function openPopup() {
        import(POPUP_MODULE)
            .then(module => module.openExportPopup({ documentType: 'WIKI_PAGE' }))
            .catch(console.error);
    }

    function injectInto(frame) {
        let doc, path;
        try {
            doc = frame.contentDocument;
            path = frame.contentWindow.location.pathname;
        } catch {
            return; // another origin, or an iframe already removed: not a wiki page
        }
        if (!doc || !path.startsWith(WIKI_VIEW_PATH) || !WIKI_HASH.test(window.location.hash)
                || doc.getElementById(MARKER_ID)) {
            return;
        }
        // The Edit button is in the toolbar row of every wiki page view, and only there.
        const row = doc.getElementById('editButt')?.closest('tr');
        if (!row) {
            return;
        }
        const cell = doc.createElement('td');
        cell.id = MARKER_ID;
        cell.innerHTML = BUTTON_HTML;
        const button = cell.querySelector('[role="button"]');
        button.addEventListener('click', openPopup);
        button.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                openPopup();
            }
        });
        // Before the cell that pushes the right-hand buttons to the right, like the native left-hand ones.
        const spacer = [...row.children].find(td => td.getAttribute('width') === '100%');
        if (spacer) {
            spacer.before(cell);
        } else {
            row.append(cell);
        }
        // Polarion's own "Expand Tools" handler. It only shows the toolbar, so a second call changes nothing.
        if (expandTools && typeof frame.contentWindow.showMenubar === 'function') {
            frame.contentWindow.showMenubar();
        }
    }

    function watch(frame) {
        if (frame[WATCHED]) {
            return;
        }
        frame[WATCHED] = true;
        frame.addEventListener('load', () => injectInto(frame));
        injectInto(frame); // the iframe may have loaded before this script found it
    }

    function scan() {
        document.querySelectorAll('iframe').forEach(watch);
    }

    let scheduled = false;
    new MutationObserver(() => {
        // Coalesce a burst of GWT mutations into a single scan.
        if (!scheduled) {
            scheduled = true;
            requestAnimationFrame(() => {
                scheduled = false;
                scan();
            });
        }
    }).observe(document.documentElement, { childList: true, subtree: true });
    scan();
})();
