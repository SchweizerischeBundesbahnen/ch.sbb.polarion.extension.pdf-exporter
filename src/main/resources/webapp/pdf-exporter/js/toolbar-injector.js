/*
 * The extension's toolbar buttons, injected into every Polarion page without any administrator
 * configuration. external-js/pdf-exporter-ui-loader.js loads this script into the main portal page;
 * see plugin.xml for how Polarion gets it there.
 *
 * What is drawn is read per project from the `appearance` setting (the Appearance administration
 * page), which a project inherits from the Default Repository unless it saves its own. Polarion is a
 * single-page application: the user moves to another project without reloading the page, and this
 * script runs once. So the configuration is re-read on every hash change and the buttons are rebuilt
 * for the project the user is now in.
 *
 * Three surfaces, two mechanisms:
 *   - the document editor and Live Report toolbars go through the shared self-healing engine of
 *     react-sbb-polarion (CommonDleToolbarStarter), which re-injects after a GWT re-render;
 *   - a Classic Wiki page is rendered server-side into a same-origin iframe which no injection
 *     reaches, so its toolbar is injected into that iframe from here (a load listener per iframe,
 *     because Polarion reloads it between wiki pages, and an observer for iframes it replaces).
 *
 * Nothing else is put on the page: the buttons use Polarion's own toolbar classes, and the export
 * dialog is a React module which mounts into a shadow root of its own, styles included.
 */
