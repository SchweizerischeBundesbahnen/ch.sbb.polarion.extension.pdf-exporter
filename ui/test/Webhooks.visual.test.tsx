import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { found, snapshotFeature } from './visualHelpers';

// Docker-only snapshot of the Webhooks page: the configuration selector, a row with its auth block and
// one without, the add button and the toolbar.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-webhooks=; path=/; max-age=0';
});

describe.skipIf(!__PIXEL_REFERENCES__)('Webhooks page visual', () => {
  it('a configuration loaded, webhooks on screen', async () => {
    await snapshotFeature(
      'webhooks',
      [
        { method: 'GET', match: /\/webhooks\/status/, json: { enabled: true } },
        {
          method: 'GET',
          match: /\/settings\/webhooks\/names\?/,
          json: [{ name: 'Default', scope: 'project/elibrary/' }],
        },
        {
          method: 'GET',
          match: /\/settings\/webhooks\/names\/[^/]+\/content/,
          json: {
            webhookConfigs: [
              { url: 'https://my.domain.com/rewrite', authType: 'BEARER_TOKEN', authTokenName: 'pdf-webhook' },
              { url: 'https://my.domain.com/watermark', authType: null, authTokenName: null },
            ],
          },
        },
        { method: 'GET', match: /\/settings\/webhooks\/default-content/, json: { webhookConfigs: [] } },
        { method: 'GET', match: /\/settings\/webhooks\/names\/[^/]+\/revisions/, json: [] },
      ],
      found('.webhooks-table tbody tr'),
      'webhooks-loaded',
    );
    expect(true).toBe(true);
  });
});
