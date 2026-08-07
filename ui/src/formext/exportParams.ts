import type { ExportForm } from './exportForm';
import { CHAPTERS_ERROR, parseChapters, parseMetadataFields, validateNumberedListStyles } from './validation';

/**
 * The export request the conversion endpoints take, built from an export dialog's form.
 *
 * The field names are the product `ExportParams`' own (`cutEmptyWIAttributes`, `cutLocalUrls`, ...), so
 * this is the same body the legacy `ExportPanel.buildRequestJson()` produced. Whatever is switched off is
 * left out rather than sent as null, which is what the legacy `toJSON()` did with its null filter.
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
}

/** Where the document being exported lives, as the editor URL says. */
export interface DocumentContext {
  projectId?: string | null;
  locationPath?: string | null;
  baselineRevision?: string | null;
  revision?: string | null;
  urlQueryParameters?: Record<string, string>;
}

/** Which field a validation error belongs to, so the form can mark it. */
export type ExportField = 'chapters' | 'numberedListStyles';

export interface ExportValidationError {
  field: ExportField;
  message: string;
}

export type BuildResult = { params: ExportParamsJson } | { error: ExportValidationError };

/**
 * The export request for a Live Document, or the first validation error standing in its way.
 *
 * The three fields that carry a value are validated in the order the legacy panel validated them, and the
 * first bad one stops the build - the panel then marks that field and shows the message, as it always did.
 * The side panel exports a Live Document and nothing else, which is why the flags the product gates on the
 * document type (fit to page, the two "cut empty" switches, metadata fields, localized enums) are simply
 * the form's own values here.
 */
export function buildExportParams(form: ExportForm, context: DocumentContext, fileName?: string): BuildResult {
  let chapters: string[] | null = null;
  if (form.specificChaptersEnabled) {
    const parsed = parseChapters(form.specificChapters);
    if (!parsed) {
      return { error: { field: 'chapters', message: CHAPTERS_ERROR } };
    }
    chapters = parsed;
  }

  // No emptiness check, on purpose. The legacy panel tested `if (!selectedMetadataFields)` against the
  // array `getSelectedMetadataFields()` always returns, so that branch - and METADATA_FIELDS_ERROR with
  // it - was unreachable and an empty entry exported as `metadataFields: []`. Reproducing the panel means
  // reproducing that: turning the dead check on would change what an export sends, which is not this
  // migration's business. The message is exported for whoever decides to make it reachable.
  const metadataFields: string[] | null = form.metadataFieldsEnabled ? parseMetadataFields(form.metadataFields) : null;

  let numberedListStyles: string | null = null;
  if (form.customListStylesEnabled) {
    const message = validateNumberedListStyles(form.customNumberedListStyles);
    if (message) {
      return { error: { field: 'numberedListStyles', message } };
    }
    numberedListStyles = form.customNumberedListStyles;
  }

  const roles = form.rolesEnabled ? form.linkedWorkitemRoles : [];

  return {
    params: {
      documentType: 'LIVE_DOC',
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
      fitToPage: form.fitToPage,
      renderComments: form.renderCommentsEnabled ? form.renderComments : null,
      renderNativeComments: form.renderNativeComments,
      includeUnreferencedComments: form.includeUnreferencedComments,
      watermark: form.watermark,
      markReferencedWorkitems: form.markReferencedWorkitems,
      cutEmptyChapters: form.cutEmptyChapters,
      cutEmptyWIAttributes: form.cutEmptyWorkitemAttributes,
      cutLocalUrls: form.cutLocalURLs,
      followHTMLPresentationalHints: form.followHTMLPresentationalHints,
      numberedListStyles,
      chapters,
      metadataFields,
      language: form.localizeEnums ? form.language : null,
      languageCustomField: form.languageCustomField || null,
      linkedWorkitemRoles: roles,
      linkRoleDirection: roles.length > 0 ? form.linkRoleDirection : null,
      fileName,
      urlQueryParameters: urlQueryParameters(form, context),
    },
  };
}

/**
 * The editor URL's own query parameters, with `query` reflecting the work items query field: set when the
 * switch is on, dropped when it is off. The export has to carry the whole set because the renderer reads
 * the document the same way the editor does.
 */
function urlQueryParameters(form: ExportForm, context: DocumentContext): Record<string, string> {
  const parameters = { ...(context.urlQueryParameters ?? {}) };
  if (form.workItemsQueryEnabled) {
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
export function toRequestBody(params: ExportParamsJson): string {
  const defined = Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== null && value !== undefined),
  );
  return JSON.stringify(defined, null, 2);
}
