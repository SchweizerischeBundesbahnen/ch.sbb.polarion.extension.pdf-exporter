import type { SelectOption } from '@sbb-polarion/react-sbb-polarion';
import {
  DEFAULT_HEADERS_COLOR,
  DEFAULT_IMAGE_DENSITY,
  DEFAULT_LINK_ROLE_DIRECTION,
  DEFAULT_NAME,
  DEFAULT_ORIENTATION,
  DEFAULT_PAPER_SIZE,
  DEFAULT_PDF_VARIANT,
  DEFAULT_RENDER_COMMENTS,
  LANGUAGES,
  type StylePackageSettings,
} from '../services/stylePackage';

/**
 * The state of an export dialog's form, and how a style package is read into it.
 *
 * This is the React port of the legacy `ExportPanel.stylePackageSelected()`, which pushed the selected
 * style package into the DOM field by field. The mapping is the same one, with the same fallbacks; what
 * differs is that a setting the style package expresses as "null means off" is two fields here - the
 * checkbox that switches it on and the value it carries - so unticking a box does not throw away what
 * the user typed before ticking it again. The Style Packages administration page models its own form the
 * same way, for the same reason.
 */
export interface ExportForm {
  coverPageEnabled: boolean;
  coverPage: string;
  css: string;
  headerFooter: string;
  localization: string;
  webhooksEnabled: boolean;
  webhooks: string;
  headersColor: string;
  paperSize: string;
  orientation: string;
  pdfVariant: string;
  imageDensity: string;
  fullFonts: boolean;
  fitToPage: boolean;
  followHTMLPresentationalHints: boolean;
  renderCommentsEnabled: boolean;
  renderComments: string;
  includeUnreferencedComments: boolean;
  renderNativeComments: boolean;
  watermark: boolean;
  cutEmptyChapters: boolean;
  cutEmptyWorkitemAttributes: boolean;
  cutLocalURLs: boolean;
  markReferencedWorkitems: boolean;
  specificChaptersEnabled: boolean;
  specificChapters: string;
  metadataFieldsEnabled: boolean;
  metadataFields: string;
  workItemsQueryEnabled: boolean;
  workItemsQuery: string;
  customListStylesEnabled: boolean;
  customNumberedListStyles: string;
  localizeEnums: boolean;
  language: string;
  languageCustomField: string;
  rolesEnabled: boolean;
  linkedWorkitemRoles: string[];
  linkRoleDirection: string;
  /**
   * The four test run fields. Only the toolbar popup shows them, and only for a test run, but they are part
   * of a style package like everything else here, so they are read into the form whatever is being exported.
   */
  downloadAttachments: boolean;
  attachmentsFilter: string;
  testcaseFieldId: string;
  embedAttachments: boolean;
}

export interface ExportFormContext {
  /**
   * The `docLanguage` custom field of the document, as `/document-language` returns it. A style package
   * that exposes its settings lets the document's own language win over the package's, which is what
   * makes the panel offer the language the document is actually written in.
   */
  documentLanguage?: string | null;
  /**
   * The `?query=` of the editor URL. The document is being viewed filtered, so an export started from
   * there should match what is on screen - it takes priority over the style package's own query.
   */
  urlQuery?: string | null;
}

/**
 * The language option a `docLanguage` value stands for, or `undefined` when it stands for none.
 *
 * `/document-language` returns the enum option id of the field, which is not guaranteed to be one of the
 * three ids offered here: it may be the display name ("Deutsch"), a different case, or English, which is
 * not an option at all because there is nothing to localize into. The legacy Java did the same match
 * against `Language.name()` and `Language.getValue()` before preselecting an option.
 */
export function resolveLanguage(documentLanguage: string | null | undefined): string | undefined {
  if (!documentLanguage) {
    return undefined;
  }
  const wanted = documentLanguage.toLowerCase();
  return LANGUAGES.find((option) => option.id.toLowerCase() === wanted || option.name.toLowerCase() === wanted)?.id;
}

