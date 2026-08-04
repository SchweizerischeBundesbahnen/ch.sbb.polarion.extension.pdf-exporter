import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { filled, snapshotFeature } from './visualHelpers';

// Docker-only snapshot of the header and footer page: six editors in two rows of three, which is the
// layout the legacy page had and the one thing a CSS change here would silently break.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-header-footer=; path=/; max-age=0';
});

describe.skipIf(!__PIXEL_REFERENCES__)('Header and footer page visual', () => {
  it('six cells of a custom header and footer', async () => {
    await snapshotFeature(
      'header-footer',
      [
        {
          method: 'GET',
          match: /\/settings\/header-footer\/names\?/,
          json: [{ name: 'Default', scope: 'project/elibrary/' }],
        },
        {
          method: 'GET',
          match: /\/settings\/header-footer\/names\/[^/]+\/content/,
          json: {
            useCustomValues: true,
            headerLeft: '{{ PROJECT_NAME }}',
            headerCenter: '{{ DOCUMENT_TITLE }}',
            headerRight: '{{ REVISION }}',
            footerLeft: '{{ TIMESTAMP }}',
            footerCenter: '{{ PAGE_NUMBER }} / {{ PAGES_TOTAL_COUNT }}',
            footerRight: '{{ PRODUCT_NAME }}',
          },
        },
        {
          method: 'GET',
          match: /\/settings\/header-footer\/default-content/,
          json: {
            useCustomValues: false,
            headerLeft: '',
            headerCenter: '',
            headerRight: '',
            footerLeft: '',
            footerCenter: '',
            footerRight: '',
          },
        },
        { method: 'GET', match: /\/settings\/header-footer\/names\/[^/]+\/revisions/, json: [] },
      ],
      filled('#custom-headerLeft'),
      'header-footer-loaded',
    );
    expect(true).toBe(true);
  });
});
