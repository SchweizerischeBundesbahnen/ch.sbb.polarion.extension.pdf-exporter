import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { snapshotFeature } from './visualHelpers';

// Docker-only snapshot of the Style Packages page: the whole two-column form with every section on
// screen, including the sub-controls that only appear while their switch is on.

const origUrl = window.location.pathname + window.location.search;

const SCOPE = 'project/elibrary/';

const childNames = (name: string) => [
  { name: 'Default', scope: SCOPE },
  { name, scope: '' },
];

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-style-package=; path=/; max-age=0';
});

/** The routes of the page, with a style package which sets every section, and what `content` adds to it. */
const routes = (content: Record<string, unknown> = {}) => [
  { method: 'GET', match: /\/webhooks\/status/, json: { enabled: true } },
  { method: 'GET', match: /\/link-role-names/, json: ['relates_to', 'verifies'] },
  { method: 'GET', match: /\/settings\/cover-page\/names\?/, json: childNames('Fancy cover') },
  { method: 'GET', match: /\/settings\/css\/names\?/, json: childNames('Compact') },
  { method: 'GET', match: /\/settings\/header-footer\/names\?/, json: childNames('With logo') },
  { method: 'GET', match: /\/settings\/localization\/names\?/, json: childNames('German') },
  { method: 'GET', match: /\/settings\/webhooks\/names\?/, json: childNames('Rewriter') },
  {
    method: 'GET',
    match: /\/settings\/style-package\/names\?/,
    json: [
      { name: 'Test runs', scope: SCOPE },
      { name: 'Default', scope: SCOPE },
    ],
  },
  {
    method: 'GET',
    match: /\/settings\/style-package\/names\/[^/]+\/content/,
    json: {
      matchingQuery: 'type:testrun',
      weight: 50,
      coverPage: 'Fancy cover',
      css: 'Default',
      headerFooter: 'Default',
      localization: 'Default',
      webhooks: 'Rewriter',
      headersColor: '#004d73',
      paperSize: 'A4',
      orientation: 'PORTRAIT',
      pdfVariant: 'PDF_A_2B',
      imageDensity: 'DPI_96',
      fitToPage: true,
      followHTMLPresentationalHints: true,
      renderComments: 'OPEN',
      cutEmptyWorkitemAttributes: true,
      specificChapters: '1,2',
      metadataFields: 'docOwner',
      customNumberedListStyles: '1ai',
      language: 'de',
      linkedWorkitemRoles: ['relates_to'],
      linkRoleDirection: 'BOTH',
      workItemsQuery: 'type:requirement',
      attachmentsFilter: '*.pdf',
      testcaseFieldId: 'withAttachments',
      embedAttachments: true,
      ...content,
    },
  },
  { method: 'GET', match: /\/settings\/style-package\/default-content/, json: { weight: 50 } },
  { method: 'GET', match: /\/settings\/style-package\/names\/[^/]+\/revisions/, json: [] },
  // The switch above the packages: read from the global scope, so the pane's button is live
  {
    method: 'GET',
    match: /\/settings\/style-package-visibility\/names\?/,
    json: [{ name: 'Default', scope: '' }],
  },
  {
    method: 'GET',
    match: /\/settings\/style-package-visibility\/names\/Default\/content/,
    json: { hideGlobalStylePackages: false },
  },
];

/** The last section rendered, and the button of the switch above the packages enabled. */
const loaded = () =>
  document.querySelector('#roles-select') !== null &&
  Array.from(document.querySelectorAll<HTMLButtonElement>('.configurations-pane button')).some(
    (b) => (b.textContent ?? '').trim() === 'Change visibility' && !b.disabled,
  );

describe.skipIf(!__PIXEL_REFERENCES__)('Style Packages page visual', () => {
  it('a style package loaded, every section on screen', async () => {
    await snapshotFeature('style-package', routes(), loaded, 'style-packages-loaded');
    expect(true).toBe(true);
  });

  it('a style package which gives the first page a header and footer of its own', async () => {
    await snapshotFeature(
      'style-package',
      routes({ firstPageHeaderFooter: 'With logo' }),
      () => loaded() && document.querySelector('#first-page-header-footer-select') !== null,
      'style-packages-first-page',
    );
    expect(true).toBe(true);
  });
});
