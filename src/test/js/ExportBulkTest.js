import { expect } from 'chai';
import { JSDOM } from 'jsdom';
import TestUtils from "./TestUtils.js";

/** @type {typeof ExportParams} */
const ExportBulk = await TestUtils.importUsingGeneric('ExportBulk.js');

describe('ExportBulk', function () {

    afterEach(function() {
        // do not forget to clean up otherwise this may screw up other tests (even in other files)
        delete global.window;
        delete global.document;
    });

    it('should get document type from header', function () {
        mockWindow('<!doctype html><html><body><div class="header" document-type="TEST_RUN"></div></body></html>');
        expect(new ExportBulk().widgetDocumentType).to.equal('TEST_RUN');
    });

    it('should throw an error if document type is not provided', function () {
        mockWindow('<!doctype html><html><body><div class="header"></div></body></html>');
        expect(() => new ExportBulk()).to.throw('unable to get documentType from the header');
    });

});

function mockWindow(htmlContent) {
    const dom = new JSDOM(htmlContent, {
        url: "http://localhost/",
    });
    global.window = dom.window;
    global.document = dom.window.document;
}
