/**
 * What is being exported, and which fields of an export dialog that leaves standing.
 *
 * The DLE toolbar popup offers one form for five kinds of item, and used to switch its rows by writing
 * `display` onto elements carrying `visible-for-<TYPE>` / `not-visible-for-<TYPE>` classes
 * (`ExportPopup.openPopup`). Those classes are the only description of the rules there was, and the
 * document properties side panel - which exports a Live Document and nothing else - had no need for them.
 * They are one tested module here because two dialogs now render the same form.
 *
 * Note the deliberate split between {@link isFieldVisible} and {@link isFieldSent}: the legacy popup showed
 * a handful of fields for a type whose export request then dropped them. See {@link isFieldSent}.
 */

/** The document types the export endpoints understand. Mirrors the Java `DocumentType`. */
export type DocumentType = 'LIVE_DOC' | 'LIVE_REPORT' | 'TEST_RUN' | 'BASELINE_COLLECTION' | 'WIKI_PAGE';

/** One item picked on the page, or a set of them picked in the Bulk PDF Export widget. */
export type ExportType = 'SINGLE' | 'BULK';

/** Every row of the export form whose presence depends on what is being exported. */
export type ExportFieldName =
  | 'fitToPage'
  | 'renderComments'
  | 'cutEmptyChapters'
  | 'cutEmptyWorkitemAttributes'
  | 'markReferencedWorkitems'
  | 'customListStyles'
  | 'specificChapters'
  | 'metadataFields'
  | 'localizeEnums'
  | 'roles'
  | 'workItemsQuery'
  | 'testRunAttachments';

const DOCUMENT_AND_COLLECTION: DocumentType[] = ['LIVE_DOC', 'BASELINE_COLLECTION'];

/**
 * Which document types each field is shown for, transcribed from the `visible-for-*` classes of
 * `popupForm.html`. A field absent from this map is shown for every type (paper size, watermark, the
 * "cut local Polarion URLs" switch and so on carried no such class).
 */
const VISIBLE_FOR: Partial<Record<ExportFieldName, DocumentType[]>> = {
  fitToPage: ['LIVE_DOC', 'BASELINE_COLLECTION', 'TEST_RUN'],
  renderComments: DOCUMENT_AND_COLLECTION,
  cutEmptyChapters: DOCUMENT_AND_COLLECTION,
  cutEmptyWorkitemAttributes: DOCUMENT_AND_COLLECTION,
  markReferencedWorkitems: DOCUMENT_AND_COLLECTION,
  customListStyles: DOCUMENT_AND_COLLECTION,
  specificChapters: DOCUMENT_AND_COLLECTION,
  metadataFields: ['LIVE_DOC'],
  localizeEnums: DOCUMENT_AND_COLLECTION,
  roles: DOCUMENT_AND_COLLECTION,
  workItemsQuery: DOCUMENT_AND_COLLECTION,
  testRunAttachments: ['TEST_RUN'],
};

/**
 * Which document types each field is actually **sent** for, transcribed from the type guards of
 * `ExportPopup.buildExportParams`.
 *
 * Five of them are narrower than {@link VISIBLE_FOR}, all in the same direction: a baseline collection
 * shows "Fit images and tables to page", "Mark referenced Workitems", the two "Cut empty ..." switches and
 * "Localize enums", and then the request leaves every one of them out. That is what the extension has
 * always done, so it is what this port does; it is transcribed as data rather than buried in a builder so
 * that the divergence is visible and one line to change if it is ever decided to be a bug.
 */
const SENT_FOR: Partial<Record<ExportFieldName, DocumentType[]>> = {
  ...VISIBLE_FOR,
  fitToPage: ['LIVE_DOC', 'TEST_RUN'],
  cutEmptyChapters: ['LIVE_DOC'],
  cutEmptyWorkitemAttributes: ['LIVE_DOC'],
  markReferencedWorkitems: ['LIVE_DOC'],
  localizeEnums: ['LIVE_DOC'],
};

const applies = (types: DocumentType[] | undefined, documentType: DocumentType): boolean =>
  types === undefined || types.includes(documentType);

/** Whether the export form shows this field for the given document type. */
export function isFieldVisible(field: ExportFieldName, documentType: DocumentType): boolean {
  return applies(VISIBLE_FOR[field], documentType);
}

/**
 * Whether an export request for the given document type carries this field. Not the same question as
 * {@link isFieldVisible} - see {@link SENT_FOR}.
 */
export function isFieldSent(field: ExportFieldName, documentType: DocumentType): boolean {
  return applies(SENT_FOR[field], documentType);
}

/**
 * Whether work item link roles apply at all.
 *
 * Reports, test runs and wiki pages carry no link roles, which is why the popup skipped reading them
 * (`ExportPopup.rolesSelectable`) rather than offering an empty selector. Bulk changes nothing here: a bulk
 * export runs the per-item conversion for each selected item.
 */
export const areRolesSelectable = (documentType: DocumentType): boolean => isFieldVisible('roles', documentType);

/**
 * Whether the dialog offers to pick the most suitable style package per item instead of one for all of them.
 *
 * Only a bulk export can, and only over documents and collections - `ExportPopup.autoSelectStylePackageAvailable`.
 */
export const isAutoSelectStylePackageAvailable = (documentType: DocumentType, exportType: ExportType): boolean =>
  exportType === 'BULK' && DOCUMENT_AND_COLLECTION.includes(documentType);

/** A bulk export names each file after its own item, so the dialog has no file name to offer. */
export const isFileNameOffered = (exportType: ExportType): boolean => exportType !== 'BULK';

/**
 * Whether the page-width validation is offered. A bulk export never validates - it would mean one
 * validation run per selected item - so the style package's `exposePageWidthValidation` only counts for a
 * single export (`ExportPopup.stylePackageSelected`).
 */
export const isPageWidthValidationOffered = (exportType: ExportType, exposePageWidthValidation: boolean): boolean =>
  exportType !== 'BULK' && exposePageWidthValidation;

/** The document's own language is only read where there is one document with a `docLanguage` field. */
export const isDocumentLanguageRead = (documentType: DocumentType, exportType: ExportType): boolean =>
  exportType !== 'BULK' && documentType !== 'LIVE_REPORT' && documentType !== 'TEST_RUN';
