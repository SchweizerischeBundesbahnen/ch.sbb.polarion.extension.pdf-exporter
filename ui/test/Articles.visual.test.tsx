import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { found, snapshotFeature } from './visualHelpers';

// Docker-only snapshots of the two article pages - the Usage Disclaimer and the User Guide. Both
// render build-generated HTML, so what is pinned is the frame around it and the markdown styling.

// The documentation search box renders only when the search index has records, and the real index is a
// build artifact that is empty unless the articles were rendered by Maven first. Pin it to a fixed record so
// the User Guide frame looks the same whichever way the suite was started.
vi.mock('../src/docs/search-index.json', () => ({
  default: [{ doc: 'user-guide', docTitle: 'User Guide', anchor: 'limits', title: 'Limits', text: 'One Two' }],
}));

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
          match: /\/disclaimer$/,
          respond: () => new Response(ARTICLE, { status: 200, headers: { 'Content-Type': 'text/html' } }),
        },
      ],
      found('article.markdown-body'),
      'disclaimer-loaded',
    );
    expect(true).toBe(true);
  });

  it('user guide', async () => {
    // User Guide is now a documentation-site page: DocArticle fetches the static user-guide.html and
    // DocLayout wraps it in the sidebar/on-this-page/prev-next frame. The baseline was regenerated to
    // capture that frame (npm run test:update:docker).
    await snapshotFeature(
      'user-guide',
      [{ method: 'GET', match: /\/html\/user-guide\.html$/, respond: () => new Response(ARTICLE, { status: 200 }) }],
      found('article.markdown-body'),
      'user-guide-loaded',
    );
    expect(true).toBe(true);
  });
});
