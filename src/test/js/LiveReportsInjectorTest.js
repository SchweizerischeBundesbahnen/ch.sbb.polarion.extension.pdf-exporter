import { expect } from 'chai';
import { JSDOM } from 'jsdom';

// live-reports.js is a plain IIFE that at load time reads `document.currentScript` (for the
// extension base URL and the data-expand-tools opt-in) and `top` (order registry). The globals it
// closes over must exist BEFORE evaluation, and each test needs a fresh evaluation — hence dynamic
// import() with a unique query string per load (Node caches ES modules per URL).
describe('live-reports.js injector', function () {
    let dom, window, document;
    let createdConfigs, injectToolbarCalls, autoExpandCalls;
    let importCounter = 0;

    beforeEach(function () {
        dom = new JSDOM('<!DOCTYPE html><html lang="en"><head></head><body></body></html>', { url: 'http://localhost/' });
        window = dom.window;
        document = window.document;
        global.window = window;
        global.top = window;                 // script reads bare `top`; self === top (not in an iframe)
        global.document = document;
        createdConfigs = [];
        injectToolbarCalls = [];
        autoExpandCalls = 0;
    });

    afterEach(function () {
        delete global.window;
        delete global.top;
        delete global.document;
    });

    // Loads live-reports.js with the shared engine already "loaded": the engine element id is
    // present, so injectScript takes its already-loaded branch and the callback runs synchronously
    // against the stubbed GenericDleToolbarStarter.
    async function loadInjector({ expandTools = false } = {}) {
        const engineTag = document.createElement('script');
        engineTag.id = 'generic-dle-toolbar-engine';
        document.head.appendChild(engineTag);
        window.GenericDleToolbarStarter = {
            create: (config) => {
                createdConfigs.push(config);
                return { injectToolbar: (params) => injectToolbarCalls.push(params) };
            },
            autoExpandRichPageTools: () => autoExpandCalls++
        };

        // The <script> tag the Polarion scriptInjection config would produce.
        const selfTag = document.createElement('script');
        selfTag.src = 'http://localhost/polarion/pdf-exporter/js/live-reports.js';
        if (expandTools) {
            selfTag.dataset.expandTools = 'true';
        }
        Object.defineProperty(document, 'currentScript', { value: selfTag, configurable: true });

        await import(`../../main/resources/webapp/pdf-exporter/js/live-reports.js?load=${importCounter++}`);
        await flushPromises(); // the engine promise resolves synchronously; let its .then(create) run
    }

    const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

    it('injects the popup stylesheets and micromodal once', async function () {
        await loadInjector();
        for (const id of ['pdf-exporter-styles', 'pdf-micromodal-styles', 'generic-control-tokens',
            'generic-checkbox-styles', 'generic-searchable-dropdown-styles', 'generic-inputs-styles',
            'generic-alerts-styles']) {
            const link = document.getElementById(id);
            expect(link, id).to.exist;
            expect(link.tagName).to.equal('LINK');
        }
        expect(document.getElementById('pdf-micromodal-script')).to.exist;
    });

    it('creates the report-toolbar starter against the richPagePreview target and injects', async function () {
        await loadInjector();
        expect(createdConfigs.length).to.equal(1);
        const config = createdConfigs[0];
        expect(config.markerId).to.equal('pdf-exporter-rp-toolbar-injected');
        expect(config.target).to.equal('richPagePreview');
        expect(config.order).to.be.a('number');
        expect(injectToolbarCalls.length).to.equal(1);
    });

    it('builds the button from its own script URL and opens the popup in Live Report context', async function () {
        await loadInjector();
        const html = createdConfigs[0].alternateHtml;
        expect(html).to.contain('/polarion/pdf-exporter/ui/js/modules/ExportPopup.js');
        expect(html).to.contain("documentType: 'LIVE_REPORT'");
        expect(html).to.contain('Export to PDF');
        // visually separated from the native buttons, like the DLE toolbar button
        expect(html).to.contain('toolbar_splitter_gray.gif');
    });

    it('does not auto-expand the tools toolbar by default', async function () {
        await loadInjector();
        expect(autoExpandCalls).to.equal(0);
    });

    it('auto-expands the tools toolbar when opted in via data-expand-tools="true"', async function () {
        await loadInjector({ expandTools: true });
        expect(autoExpandCalls).to.equal(1);
    });

    it('waits for an in-flight engine load instead of dropping the button (multi-extension)', async function () {
        // Another extension already added the engine <script> (same id) but it hasn't finished
        // loading yet — GenericDleToolbarStarter is not defined. Our onload must NOT run
        // synchronously (that dropped the button before this fix); it must wait for the load event.
        const engineTag = document.createElement('script');
        engineTag.id = 'generic-dle-toolbar-engine';
        document.head.appendChild(engineTag);

        const selfTag = document.createElement('script');
        selfTag.src = 'http://localhost/polarion/pdf-exporter/js/live-reports.js';
        Object.defineProperty(document, 'currentScript', { value: selfTag, configurable: true });
        await import(`../../main/resources/webapp/pdf-exporter/js/live-reports.js?load=${importCounter++}`);
        await flushPromises();

        expect(createdConfigs.length).to.equal(0); // engine not loaded yet → nothing created

        // The engine finishes loading and defines its global, then fires load.
        window.GenericDleToolbarStarter = {
            create: (config) => { createdConfigs.push(config); return { injectToolbar: (p) => injectToolbarCalls.push(p) }; },
            autoExpandRichPageTools: () => autoExpandCalls++
        };
        engineTag.dispatchEvent(new window.Event('load'));
        await flushPromises(); // the engine promise resolves on load → its .then(create) runs

        expect(createdConfigs.length).to.equal(1); // button registered after the engine loaded
        expect(injectToolbarCalls.length).to.equal(1);
    });
});
