import type { DocumentType, ExportFieldName, ExportType } from './documentType';
import { isAutoSelectStylePackageAvailable, isFieldSent } from './documentType';
import type { ExportForm } from './exportForm';
import { CHAPTERS_ERROR, parseChapters, parseMetadataFields, validateNumberedListStyles } from './validation';

/**
 * The export request the conversion endpoints take, built from an export dialog's form.
 *
 * The field names are the product `ExportParams`' own (`cutEmptyWIAttributes`, `cutLocalUrls`, ...), so
 * this is the same body the legacy `ExportPanel.buildRequestJson()` and `ExportPopup.buildExportParams()`
 * produced. Whatever is switched off is left out rather than sent as null, which is what the legacy
 * `toJSON()` did with its null filter.
 *
 * Both dialogs build their request here: the document properties side panel, which exports a Live Document,
 * and the toolbar popup, which exports any of the five document types either singly or in bulk. Which fields
 * a type carries is {@link isFieldSent}'s answer, not this module's.
 */
export interface ExportParamsJson {
  documentType: string;
  projectId?: string | null;
  locationPath?: string | null;
  baselineRevision?: string | null;
  revision?: string | null;
  coverPage?: string | null;
  css?: string;
  headerFooter?: string;
  localization?: string;
  webhooks?: string | null;
  headersColor?: string;
  paperSize?: string;
  orientation?: string;
  pdfVariant?: string;
  imageDensity?: string;
  fullFonts?: boolean;
  fitToPage?: boolean;
  renderComments?: string | null;
  renderNativeComments?: boolean;
  includeUnreferencedComments?: boolean;
  watermark?: boolean;
  markReferencedWorkitems?: boolean;
  cutEmptyChapters?: boolean;
  cutEmptyWIAttributes?: boolean;
  cutLocalUrls?: boolean;
  followHTMLPresentationalHints?: boolean;
  numberedListStyles?: string | null;
  chapters?: string[] | null;
  metadataFields?: string[] | null;
  language?: string | null;
  languageCustomField?: string | null;
  linkedWorkitemRoles?: string[];
  linkRoleDirection?: string | null;
  fileName?: string;
  urlQueryParameters?: Record<string, string>;
  attachmentsFilter?: string | null;
  testcaseFieldId?: string | null;
  embedAttachments?: boolean | null;
  autoSelectStylePackage?: boolean | null;
}

/** Where the item being exported lives, as the page URL says. */
export interface DocumentContext {
  projectId?: string | null;
  locationPath?: string | null;
  baselineRevision?: string | null;
  revision?: string | null;
  urlQueryParameters?: Record<string, string>;
}

/** What is being exported, and how - which together decide the fields the request carries. */
export interface ExportTarget {
  documentType: DocumentType;
  exportType: ExportType;
  /** Bulk over documents or collections only: let the server pick the best style package per item. */
  autoSelectStylePackage?: boolean;
  fileName?: string;
}

/** Which field a validation error belongs to, so the form can mark it. */
export type ExportField = 'chapters' | 'numberedListStyles';

export interface ExportValidationError {
  field: ExportField;
  message: string;
}

export type BuildResult = { params: ExportParamsJson } | { error: ExportValidationError };

/**
 * The export request for the given target, or the first validation error standing in its way.
 *
 * The three fields that carry a value are validated in the order the legacy dialogs validated them, and the
 * first bad one stops the build - the dialog then marks that field and shows the message, as it always did.
 * Two of those three - the chapters and the numbered list styles - have no document type guard in either
 * dialog, so they are validated and sent whatever is being exported.
 */
