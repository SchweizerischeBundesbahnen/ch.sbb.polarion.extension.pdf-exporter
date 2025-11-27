import { expect } from 'chai';
import TestUtils from "./TestUtils.js";

/** @type {typeof ExportParams} */
const ExportParams = await TestUtils.loadModule('ExportParams.js');

describe('ExportParams', function () {
    it('should throw an error if documentType is not provided', function () {
        expect(() => new ExportParams.Builder()).to.throw("documentType is mandatory");
    });

    it('should create an instance with the provided documentType', function () {
        const params = new ExportParams.Builder(ExportParams.DocumentType.LIVE_DOC).build();
        expect(params).to.be.an.instanceOf(ExportParams);
        expect(params.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
    });

    it('should allow setting projectId and retrieve it correctly', function () {
        const params = new ExportParams.Builder(ExportParams.DocumentType.LIVE_DOC)
            .setProjectId('12345')
            .build();
        expect(params.projectId).to.equal('12345');
    });

    it('should correctly serialize to JSON excluding undefined properties', function () {
        const params = new ExportParams.Builder(ExportParams.DocumentType.LIVE_DOC)
            .setProjectId('12345')
            .setOrientation(ExportParams.Orientation.PORTRAIT)
            .build();

        const json = JSON.parse(params.toJSON());
        expect(json).to.have.property('projectId', '12345');
        expect(json).to.have.property('orientation', ExportParams.Orientation.PORTRAIT);
        expect(json).to.not.have.property('locationPath');
    });

    it('should allow setting multiple properties using the Builder pattern', function () {
        const params = new ExportParams.Builder(ExportParams.DocumentType.LIVE_DOC)
            .setProjectId('12345')
            .setLocationPath('/path/to/doc')
            .setPaperSize(ExportParams.PaperSize.A4)
            .build();

        expect(params.projectId).to.equal('12345');
        expect(params.locationPath).to.equal('/path/to/doc');
        expect(params.paperSize).to.equal(ExportParams.PaperSize.A4);
    });
});
