import type { SelectOption } from '@sbb-polarion/react-sbb-polarion';
import type { PopupData } from '../src/export/exportData';
import type { ExportPopupDependencies } from '../src/popup/ExportPopupModal';
import type { ConversionResult } from '../src/services/conversion';
import type { DocumentIdentity } from '../src/services/exportContext';
import type { StylePackageSettings } from '../src/services/stylePackage';

/**
 * An export dialog filled in without a Polarion behind it: the fixture the dialog's suites share, and the
 * one the bulk export run drives the dialog with.
 *
 * The dialog reads its data over REST and runs a conversion, neither of which a browser test has. This
 * stands in for both, so the behavior suites, the visual references and the widget's run all describe the
 * same dialog.
 */

const NAMES: SelectOption[] = [
  { id: 'Default', name: 'Default' },
  { id: 'SBB', name: 'SBB' },
];

/** A Live Document, as the document editor toolbar opens the dialog for one. */
export const SAMPLE_DOCUMENT: DocumentIdentity = {
  documentType: 'LIVE_DOC',
  scope: 'project/elibrary/',
  projectId: 'elibrary',
  locationPath: '_default/Cross Link Issue',
  spaceId: '_default',
  documentName: 'Cross Link Issue',
  urlQueryParameters: {},
};

/** Where a document type addressed by an id sits: a project scope and no location path at all. */
export const SAMPLE_TEST_RUN: DocumentIdentity = {
  documentType: 'TEST_RUN',
  scope: 'project/elibrary/',
  projectId: 'elibrary',
  urlQueryParameters: { id: 'build_quick-20170211-141155' },
};

export const SAMPLE_POPUP_DATA: PopupData = {
  stylePackages: [
    { id: 'Default', name: 'Default' },
    { id: 'Specification', name: 'Specification' },
  ],
  childNames: {
    'cover-page': NAMES,
    css: NAMES,
    'header-footer': NAMES,
    localization: NAMES,
    webhooks: NAMES,
  },
  roles: [
    { id: 'relates_to', name: 'relates_to' },
    { id: 'depends_on', name: 'depends_on' },
    { id: 'verifies', name: 'verifies' },
  ],
  fileName: 'E-Library Cross Link Issue.pdf',
  documentLanguage: 'de',
  webhooksEnabled: false,
};

/** The style package of the screenshots: settings exposed, page width validation offered. */
export const SAMPLE_STYLE_PACKAGE: StylePackageSettings = {
  exposeSettings: true,
  exposePageWidthValidation: true,
  css: 'Default',
  headerFooter: 'Default',
  localization: 'Default',
  headersColor: '#004d73',
  paperSize: 'A4',
  orientation: 'PORTRAIT',
  pdfVariant: 'PDF_A_2B',
  imageDensity: 'DPI_96',
  fitToPage: true,
  followHTMLPresentationalHints: true,
  cutEmptyWorkitemAttributes: true,
};

/** A package that keeps its settings to itself: only the package name, the file name and the buttons. */
export const SAMPLE_STYLE_PACKAGE_HIDDEN: StylePackageSettings = {
  ...SAMPLE_STYLE_PACKAGE,
  exposeSettings: false,
  exposePageWidthValidation: false,
};

/** Every optional field switched on, which is what puts the value-carrying rows on screen. */
export const SAMPLE_STYLE_PACKAGE_FULL: StylePackageSettings = {
  ...SAMPLE_STYLE_PACKAGE,
  coverPage: 'Default',
  webhooks: 'Default',
  renderComments: 'OPEN',
  includeUnreferencedComments: true,
  watermark: true,
  cutEmptyChapters: true,
  cutLocalURLs: true,
  markReferencedWorkitems: true,
  specificChapters: '1,2',
  metadataFields: 'docOwner',
  workItemsQuery: 'type:requirement',
  customNumberedListStyles: '1ai',
  language: 'de',
  linkedWorkitemRoles: ['relates_to'],
  linkRoleDirection: 'BOTH',
  fullFonts: true,
  attachmentsFilter: '*.*',
  testcaseFieldId: 'exportIt',
};

/** A conversion result, as a finished job produces one. */
export const pdfResult = (warning: string | null = null, fileName: string | null = null): ConversionResult => ({
  blob: new Blob(['pdf']),
  fileName,
  warning,
});

/** A conversion that never finishes, so the dialog can be looked at while an export is running. */
const NEVER_COMPLETES = (): Promise<ConversionResult> => new Promise<ConversionResult>(() => {});

export interface PopupSampleOptions {
  /** The style package the dialog loads. Defaults to the one the screenshots were taken with. */
  stylePackage?: StylePackageSettings;
  /** Fields of the dialog's data to override (webhooks, roles, style packages, ...). */
  data?: Partial<PopupData>;
  /** Fails the whole read, which is what leaves the dialog unusable. */
  loadError?: Error;
  /** What an export does, given the request body. Left out, it never completes: the in-progress state. */
  convert?: (request: string) => Promise<ConversionResult>;
  download?: (blob: Blob, fileName: string) => void;
  downloadAttachments?: ExportPopupDependencies['downloadAttachments'];
}

/**
 * The REST routes the dialog reads, for the one suite that mounts it through `openExportPopup` rather than
 * rendering it with stubbed dependencies. Shaped as `mockFetch` wants them; the caller adds the `/validate`
 * route if it needs one.
 */
export const popupRoutes = () => {
  const names = (...values: string[]) => values.map((name) => ({ name, scope: 'project/elibrary/' }));
  return [
    { method: 'POST', match: /\/settings\/style-package\/suitable-names/, json: names('Default', 'SBB') },
    { method: 'GET', match: /\/settings\/style-package\/names\/[^/]+\/content/, json: SAMPLE_STYLE_PACKAGE },
    { method: 'GET', match: /\/settings\/cover-page\/names/, json: names('Default') },
    { method: 'GET', match: /\/settings\/css\/names/, json: names('Default') },
    { method: 'GET', match: /\/settings\/header-footer\/names/, json: names('Default') },
    { method: 'GET', match: /\/settings\/localization\/names/, json: names('Default') },
    { method: 'GET', match: /\/settings\/webhooks\/names/, json: names('Default') },
    { method: 'GET', match: /\/link-role-names/, json: ['relates_to'] },
    { method: 'POST', match: /\/export-filename/, respond: () => new Response('E-Library Cross Link Issue.pdf') },
    { method: 'GET', match: /\/document-language/, respond: () => new Response('de') },
    { method: 'GET', match: /\/webhooks\/status/, json: { enabled: false } },
  ];
};

/** Dependencies that answer from the sample data instead of the network. */
export function popupDependencies(options: PopupSampleOptions = {}): ExportPopupDependencies {
  const convert = options.convert ?? NEVER_COMPLETES;
  return {
    loadData: () =>
      options.loadError
        ? Promise.reject(options.loadError)
        : Promise.resolve({ ...SAMPLE_POPUP_DATA, ...options.data }),
    loadPackage: () => Promise.resolve(options.stylePackage ?? SAMPLE_STYLE_PACKAGE),
    convert: (_remote, request) => convert(request),
    download: options.download ?? (() => {}),
    downloadAttachments: options.downloadAttachments ?? (() => Promise.resolve()),
  };
}
