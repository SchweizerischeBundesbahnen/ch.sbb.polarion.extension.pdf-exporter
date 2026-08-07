import type { SelectOption } from '@grigoriev/react-sbb-polarion';
import type { PanelData } from '../src/export/exportData';
import type { ConversionResult } from '../src/services/conversion';
import type { DocumentIdentity } from '../src/services/exportContext';
import type { StylePackageSettings } from '../src/services/stylePackage';
import type { SidePanelDependencies } from '../src/sidepanel/SidePanel';

/**
 * A side panel filled in without a Polarion behind it: the fixture the panel's suites share.
 *
 * The panel reads its data over REST, reads the document out of the editor URL and runs a conversion, none
 * of which a browser test has. This stands in for all three, so the behavior suites and the visual
 * references describe the same panel.
 *
 * It lived next to the panel in `src/` while the development harness rendered stubbed states. The harness
 * now runs the real panel against a real document (`pages/SidePanelPreview.tsx`), so nothing in `src/` uses this any
 * more and it belongs here - out of the shipped tree and out of the coverage denominator, a fixture's
 * coverage percentage meaning nothing.
 */

const NAMES: SelectOption[] = [
  { id: 'Default', name: 'Default' },
  { id: 'SBB', name: 'SBB' },
];

export const SAMPLE_DOCUMENT: DocumentIdentity = {
  documentType: 'LIVE_DOC',
  scope: 'project/elibrary/',
  projectId: 'elibrary',
  locationPath: 'Default Space/Cross Link Issue',
  spaceId: 'Default Space',
  documentName: 'Cross Link Issue',
  urlQueryParameters: {},
};

export const SAMPLE_PANEL_DATA: PanelData = {
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
  exportPermission: 'granted',
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

/** A style package that keeps its settings to itself: only the file name and the buttons are offered. */
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
};

/** A conversion result, as a finished job produces one. */
export const pdfResult = (warning: string | null = null): ConversionResult => ({
  blob: new Blob(['pdf']),
  fileName: null,
  warning,
});

/** A conversion that never finishes, so the panel can be looked at while an export is running. */
const NEVER_COMPLETES = (): Promise<ConversionResult> => new Promise<ConversionResult>(() => {});

export interface SampleOptions {
  /** The style package the panel loads. Defaults to the one the screenshots were taken with. */
  stylePackage?: StylePackageSettings;
  /** Fields of the panel data to override (permission, webhooks, roles, ...). */
  data?: Partial<PanelData>;
  /** What an export does, given the request body. Left out, it never completes: the in-progress state. */
  convert?: (request: string) => Promise<ConversionResult>;
  /** Called instead of the browser download, so a test can assert on the file that was produced. */
  download?: (blob: Blob, fileName: string) => void;
}

/** Dependencies that answer from the sample data instead of the network. */
export function sampleDependencies(options: SampleOptions = {}): SidePanelDependencies {
  const convert = options.convert ?? NEVER_COMPLETES;
  return {
    location: SAMPLE_DOCUMENT,
    loadData: () => Promise.resolve({ ...SAMPLE_PANEL_DATA, ...options.data }),
    loadPackage: () => Promise.resolve(options.stylePackage ?? SAMPLE_STYLE_PACKAGE),
    convert: (_remote, request) => convert(request),
    download: options.download ?? (() => {}),
  };
}
