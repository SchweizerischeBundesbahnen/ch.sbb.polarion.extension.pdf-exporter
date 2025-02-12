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
        expect(exportContext.baselineRevision).to.be.undefined;
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
        expect(exportContext.baselineRevision).to.be.undefined;
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
        expect(exportContext.baselineRevision).to.be.undefined;
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
        expect(exportContext.baselineRevision).to.be.undefined;
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
        expect(exportContext.baselineRevision).to.be.undefined;
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
        expect(exportContext.baselineRevision).to.be.undefined;
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
        expect(exportContext.baselineRevision).to.be.undefined;
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
        expect(exportContext.baselineRevision).to.be.undefined;
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.be.undefined;
        expect(exportContext.getDocumentName()).to.be.undefined;
    });

    it('URL: #/project/elibrary/collection?id=144', function () {
        const locationHash = "#/project/elibrary/collection?id=144";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.BASELINE_COLLECTION, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.BASELINE_COLLECTION);
        expect(exportContext.projectId).to.equal('elibrary');
        expect(exportContext.locationPath).to.be.undefined;
        expect(exportContext.baselineRevision).to.be.undefined;
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.deep.equal({ id: '144' });

        expect(exportContext.getSpaceId()).to.be.undefined;
        expect(exportContext.getDocumentName()).to.be.undefined;
    });

    it('URL: #/project/elibrary/collection/145/wiki/live_doc', function () {
        const locationHash = "#/project/elibrary/collection/145/wiki/live_doc";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_DOC, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
        expect(exportContext.projectId).to.equal('elibrary');
        expect(exportContext.locationPath).to.equal('_default/live_doc');
        expect(exportContext.baselineRevision).to.be.undefined;
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.equal('_default');
        expect(exportContext.getDocumentName()).to.equal('live_doc');
    });

    it('URL: #/project/drivepilot/collection/elibrary//144/wiki/Requirements/live%20doc', function () {
        const locationHash = "#/project/drivepilot/collection/elibrary//144/wiki/Requirements/live%20doc";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_DOC, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
        expect(exportContext.projectId).to.equal('drivepilot');
        expect(exportContext.locationPath).to.equal('Requirements/live doc');
        expect(exportContext.baselineRevision).to.be.undefined;
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.equal('Requirements');
        expect(exportContext.getDocumentName()).to.equal('live doc');
    });

    it('URL: #/project/drivepilot/collection/1/wiki/Requirements/System%20Requirement%20Specification?revision=112', function () {
        const locationHash = "#/project/drivepilot/collection/1/wiki/Requirements/System%20Requirement%20Specification?revision=112";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_DOC, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
        expect(exportContext.projectId).to.equal('drivepilot');
        expect(exportContext.locationPath).to.equal('Requirements/System Requirement Specification');
        expect(exportContext.baselineRevision).to.be.undefined;
        expect(exportContext.revision).to.equal('112');
        expect(exportContext.urlQueryParameters).to.deep.equal({ revision: '112' });

        expect(exportContext.getSpaceId()).to.equal('Requirements');
        expect(exportContext.getDocumentName()).to.equal('System Requirement Specification');
    });

    it('URL: #/baseline/6749/project/elibrary/wiki/BigDoc2', function () {
        const locationHash = "#/baseline/6749/project/elibrary/wiki/BigDoc2";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_DOC, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_DOC);
        expect(exportContext.projectId).to.equal('elibrary');
        expect(exportContext.locationPath).to.equal('_default/BigDoc2');
        expect(exportContext.baselineRevision).to.equal('6749');
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.equal('_default');
        expect(exportContext.getDocumentName()).to.equal('BigDoc2');
    });

    it('URL: #/baseline/6711/project/elibrary/wiki/Specification/Epic%20Statistics?parameter=value', function () {
        const locationHash = "#/baseline/6711/project/elibrary/wiki/Specification/Epic%20Statistics?parameter=value";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_REPORT, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_REPORT);
        expect(exportContext.projectId).to.equal('elibrary');
        expect(exportContext.locationPath).to.equal('Specification/Epic Statistics');
        expect(exportContext.baselineRevision).to.equal('6711');
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.deep.equal({ parameter: 'value' });

        expect(exportContext.getSpaceId()).to.equal('Specification');
        expect(exportContext.getDocumentName()).to.equal('Epic Statistics');
    });

    it('URL: #/wiki/space/LiveReport', function () {
        const locationHash = "#/wiki/space/LiveReport";
        const exportContext = new ExportContext({documentType: ExportParams.DocumentType.LIVE_REPORT, polarionLocationHash: locationHash});

        expect(exportContext.documentType).to.equal(ExportParams.DocumentType.LIVE_REPORT);
        expect(exportContext.projectId).to.be.null;
        expect(exportContext.locationPath).to.equal('space/LiveReport');
        expect(exportContext.baselineRevision).to.be.undefined;
        expect(exportContext.revision).to.be.undefined;
        expect(exportContext.urlQueryParameters).to.be.undefined;

        expect(exportContext.getSpaceId()).to.equal('space');
        expect(exportContext.getDocumentName()).to.equal('LiveReport');
    });

});