export function buildExportParams(form: ExportForm, context: DocumentContext, target: ExportTarget): BuildResult {
  const { documentType, exportType } = target;
  const sent = (field: ExportFieldName): boolean => isFieldSent(field, documentType);

  let chapters: string[] | null = null;
  if (form.specificChaptersEnabled) {
    const parsed = parseChapters(form.specificChapters);
    if (!parsed) {
      return { error: { field: 'chapters', message: CHAPTERS_ERROR } };
    }
    chapters = parsed;
  }

  // No emptiness check, on purpose. The legacy dialogs tested `if (!selectedMetadataFields)` against the
  // array `getSelectedMetadataFields()` always returns, so that branch - and METADATA_FIELDS_ERROR with
  // it - was unreachable and an empty entry exported as `metadataFields: []`. Reproducing the dialogs means
  // reproducing that: turning the dead check on would change what an export sends, which is not this
  // migration's business. The message is exported for whoever decides to make it reachable.
  const metadataFields: string[] | null =
    sent('metadataFields') && form.metadataFieldsEnabled ? parseMetadataFields(form.metadataFields) : null;

  let numberedListStyles: string | null = null;
  if (form.customListStylesEnabled) {
    const message = validateNumberedListStyles(form.customNumberedListStyles);
    if (message) {
      return { error: { field: 'numberedListStyles', message } };
    }
    numberedListStyles = form.customNumberedListStyles;
  }

  // Only where roles apply. The popup element used to be reused across opens, so an export type without
  // link roles must not send role options left selected by a previous document's export.
  const roles = sent('roles') && form.rolesEnabled ? form.linkedWorkitemRoles : [];

  const attachments = sent('testRunAttachments') && form.downloadAttachments;
  const autoSelect = isAutoSelectStylePackageAvailable(documentType, exportType);

  return {
    params: {
      documentType,
      projectId: context.projectId,
      locationPath: context.locationPath,
      baselineRevision: context.baselineRevision,
      revision: context.revision,
      coverPage: form.coverPageEnabled ? form.coverPage : null,
      css: form.css,
      headerFooter: form.headerFooter,
      localization: form.localization,
      webhooks: form.webhooksEnabled ? form.webhooks : null,
      headersColor: form.headersColor,
      paperSize: form.paperSize,
      orientation: form.orientation,
      pdfVariant: form.pdfVariant,
      imageDensity: form.imageDensity,
      fullFonts: form.fullFonts,
      fitToPage: sent('fitToPage') && form.fitToPage,
      renderComments: sent('renderComments') && form.renderCommentsEnabled ? form.renderComments : null,
      renderNativeComments: form.renderNativeComments,
      includeUnreferencedComments: form.includeUnreferencedComments,
      watermark: form.watermark,
      markReferencedWorkitems: sent('markReferencedWorkitems') && form.markReferencedWorkitems,
      cutEmptyChapters: sent('cutEmptyChapters') && form.cutEmptyChapters,
      cutEmptyWIAttributes: sent('cutEmptyWorkitemAttributes') && form.cutEmptyWorkitemAttributes,
      cutLocalUrls: form.cutLocalURLs,
      followHTMLPresentationalHints: form.followHTMLPresentationalHints,
      numberedListStyles,
      chapters,
      metadataFields,
      language: sent('localizeEnums') && form.localizeEnums ? form.language : null,
      // No document type guard, the way the legacy dialogs sent it: an admin-only field with no control,
      // passed through from the selected style package whatever is being exported.
      languageCustomField: form.languageCustomField || null,
      linkedWorkitemRoles: roles,
      linkRoleDirection: roles.length > 0 ? form.linkRoleDirection : null,
      fileName: target.fileName,
      urlQueryParameters: urlQueryParameters(form, context, documentType),
      // The three test run fields and the bulk auto-select switch are left out entirely where they do not
      // apply. The legacy popup sent the two booleans as `false` there instead; both are primitive booleans
      // on the Java side, where an absent one is already `false`, so the request means the same - and the
      // side panel's own request, which never carried any of them, stays exactly as it was.
      attachmentsFilter: attachments ? (form.attachmentsFilter ?? '') : null,
      testcaseFieldId: attachments && form.testcaseFieldId ? form.testcaseFieldId : null,
      embedAttachments: sent('testRunAttachments') ? attachments && form.embedAttachments : null,
      autoSelectStylePackage: autoSelect ? !!target.autoSelectStylePackage : null,
    },
  };
}

/**
 * The page URL's own query parameters, with `query` reflecting the work items query field: set when the
 * switch is on, dropped when it is off. The export has to carry the whole set because the renderer reads
 * the item the same way the page does.
 */
function urlQueryParameters(
  form: ExportForm,
  context: DocumentContext,
  documentType: DocumentType,
): Record<string, string> {
  const parameters = { ...(context.urlQueryParameters ?? {}) };
  if (isFieldSent('workItemsQuery', documentType) && form.workItemsQueryEnabled) {
    parameters.query = form.workItemsQuery || '';
  } else {
    delete parameters.query;
  }
  return parameters;
}

/**
 * The request body for the conversion endpoints: the params as JSON, without what is not set.
 *
 * The product's `ExportParams.toJSON()` filtered null and undefined out before stringifying, and the
 * server relies on it - a `null` cover page and an absent one do not mean the same thing to every field.
 */
export function toRequestBody(params: ExportParamsJson | Record<string, unknown>): string {
  const defined = Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== null && value !== undefined),
  );
  return JSON.stringify(defined, null, 2);
}
