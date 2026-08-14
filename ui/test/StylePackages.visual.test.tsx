import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { found, snapshotFeature } from './visualHelpers';

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

describe.skipIf(!__PIXEL_REFERENCES__)('Style Packages page visual', () => {
  it('a style package loaded, every section on screen', async () => {
    await snapshotFeature(
      'style-package',
      [
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
          },
        },
        { method: 'GET', match: /\/settings\/style-package\/default-content/, json: { weight: 50 } },
        { method: 'GET', match: /\/settings\/style-package\/names\/[^/]+\/revisions/, json: [] },
      ],
      // The last section rendered; snapshotFeature waits for the fourteen dropdowns on top of this.
      found('#roles-select'),
      'style-packages-loaded',
    );
    expect(true).toBe(true);
  });
});
