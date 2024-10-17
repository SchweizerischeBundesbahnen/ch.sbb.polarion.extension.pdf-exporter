import { expect } from 'chai';
import ExportContext from '../../main/resources/webapp/pdf-exporter/js/modules/ExportContext.js';
import ExportParams from "../../main/resources/webapp/pdf-exporter/js/modules/ExportParams.js";

describe('ExportContext Class', function () {
    it('URL: #/project/elibrary/wiki/BigDoc', function () {
        const locationHash = "#/project/elibrary/wiki/BigDoc";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_DOC, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
        expect(exportContext.projectId).to.equal('elibrary');
        expect(exportContext.locationPath).to.equal('_default/BigDoc');
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.equal('_default');
        expect(exportContext.getDocumentName()).to.equal('BigDoc');
    });

    it('URL: #/project/elibrary/wiki/Specification/Administration%20Specification', function () {
        const locationHash = "#/project/elibrary/wiki/Specification/Administration%20Specification";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_DOC, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
        expect(exportContext.projectId).to.equal('elibrary');
        expect(exportContext.locationPath).to.equal('Specification/Administration Specification');
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.equal('Specification');
        expect(exportContext.getDocumentName()).to.equal('Administration Specification');
    });

    it('URL: #/project/mega_project/wiki/Specs/test', function () {
        const locationHash = "#/project/mega_project/wiki/Specs/test";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_DOC, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
        expect(exportContext.projectId).to.equal('mega_project');
        expect(exportContext.locationPath).to.equal('Specs/test');
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.equal('Specs');
        expect(exportContext.getDocumentName()).to.equal('test');
    });

    it('URL: #/wiki/classic%20wiki%20page', function () {
        const locationHash = "#/wiki/classic%20wiki%20page";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_DOC, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
        expect(exportContext.projectId).to.be.null;
        expect(exportContext.locationPath).to.equal('_default/classic wiki page');
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.equal('_default');
        expect(exportContext.getDocumentName()).to.equal('classic wiki page');
    });

    it('URL: #/wiki/TestLiveReport', function () {
        const locationHash = "#/wiki/TestLiveReport";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_REPORT, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_REPORT);
        expect(exportContext.projectId).to.be.null;
        expect(exportContext.locationPath).to.equal('_default/TestLiveReport');
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.equal('_default');
        expect(exportContext.getDocumentName()).to.equal('TestLiveReport');
    });

    it('URL: #/project/elibrary/testrun?id=elibrary_20231026-163136654', function () {
        const locationHash = "#/project/elibrary/testrun?id=elibrary_20231026-163136654";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_REPORT, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.TEST_RUN);
        expect(exportContext.projectId).to.equal('elibrary');
        expect(exportContext.locationPath).to.be.undefined;
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.deep.equal({ id: 'elibrary_20231026-163136654' });

        expect(exportContext.getSpaceId()).to.be.undefined;
        expect(exportContext.getDocumentName()).to.be.undefined;
    });

    it('URL: #/project/elibrary/wiki/Reports/LiveReport%20with%20params?stringParameter=asd&workItemType=changerequest&yesnoParameter=yes', function () {
        const locationHash = "#/project/elibrary/wiki/Reports/LiveReport%20with%20params?stringParameter=asd&workItemType=changerequest&yesnoParameter=yes";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_DOC, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
        expect(exportContext.projectId).to.equal('elibrary');
        expect(exportContext.locationPath).to.equal('Reports/LiveReport with params');
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.deep.equal({
            stringParameter: 'asd',
            workItemType: 'changerequest',
            yesnoParameter: 'yes'
        });

        expect(exportContext.getSpaceId()).to.equal('Reports');
        expect(exportContext.getDocumentName()).to.equal('LiveReport with params');
    });

    it('URL: #/project/elibrary/testruns', function () {
        const locationHash = "#/project/elibrary/testruns";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_REPORT, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.TEST_RUN);
        expect(exportContext.projectId).to.equal('elibrary');
        expect(exportContext.locationPath).to.be.undefined;
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.be.undefined;
        expect(exportContext.getDocumentName()).to.be.undefined;
    });

});
