import type { SelectOption } from '@sbb-polarion/react-sbb-polarion';

/**
 * The style package: what one named `style-package` configuration holds, and the fixed option lists its
 * values come from.
 *
 * It is shared rather than owned by a page because a style package is read on three surfaces: the Style
 * Packages administration page edits it, and the export dialogs (the Document Properties side panel and
 * the DLE toolbar popup) load the selected one to preset their own fields from it. All three offered the
 * same lists of paper sizes, PDF variants and so on, in three copies, until this module.
 */

/** Content of one named `style-package` configuration, as `StylePackageModel` serializes it. */
export interface StylePackageSettings {
  matchingQuery?: string | null;
  weight?: number | null;
  exposeSettings?: boolean;
  coverPage?: string | null;
  headerFooter?: string | null;
  css?: string | null;
  localization?: string | null;
  webhooks?: string | null;
  headersColor?: string | null;
  paperSize?: string | null;
  orientation?: string | null;
  pdfVariant?: string | null;
  imageDensity?: string | null;
  fullFonts?: boolean;
  fitToPage?: boolean;
  renderComments?: string | null;
  renderNativeComments?: boolean;
  includeUnreferencedComments?: boolean;
  watermark?: boolean;
  markReferencedWorkitems?: boolean;
  cutEmptyChapters?: boolean;
  cutEmptyWorkitemAttributes?: boolean;
  cutLocalURLs?: boolean;
  followHTMLPresentationalHints?: boolean;
  specificChapters?: string | null;
  metadataFields?: string | null;
  customNumberedListStyles?: string | null;
  language?: string | null;
  languageCustomField?: string | null;
  linkedWorkitemRoles?: string[] | null;
  linkRoleDirection?: string | null;
  workItemsQuery?: string | null;
  exposePageWidthValidation?: boolean;
  attachmentsFilter?: string | null;
  testcaseFieldId?: string | null;
  embedAttachments?: boolean;
}

/** The configuration every child dropdown falls back to, and the one whose matching query is unused. */
export const DEFAULT_NAME = 'Default';

/** The `value` of the JSP page's color input, kept so an unset color looks the way it always did. */
export const DEFAULT_HEADERS_COLOR = '#004d73';

/** What a style package leaves unset, resolved the way the renderer resolves it. */
export const DEFAULT_PAPER_SIZE = 'A4';
export const DEFAULT_ORIENTATION = 'PORTRAIT';
export const DEFAULT_PDF_VARIANT = 'PDF_A_2B';
export const DEFAULT_IMAGE_DENSITY = 'DPI_96';
export const DEFAULT_RENDER_COMMENTS = 'OPEN';
export const DEFAULT_LINK_ROLE_DIRECTION = 'BOTH';

/**
 * The named settings a style package points at. Each of them is an administration page of its own, so
 * the pages that read them only list their names - they never read their content.
 */
export const CHILD_SETTINGS = ['cover-page', 'css', 'header-footer', 'localization', 'webhooks'] as const;
export type ChildSetting = (typeof CHILD_SETTINGS)[number];
export type ChildNames = Record<ChildSetting, SelectOption[]>;

export const NO_CHILD_NAMES: ChildNames = {
  'cover-page': [],
  css: [],
  'header-footer': [],
  localization: [],
  webhooks: [],
};

export const PAPER_SIZES: SelectOption[] = [
  { id: 'A5', name: 'A5' },
  { id: 'A4', name: 'A4' },
  { id: 'A3', name: 'A3' },
  { id: 'B5', name: 'B5' },
  { id: 'B4', name: 'B4' },
  { id: 'JIS_B5', name: 'JIS-B5' },
  { id: 'JIS_B4', name: 'JIS-B4' },
  { id: 'LETTER', name: 'Letter' },
  { id: 'LEGAL', name: 'Legal' },
  { id: 'LEDGER', name: 'Ledger' },
];

export const ORIENTATIONS: SelectOption[] = [
  { id: 'PORTRAIT', name: 'Portrait' },
  { id: 'LANDSCAPE', name: 'Landscape' },
];

export const PDF_VARIANTS: SelectOption[] = [
  { id: 'PDF_A_1A', name: 'pdf/a-1a' },
  { id: 'PDF_A_1B', name: 'pdf/a-1b' },
  { id: 'PDF_A_2A', name: 'pdf/a-2a' },
  { id: 'PDF_A_2B', name: 'pdf/a-2b' },
  { id: 'PDF_A_2U', name: 'pdf/a-2u' },
  { id: 'PDF_A_3A', name: 'pdf/a-3a' },
  { id: 'PDF_A_3B', name: 'pdf/a-3b' },
  { id: 'PDF_A_3U', name: 'pdf/a-3u' },
  { id: 'PDF_A_4E', name: 'pdf/a-4e' },
  { id: 'PDF_A_4F', name: 'pdf/a-4f' },
  { id: 'PDF_A_4U', name: 'pdf/a-4u' },
  { id: 'PDF_UA_1', name: 'pdf/ua-1' },
  { id: 'PDF_UA_2', name: 'pdf/ua-2' },
];

export const IMAGE_DENSITIES: SelectOption[] = [
  { id: 'DPI_96', name: '96 dpi' },
  { id: 'DPI_192', name: '192 dpi' },
  { id: 'DPI_300', name: '300 dpi' },
  { id: 'DPI_600', name: '600 dpi' },
];

export const COMMENTS_RENDER_TYPES: SelectOption[] = [
  { id: 'OPEN', name: 'Open' },
  { id: 'ALL', name: 'All' },
];

export const LANGUAGES: SelectOption[] = [
  { id: 'de', name: 'Deutsch' },
  { id: 'fr', name: 'Français' },
  { id: 'it', name: 'Italiano' },
];

export const LINK_ROLE_DIRECTIONS: SelectOption[] = [
  { id: 'BOTH', name: 'Both directions' },
  { id: 'DIRECT', name: 'Direct only' },
  { id: 'REVERSE', name: 'Reverse only' },
];

export const FULL_FONTS_HELP =
  'When enabled, fonts are embedded in their entirety without subsetting: full glyph coverage and better ' +
  'editability of the resulting PDF, at the cost of a larger file. This is not a robustness switch: ' +
  'subsetting already keeps a font whole when it cannot be applied, while skipping it can itself fail on ' +
  'a damaged font.';

export const UNREFERENCED_COMMENTS_HELP = 'Unreferenced comments will be rendered at the end of the document';

export const NATIVE_COMMENTS_HELP = 'Comments will be transformed into native PDF sticky notes/bubbles';
