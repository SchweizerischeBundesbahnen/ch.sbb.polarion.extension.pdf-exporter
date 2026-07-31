import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { found, snapshotFeature } from './visualHelpers';

// Docker-only snapshot of the Localization page: the configuration selector, the translation table with
// a flagged empty cell, the per-language export/import row and the toolbar.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-localization=; path=/; max-age=0';
});

describe.skipIf(!__PIXEL_REFERENCES__)('Localization page visual', () => {
  it('a configuration loaded, translations on screen', async () => {
    await snapshotFeature(
      'localization',
      [
        {
          method: 'GET',
          match: /\/settings\/localization\/names\?/,
          json: [{ name: 'Default', scope: 'project/elibrary/' }],
        },
        {
          method: 'GET',
          match: /\/settings\/localization\/names\/[^/]+\/content/,
          json: {
            translations: {
              Approved: [
                { language: 'de', value: 'Genehmigt' },
                { language: 'fr', value: 'Approuvé' },
                { language: 'it', value: 'Approvato' },
              ],
              Draft: [
                { language: 'de', value: 'Entwurf' },
                { language: 'fr', value: 'Brouillon' },
                { language: 'it', value: '' },
              ],
            },
          },
        },
        { method: 'GET', match: /\/settings\/localization\/default-content/, json: { translations: {} } },
        { method: 'GET', match: /\/settings\/localization\/names\/[^/]+\/revisions/, json: [] },
      ],
      found('.translations-table tbody tr'),
      'localization-loaded',
    );
    expect(true).toBe(true);
  });
});
