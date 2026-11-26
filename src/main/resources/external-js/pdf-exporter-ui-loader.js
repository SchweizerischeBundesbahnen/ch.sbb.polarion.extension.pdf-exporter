/*
 * Loads the toolbar buttons of the PDF Exporter extension into the main Polarion page.
 *
 * Polarion renders a <script> tag for this file into authapp/index.jsp for every user, from the
 * com.polarion.portal.tomcat.externalJsFile extension point in plugin.xml. That is what replaced the
 * scriptInjection.* properties an administrator used to paste into Configuration Properties.
 *
 * Polarion serves this file from /polarion/codemirror-modes/ with "Cache-Control: immutable" for the
 * whole Polarion build, so a browser never fetches a changed version of it. That is why this loader
 * must never change, and the real script comes from the extension's own web application, which
 * Polarion serves without that header. The base name must stay unique across all installed plugins:
 * the servlet resolves a request by base name alone.
 */
(function () {
    'use strict';

    if (window !== window.top) {
        return;
    }
    const script = document.createElement('script');
    script.src = `/polarion/pdf-exporter/js/toolbar-injector.js?timestamp=${Date.now()}`;
    script.onerror = function () {
        console.error('pdf-exporter: failed to load toolbar-injector.js - toolbar buttons skipped.');
    };
    document.head.appendChild(script);
})();
