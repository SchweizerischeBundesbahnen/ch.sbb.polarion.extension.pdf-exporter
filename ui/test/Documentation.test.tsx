import { pageViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { DOC_ORDER } from '../src/docs/manifest';
import { installFetchMock } from './mockFetch';

// The documentation site: every article in the shared frame (breadcrumb, search, sidebar, "on this page",
// prev/next). The search index is generated from the Maven-rendered articles and is empty in a plain
// `npm test`, which hides the search box, so a fixture index stands in for it here.
vi.mock('../src/docs/search-index.json', () => ({
  default: [
    { doc: 'user-guide', docTitle: 'User Guide', anchor: 'export', title: 'Export', text: 'Export a document.' },
    { doc: 'configuration', docTitle: 'Configuration', anchor: 'webhooks', title: 'Webhooks', text: 'Webhooks.' },
  ],
}));

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

const ARTICLE = '<h1>Article</h1><h2 id="section">Section</h2><p>Steps.</p>';

/** Opens one article and waits for it and the frame around it, the search box included. */
async function openArticle(id: string, respond = () => new Response(ARTICLE)) {
  installFetchMock([{ method: 'GET', match: new RegExp(`/html/${id}\\.html$`), respond }]);
  window.history.replaceState({}, '', `?feature=${id}&embedded=true`);
  render(<App />);
  await vi.waitFor(() => expect(document.querySelector('.docs-nav-link-active')).not.toBeNull());
  // Asserted, so the scans below fail if the fixture index stops reaching the app.
  await vi.waitFor(() => expect(document.querySelector('.docs-search-input')).not.toBeNull());
}

const search = () => document.querySelector<HTMLInputElement>('.docs-search-input')!;

describe('Documentation site, accessibility', () => {
  it.each(DOC_ORDER.map((doc) => doc.id))('has no WCAG A/AA violations on the %s article', async (id) => {
    await openArticle(id);
    await vi.waitFor(() => expect(document.querySelector('article.markdown-body h2')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations on an article that was not generated', async () => {
    await openArticle('user-guide', () => new Response('', { status: 404 }));
    await vi.waitFor(() =>
      expect(document.body.textContent).toContain('This article has not been generated during build.'),
    );
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with search results listed', async () => {
    await openArticle('quick-start');
    await userEvent.fill(search(), 'webhooks');
    await vi.waitFor(() => expect(document.querySelector('.docs-search-result')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with a search that matches nothing', async () => {
    await openArticle('quick-start');
    await userEvent.fill(search(), 'nothing-matches-this');
    await vi.waitFor(() => expect(document.querySelector('.docs-search-empty')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });
});
