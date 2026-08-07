import { describe, expect, it } from 'vitest';
import type { DocumentType, ExportFieldName } from '../src/export/documentType';
import {
  areRolesSelectable,
  isAutoSelectStylePackageAvailable,
  isDocumentLanguageRead,
  isFieldSent,
  isFieldVisible,
  isFileNameOffered,
  isPageWidthValidationOffered,
} from '../src/export/documentType';
import { toExportForm } from '../src/export/exportForm';
import type { ExportParamsJson, ExportTarget } from '../src/export/exportParams';
import { buildExportParams } from '../src/export/exportParams';
import { SAMPLE_DOCUMENT, SAMPLE_STYLE_PACKAGE_FULL } from './sidePanelSamples';

// Which rows the export popup shows for what it is exporting, and which of them the request then carries.
// These were the `visible-for-*` classes of popupForm.html and the type guards of
// ExportPopup.buildExportParams; the two do not agree everywhere, and the disagreement is asserted here so
// that it stays a decision rather than becoming a regression.

const ALL_TYPES: DocumentType[] = ['LIVE_DOC', 'LIVE_REPORT', 'TEST_RUN', 'BASELINE_COLLECTION', 'WIKI_PAGE'];

const visibleFor = (field: ExportFieldName): DocumentType[] => ALL_TYPES.filter((type) => isFieldVisible(field, type));
const sentFor = (field: ExportFieldName): DocumentType[] => ALL_TYPES.filter((type) => isFieldSent(field, type));

describe('which rows a document type shows', () => {
  it('shows the document-and-collection rows for those two only', () => {
    for (const field of [
      'renderComments',
      'cutEmptyChapters',
      'cutEmptyWorkitemAttributes',
      'markReferencedWorkitems',
      'customListStyles',
      'specificChapters',
      'localizeEnums',
      'roles',
      'workItemsQuery',
    ] as ExportFieldName[]) {
      expect(visibleFor(field), field).toEqual(['LIVE_DOC', 'BASELINE_COLLECTION']);
    }
  });

  it('shows the metadata fields for a Live Document only', () => {
    expect(visibleFor('metadataFields')).toEqual(['LIVE_DOC']);
  });

  it('shows fitting images and tables to the page for a test run as well', () => {
    expect(visibleFor('fitToPage')).toEqual(['LIVE_DOC', 'TEST_RUN', 'BASELINE_COLLECTION']);
  });

  it('shows the attachment fields for a test run only', () => {
    expect(visibleFor('testRunAttachments')).toEqual(['TEST_RUN']);
  });

  it('shows a row carrying no type restriction for every type', () => {
    // Paper size, the watermark, "cut local Polarion URLs" and the rest of the always-visible rows are not
    // in the map at all, which is what makes them unconditional.
    expect(isFieldVisible('fitToPage', 'LIVE_DOC')).toBe(true);
    expect(visibleFor('roles')).not.toEqual(ALL_TYPES);
  });
});

describe('which rows the request carries', () => {
  it('sends the roles, the comments and the query exactly where they are shown', () => {
    for (const field of [
      'renderComments',
      'roles',
      'workItemsQuery',
      'metadataFields',
      'testRunAttachments',
    ] as ExportFieldName[]) {
      expect(sentFor(field), field).toEqual(visibleFor(field));
    }
  });

  it('drops five rows a baseline collection shows, as the legacy popup did', () => {
    // A collection shows these and then exports without them. Reproduced from
    // ExportPopup.buildExportParams, whose guards were narrower than the markup's classes.
    for (const field of [
      'fitToPage',
      'cutEmptyChapters',
      'cutEmptyWorkitemAttributes',
      'markReferencedWorkitems',
      'localizeEnums',
    ] as ExportFieldName[]) {
      expect(isFieldVisible(field, 'BASELINE_COLLECTION'), field).toBe(true);
      expect(isFieldSent(field, 'BASELINE_COLLECTION'), field).toBe(false);
    }
  });

  it('still sends fitting to the page for a test run', () => {
    expect(sentFor('fitToPage')).toEqual(['LIVE_DOC', 'TEST_RUN']);
  });
});

describe('what the dialog offers per export type', () => {
  it('offers the automatic style package for bulk documents and collections only', () => {
    expect(isAutoSelectStylePackageAvailable('LIVE_DOC', 'BULK')).toBe(true);
    expect(isAutoSelectStylePackageAvailable('BASELINE_COLLECTION', 'BULK')).toBe(true);
    expect(isAutoSelectStylePackageAvailable('TEST_RUN', 'BULK')).toBe(false);
    expect(isAutoSelectStylePackageAvailable('LIVE_REPORT', 'BULK')).toBe(false);
    expect(isAutoSelectStylePackageAvailable('LIVE_DOC', 'SINGLE')).toBe(false);
  });

  it('offers a file name for a single export only', () => {
    expect(isFileNameOffered('SINGLE')).toBe(true);
    expect(isFileNameOffered('BULK')).toBe(false);
  });

  it('offers the page width validation only for a single export of a package that exposes it', () => {
    expect(isPageWidthValidationOffered('SINGLE', true)).toBe(true);
    expect(isPageWidthValidationOffered('SINGLE', false)).toBe(false);
    expect(isPageWidthValidationOffered('BULK', true)).toBe(false);
  });

  it('reads the link roles only where the type has any', () => {
    expect(ALL_TYPES.filter(areRolesSelectable)).toEqual(['LIVE_DOC', 'BASELINE_COLLECTION']);
  });

  it('reads the document language for one document, not for reports, test runs or a bulk run', () => {
    expect(isDocumentLanguageRead('LIVE_DOC', 'SINGLE')).toBe(true);
    expect(isDocumentLanguageRead('BASELINE_COLLECTION', 'SINGLE')).toBe(true);
    expect(isDocumentLanguageRead('WIKI_PAGE', 'SINGLE')).toBe(true);
    expect(isDocumentLanguageRead('LIVE_REPORT', 'SINGLE')).toBe(false);
    expect(isDocumentLanguageRead('TEST_RUN', 'SINGLE')).toBe(false);
    expect(isDocumentLanguageRead('LIVE_DOC', 'BULK')).toBe(false);
  });
});

