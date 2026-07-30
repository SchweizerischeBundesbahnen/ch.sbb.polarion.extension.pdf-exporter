import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { found, snapshotFeature } from './visualHelpers';

// Docker-only snapshots of the two article pages - the Usage Disclaimer and the User Guide. Both
// render build-generated HTML, so what is pinned is the frame around it and the markdown styling.

const origUrl = window.location.pathname + window.location.search;

const ARTICLE =
  '<h1>Usage</h1><p>This extension is provided as is.</p><h2>Limits</h2><ul><li>One</li><li>Two</li></ul>';

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe.skipIf(!__PIXEL_REFERENCES__)('Article pages visual', () => {
  it('usage disclaimer', async () => {
    await snapshotFeature(
      'disclaimer',
      [
        {
          method: 'GET',
          match: /disclaimer\.html$/,
          respond: () => new Response(ARTICLE, { status: 200, headers: { 'Content-Type': 'text/html' } }),
        },
      ],
      found('article.markdown-body'),
      'disclaimer-loaded',
    );
    expect(true).toBe(true);
  });

  it('user guide', async () => {
    await snapshotFeature(
      'user-guide',
      [{ method: 'GET', match: /\/user-guide$/, respond: () => new Response(ARTICLE, { status: 200 }) }],
      found('article.markdown-body'),
      'user-guide-loaded',
    );
    expect(true).toBe(true);
  });
});