/** The state an export form starts in for the given style package. */
export function toExportForm(content: StylePackageSettings, context: ExportFormContext = {}): ExportForm {
  const roles = content.linkedWorkitemRoles ?? [];
  const workItemsQuery = context.urlQuery || content.workItemsQuery || '';
  // The document's own language only overrides the package's where the package invites the user to
  // redefine its settings at all.
  const documentLanguage = content.exposeSettings ? resolveLanguage(context.documentLanguage) : undefined;
  return {
    coverPageEnabled: !!content.coverPage,
    coverPage: content.coverPage ?? DEFAULT_NAME,
    css: content.css ?? DEFAULT_NAME,
    headerFooter: content.headerFooter ?? DEFAULT_NAME,
    localization: content.localization ?? DEFAULT_NAME,
    webhooksEnabled: !!content.webhooks,
    webhooks: content.webhooks ?? DEFAULT_NAME,
    headersColor: content.headersColor ?? DEFAULT_HEADERS_COLOR,
    paperSize: content.paperSize ?? DEFAULT_PAPER_SIZE,
    orientation: content.orientation ?? DEFAULT_ORIENTATION,
    pdfVariant: content.pdfVariant ?? DEFAULT_PDF_VARIANT,
    imageDensity: content.imageDensity ?? DEFAULT_IMAGE_DENSITY,
    fullFonts: !!content.fullFonts,
    fitToPage: !!content.fitToPage,
    followHTMLPresentationalHints: !!content.followHTMLPresentationalHints,
    renderCommentsEnabled: !!content.renderComments,
    renderComments: content.renderComments ?? DEFAULT_RENDER_COMMENTS,
    includeUnreferencedComments: !!content.includeUnreferencedComments,
    renderNativeComments: !!content.renderNativeComments,
    watermark: !!content.watermark,
    cutEmptyChapters: !!content.cutEmptyChapters,
    cutEmptyWorkitemAttributes: !!content.cutEmptyWorkitemAttributes,
    cutLocalURLs: !!content.cutLocalURLs,
    markReferencedWorkitems: !!content.markReferencedWorkitems,
    specificChaptersEnabled: !!content.specificChapters,
    specificChapters: content.specificChapters ?? '',
    metadataFieldsEnabled: !!content.metadataFields,
    metadataFields: content.metadataFields ?? '',
    workItemsQueryEnabled: !!workItemsQuery,
    workItemsQuery,
    customListStylesEnabled: !!content.customNumberedListStyles,
    customNumberedListStyles: content.customNumberedListStyles ?? '',
    localizeEnums: !!content.language,
    language: documentLanguage ?? content.language ?? LANGUAGES[0].id,
    // Admin-only field (no control): carried from the package so the export request keeps it, like the popup does.
    languageCustomField: content.languageCustomField ?? '',
    rolesEnabled: roles.length > 0,
    linkedWorkitemRoles: roles,
    linkRoleDirection: content.linkRoleDirection ?? DEFAULT_LINK_ROLE_DIRECTION,
    // One switch over two values, the way the legacy popup derived it: attachments are downloaded when the
    // package names a filter or a test case field, either of them being enough.
    downloadAttachments: !!(content.attachmentsFilter || content.testcaseFieldId),
    attachmentsFilter: content.attachmentsFilter ?? '',
    testcaseFieldId: content.testcaseFieldId ?? '',
    embedAttachments: !!content.embedAttachments,
  };
}

/**
 * The configuration a child dropdown actually points at: a name the scope no longer offers falls back to
 * Default, which is what the legacy `ExtensionContext.setSelector` did. An empty or not-yet-loaded option
 * list leaves the stored reference alone, so a pending read cannot rewrite a perfectly good one.
 */
export function childValue(options: SelectOption[], value: string): string {
  if (options.length === 0 || options.some((option) => option.id === value)) {
    return value;
  }
  return DEFAULT_NAME;
}