describe('building the request for each document type', () => {
  const form = {
    ...toExportForm(SAMPLE_STYLE_PACKAGE_FULL),
    downloadAttachments: true,
    attachmentsFilter: '*.pdf',
    testcaseFieldId: 'exportIt',
    embedAttachments: true,
  };

  const built = (target: ExportTarget): ExportParamsJson => {
    const result = buildExportParams(form, SAMPLE_DOCUMENT, target);
    if ('error' in result) {
      throw new Error(`unexpected validation error: ${result.error.message}`);
    }
    return result.params;
  };

  it('carries everything for a Live Document', () => {
    const params = built({ documentType: 'LIVE_DOC', exportType: 'SINGLE' });

    expect(params.fitToPage).toBe(true);
    expect(params.cutEmptyChapters).toBe(true);
    expect(params.cutEmptyWIAttributes).toBe(true);
    expect(params.markReferencedWorkitems).toBe(true);
    expect(params.metadataFields).toEqual(['docOwner']);
    expect(params.language).toBe('de');
    expect(params.linkedWorkitemRoles).toEqual(['relates_to']);
    expect(params.urlQueryParameters).toEqual({ query: 'type:requirement' });
    // Not a test run and not a bulk run, so neither group of fields applies
    expect(params.attachmentsFilter).toBeNull();
    expect(params.embedAttachments).toBeNull();
    expect(params.autoSelectStylePackage).toBeNull();
  });

  it('carries the attachment fields for a test run, and nothing document-only', () => {
    const params = built({ documentType: 'TEST_RUN', exportType: 'SINGLE' });

    expect(params.attachmentsFilter).toBe('*.pdf');
    expect(params.testcaseFieldId).toBe('exportIt');
    expect(params.embedAttachments).toBe(true);
    expect(params.fitToPage).toBe(true);
    expect(params.cutEmptyChapters).toBe(false);
    expect(params.metadataFields).toBeNull();
    expect(params.renderComments).toBeNull();
    expect(params.language).toBeNull();
    expect(params.linkedWorkitemRoles).toEqual([]);
    expect(params.urlQueryParameters).toEqual({});
  });

  it('sends an empty filter rather than none when attachments are asked for without one', () => {
    const params = buildExportParams({ ...form, attachmentsFilter: '', testcaseFieldId: '' }, SAMPLE_DOCUMENT, {
      documentType: 'TEST_RUN',
      exportType: 'SINGLE',
    });

    expect('params' in params && params.params.attachmentsFilter).toBe('');
    // An empty custom field id means every test case, which the endpoint reads as an absent one
    expect('params' in params && params.params.testcaseFieldId).toBeNull();
  });

  it('drops the five rows a baseline collection shows but does not export', () => {
    const params = built({ documentType: 'BASELINE_COLLECTION', exportType: 'SINGLE' });

    expect(params.fitToPage).toBe(false);
    expect(params.cutEmptyChapters).toBe(false);
    expect(params.cutEmptyWIAttributes).toBe(false);
    expect(params.markReferencedWorkitems).toBe(false);
    expect(params.language).toBeNull();
    // What a collection does export
    expect(params.renderComments).toBe('OPEN');
    expect(params.linkedWorkitemRoles).toEqual(['relates_to']);
    expect(params.urlQueryParameters).toEqual({ query: 'type:requirement' });
  });

  it('carries only the unconditional rows for a report or a wiki page', () => {
    for (const documentType of ['LIVE_REPORT', 'WIKI_PAGE'] as DocumentType[]) {
      const params = built({ documentType, exportType: 'SINGLE' });

      expect(params.watermark, documentType).toBe(true);
      expect(params.cutLocalUrls, documentType).toBe(true);
      expect(params.fullFonts, documentType).toBe(true);
      expect(params.fitToPage, documentType).toBe(false);
      expect(params.renderComments, documentType).toBeNull();
      expect(params.linkedWorkitemRoles, documentType).toEqual([]);
      expect(params.urlQueryParameters, documentType).toEqual({});
      // The two value fields have no type guard in either dialog and are sent whatever is exported
      expect(params.chapters, documentType).toEqual(['1', '2']);
      expect(params.numberedListStyles, documentType).toBe('1ai');
    }
  });

  it('carries the automatic style package switch only for a bulk run that offers it', () => {
    expect(
      built({ documentType: 'LIVE_DOC', exportType: 'BULK', autoSelectStylePackage: true }).autoSelectStylePackage,
    ).toBe(true);
    expect(
      built({ documentType: 'LIVE_DOC', exportType: 'BULK', autoSelectStylePackage: false }).autoSelectStylePackage,
    ).toBe(false);
    expect(
      built({ documentType: 'TEST_RUN', exportType: 'BULK', autoSelectStylePackage: true }).autoSelectStylePackage,
    ).toBeNull();
  });
});