(function () {
    'use strict';

    // One timestamp per page load: a click reuses what the previous click loaded, while an updated
    // extension is still picked up on the next page open.
    const timestampParam = `?timestamp=${Date.now()}`;
    const EXT_BASE = '/polarion/pdf-exporter/';
    const APP_BASE = '/polarion/pdf-exporter-app/ui/app/';

    // The export dialog is a React module of the pdf-exporter-app webapp, imported on click. It mounts
    // into a shadow root of its own, so nothing has to be injected into the page for it.
    const POPUP_MODULE = `${APP_BASE}assets/export-popup.js${timestampParam}`;
    // The toolbar engine is a react-sbb-polarion artifact, copied next to this extension's built app.
    const ENGINE_SRC = `${APP_BASE}assets/dle-toolbar-starter.js${timestampParam}`;
    const APPEARANCE_URL = `${EXT_BASE}rest/internal/appearance`;

    const ICON_URL = '/polarion/ria/images/dle/operations/actionPdfExport16.svg';
    const TITLE = 'Export to PDF';

    // The ids the scriptInjection injectors of v13 used, kept so that a button one of them left on a page
    // is recognised rather than drawn a second time next to it.
    const LIVE_DOC_MARKER = 'pdf-exporter-toolbar-injected';
    const LIVE_REPORT_MARKER = 'pdf-exporter-rp-toolbar-injected';
    const WIKI_MARKER = 'pdf-exporter-wiki-toolbar-injected';

    const WIKI_VIEW_PATH = '/polarion/wiki/bin/view/';
    // The hash of a wiki page, in a project, in the global repository or in a baseline. Polarion
    // renders plan and test run pages of the Classic Wiki kind with the same toolbar, but their hash
    // addresses no wiki page, so the dialog could not export them as one.
    const WIKI_HASH = /^#\/(?:baseline\/[^/]+\/)?(?:project\/[^/]+\/)?wiki\//;
    // Set on each iframe this script listens to, so a second observer callback does not add a second listener.
    const WATCHED = '__pdfExporterWikiWatched';

    // Polarion's own project-id charset. Anything else in the hash segment is not a project id, so it
    // is not passed on to the server.
    const PROJECT_ID_PATTERN = /^[\w.-]+$/;

    // What the icon of a Polarion toolbar button looks like until the pointer is over it.
    const ICON_OPACITY = '0.6';

    function openPopup(documentType) {
        import(POPUP_MODULE)
            .then(module => module.openExportPopup({documentType}))
            .catch(console.error);
    }
    // The Live Report button keeps Polarion's own onclick attribute, so it survives a re-injection by
    // the engine, which re-creates the markup from this string.
    const POPUP_ONCLICK = `import('${POPUP_MODULE}')
        .then(module => module.openExportPopup({documentType: 'LIVE_REPORT'}))
        .catch(console.error);`;

    // An exact replica of Polarion's labeled toolbar buttons (e.g. Add Comment), behind the native
    // separator, so the injected button inherits the native look, sizing and hover behavior.
    const LIVE_REPORT_HTML = `
        <table class="dleToolBarTable">
            <tr class="dleToolBarRow">
                <td><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
                <td><img src="/polarion/ria/images/toolbar_splitter_gray.gif" class="gwt-Image polarion-dle-ToolbarPanel-separator"></td>
                <td><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
                <td style="vertical-align: middle;">
                    <table class="polarion-dle-toolbar-ButtonWithLabel polarion-Button-shared polarion-Button-HighlightOnHover"
                           role="button" cellpadding="0" cellspacing="0" title="${TITLE}" tabindex="0"
                           onclick="${POPUP_ONCLICK}">
                        <colgroup><col><col></colgroup>
                        <tbody><tr>
                            <td class="polarion-Button-GridImpl-ImageCell"><img src="${ICON_URL}" class="gwt-Image" alt="${TITLE}"></td>
                            <td class="polarion-Button-GridImpl-TextCell"><div class="gwt-Label">${TITLE}</div></td>
                        </tr></tbody>
                    </table>
                </td>
            </tr>
        </table>`;

    // The wiki's own separator, as it stands before its refresh button, followed by a replica of the
    // native wiki toolbar buttons (Edit, Extract Work Item). The wiki's own stylesheets inside the
    // iframe give both the frame and the size. The label takes the font of the Live Report toolbar
    // instead of the wiki's Arial with its white shadow, so the button looks the same on both pages.
    // The hover dimming is applied from here: the wiki has no polarion-Button-HighlightOnHover rule
    // and this script adds no stylesheet.
    const WIKI_BUTTON_HTML = `
        <table cellspacing="0" cellpadding="0" border="0"><tbody><tr>
            <td style="padding: 0 6px;"><img src="/polarion/wiki/skins/sidecar/separatorbig.gif" alt=""></td>
            <td>
                <div class="enab" role="button" tabindex="0" title="${TITLE}" style="cursor: pointer;">
                    <table cellspacing="0" cellpadding="0" border="0" class="com_polarion_reina_web_js_widgets_JSPopupButton_Button">
                        <tbody><tr>
                            <td class="bt-icon"><img src="${ICON_URL}" alt="" style="width: 16px; height: 16px; opacity: ${ICON_OPACITY};"></td>
                            <td class="bt-icon-label" style="font-family: 'Segoe UI', Selawik, 'Open Sans', Arial, sans-serif; text-shadow: none;">${TITLE}</td>
                        </tr></tbody>
                    </table>
                </div>
            </td>
        </tr></tbody></table>`;

    /* ---------------------------------------------------------------------------------------- */
    /* The engine                                                                                  */
    /* ---------------------------------------------------------------------------------------- */

    // Deliberately NOT generic's engine id or promise key. The two engines export different globals
    // (GenericDleToolbarStarter vs CommonDleToolbarStarter), so sharing the loader bookkeeping would
    // hand this file an engine it cannot use - or, when the other extension's tag is already in the
    // document, leave the promise waiting for a load event which has already fired. What the two DO
    // share, and must keep sharing, are the registries on `top`: button order and the injected
    // stylesheet are coordinated through those, not through how the script got loaded.
    const ENGINE_ID = 'common-dle-toolbar-engine';

    function loadEngine() {
        if (!window.__commonDleToolbarEnginePromise) {
            window.__commonDleToolbarEnginePromise = new Promise((resolve, reject) => {
                if (window.CommonDleToolbarStarter) {
                    resolve();
                    return;
                }
                const existing = document.getElementById(ENGINE_ID);
                if (existing) {
                    existing.addEventListener('load', resolve);
                    existing.addEventListener('error', reject);
                    return;
                }
                const script = document.createElement('script');
                script.id = ENGINE_ID;
                script.setAttribute('src', ENGINE_SRC);
                script.setAttribute('type', 'text/javascript');
                script.onload = resolve;
                script.onerror = reject;
                document.head.appendChild(script);
            });
        }
        return window.__commonDleToolbarEnginePromise;
    }

    /* ---------------------------------------------------------------------------------------- */
    /* State                                                                                       */
    /* ---------------------------------------------------------------------------------------- */

    // Only the newest load of this script drives the page. The loader appends it once, but a server
    // which also still configures the deprecated scriptInjection injectors, or a user who re-runs it,
    // would otherwise end up with two copies racing to build and tear down the same buttons.
    const installToken = {};
    window.__pdfExporterToolbarInstall = installToken;
    const installed = () => window.__pdfExporterToolbarInstall === installToken;

    /** Appearance per project id, so moving back and forth does not ask the server again. */
    const appearanceCache = new Map();
    /**
     * The project the newest rebuild was started for; undefined before the first one, and again when
     * that rebuild failed, so a later visit to the same project tries once more. Set synchronously,
     * because Polarion's own hash change arrives next to the one this script already acted on and both
     * would otherwise queue a rebuild of their own.
     */
    let requestedFor;
    /** Serializes the rebuilds, so a burst of hash changes cannot interleave two of them. */
    let pending = Promise.resolve();

    let liveDocStarter = null;
    let liveReportStarter = null;
    let wikiButtonEnabled = false;
    let wikiExpandTools = false;

    /** The current project id, parsed from Polarion's location hash, or '' outside a project. */
    function currentProjectId() {
        try {
            const match = /project\/([^/]+)\//.exec(decodeURI(window.location.hash || ''));
            const projectId = match ? match[1] : '';
            return PROJECT_ID_PATTERN.test(projectId) ? projectId : '';
        } catch {
            return '';
        }
    }

    function readAppearance(projectId) {
        if (appearanceCache.has(projectId)) {
            return Promise.resolve(appearanceCache.get(projectId));
        }
        const url = projectId ? `${APPEARANCE_URL}?projectId=${encodeURIComponent(projectId)}` : APPEARANCE_URL;
        return fetch(url, {credentials: 'same-origin'})
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                return response.json();
            })
            .then(appearance => {
                appearanceCache.set(projectId, appearance);
                return appearance;
            });
    }

    /* ---------------------------------------------------------------------------------------- */
    /* The Classic Wiki iframe                                                                     */
    /* ---------------------------------------------------------------------------------------- */

    function injectIntoWikiFrame(frame) {
        let doc, path;
        try {
            doc = frame.contentDocument;
            path = frame.contentWindow.location.pathname;
        } catch {
            return; // another origin, or an iframe already removed: not a wiki page
        }
        if (!wikiButtonEnabled || !doc || !path.startsWith(WIKI_VIEW_PATH)
                || !WIKI_HASH.test(window.location.hash) || doc.getElementById(WIKI_MARKER)) {
            return;
        }
        // The Edit button is in the toolbar row of every wiki page view, and only there.
        const row = doc.getElementById('editButt')?.closest('tr');
        if (!row) {
            return;
        }
        const cell = doc.createElement('td');
        cell.id = WIKI_MARKER;
        cell.innerHTML = WIKI_BUTTON_HTML;
        const button = cell.querySelector('[role="button"]');
        const icon = button.querySelector('img');
        button.addEventListener('mouseenter', () => { icon.style.opacity = '1'; });
        button.addEventListener('mouseleave', () => { icon.style.opacity = ICON_OPACITY; });
        // The handler stays a function of the main page, so the dialog reads the main page's hash,
        // which is what addresses the wiki page. The iframe's own URL does not.
        button.addEventListener('click', () => openPopup('WIKI_PAGE'));
        button.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                openPopup('WIKI_PAGE');
            }
        });
        // Before the cell which pushes the right-hand buttons to the right, like the native left-hand ones.
        const spacer = [...row.children].find(td => td.getAttribute('width') === '100%');
        if (spacer) {
            spacer.before(cell);
        } else {
            row.append(cell);
        }
        // Polarion's own "Expand Tools" handler. It only shows the toolbar, so a second call changes nothing.
        if (wikiExpandTools && typeof frame.contentWindow.showMenubar === 'function') {
            frame.contentWindow.showMenubar();
        }
    }

    function removeWikiButtons() {
        for (const frame of document.querySelectorAll('iframe')) {
            try {
                frame.contentDocument?.getElementById(WIKI_MARKER)?.remove();
            } catch {
                // another origin, or an iframe already removed: nothing of ours in it
            }
        }
    }

    function watchWikiFrame(frame) {
        if (frame[WATCHED]) {
            return;
        }
        frame[WATCHED] = true;
        frame.addEventListener('load', () => injectIntoWikiFrame(frame));
    }

    // Injecting is separate from watching, and runs on every scan: an iframe which is already watched
    // still needs the button back after a move to another project tore it down, and after the first
    // scan of an iframe which had loaded before this script found it.
    function scanWikiFrames() {
        for (const frame of document.querySelectorAll('iframe')) {
            watchWikiFrame(frame);
            injectIntoWikiFrame(frame);
        }
    }

    /* ---------------------------------------------------------------------------------------- */
    /* Building and tearing down the buttons                                                       */
    /* ---------------------------------------------------------------------------------------- */

    function teardown(starter, markerId) {
        starter?.destroy();
        document.getElementById(markerId)?.remove();
        return null;
    }

    function build(engine, appearance) {
        if (appearance.liveDocButton) {
            liveDocStarter = engine.create({
                markerId: LIVE_DOC_MARKER,
                html: engine.buildButtonHtml({
                    title: TITLE,
                    iconUrl: `${ICON_URL}${timestampParam}`,
                    onClick: `import('${POPUP_MODULE}')
                        .then(module => module.openExportPopup({documentType: 'LIVE_DOC'}))
                        .catch(console.error);`
                }),
                target: 'dleEditor',
                order: 0,
                // Already answered by the same request the appearance came from, so the engine gets a
                // function rather than a URL it would call a second time. Fail-closed is kept: a
                // request which did not answer never reaches this point, and the button is not built.
                permissionCheck: () => appearance.permitted
            });
            liveDocStarter.injectToolbar();
        }

        if (appearance.liveReportButton) {
            liveReportStarter = engine.create({
                markerId: LIVE_REPORT_MARKER,
                html: LIVE_REPORT_HTML,
                target: 'richPagePreview',
                order: 0
            });
            liveReportStarter.injectToolbar();
            if (appearance.liveReportAutoExpandTools) {
                // One-way: the engine installs a single shared observer per page and offers no
                // teardown, so a project which keeps the toolbar expanded keeps expanding it for the
                // rest of the session, including on a project which does not ask for it. Expanding a
                // toolbar Polarion collapses on every page open is cosmetic, so this is left as is
                // rather than reaching into another extension's registry to undo it.
                engine.autoExpandRichPageTools();
            }
        }

        wikiButtonEnabled = !!appearance.classicWikiButton;
        wikiExpandTools = !!appearance.classicWikiAutoExpandTools;
        if (wikiButtonEnabled) {
            scanWikiFrames();
        }
    }

    /**
     * Rebuild the buttons for the project the user is now in. A project whose configuration cannot be
     * read keeps the buttons of the project before it rather than losing them to a network blip: the
     * server decides what may be exported, this only decides what is drawn.
     */
    function applyCurrentProject() {
        const projectId = currentProjectId();
        if (!installed() || projectId === requestedFor) {
            return;
        }
        requestedFor = projectId;
        pending = pending
            .then(() => Promise.all([loadEngine(), readAppearance(projectId)]))
            .then(([, appearance]) => {
                // Another hash change, or a newer load of this script, may have overtaken this one
                // while it waited.
                if (!installed() || currentProjectId() !== projectId) {
                    return;
                }
                liveDocStarter = teardown(liveDocStarter, LIVE_DOC_MARKER);
                liveReportStarter = teardown(liveReportStarter, LIVE_REPORT_MARKER);
                wikiButtonEnabled = false;
                removeWikiButtons();

                build(window.CommonDleToolbarStarter, appearance);
            })
            .catch(error => {
                if (requestedFor === projectId) {
                    requestedFor = undefined;
                }
                console.error('pdf-exporter: could not configure the toolbar buttons.', error);
            });
    }

    /* ---------------------------------------------------------------------------------------- */
    /* Start                                                                                       */
    /* ---------------------------------------------------------------------------------------- */

    let scheduled = false;
    // Coalesce a burst of GWT mutations into a single pass. The observer does two things: it finds the
    // iframe of a wiki page, which Polarion replaces when the user leaves the wiki and comes back, and
    // it notices that the user is now in another project. The second is not something `hashchange`
    // alone can be trusted with: Polarion's own navigation sets the hash in ways which do not always
    // raise that event, and a project change which went unnoticed would leave the page with the
    // buttons of the project before it for the rest of the session. The check is a string comparison
    // when nothing changed, so running it on every render costs nothing.
    new MutationObserver(() => {
        if (!scheduled) {
            scheduled = true;
            requestAnimationFrame(() => {
                scheduled = false;
                if (!installed()) {
                    return;
                }
                applyCurrentProject();
                if (wikiButtonEnabled) {
                    scanWikiFrames();
                }
            });
        }
    }).observe(document.documentElement, {childList: true, subtree: true});

    window.addEventListener('hashchange', applyCurrentProject);
    applyCurrentProject();
})();
