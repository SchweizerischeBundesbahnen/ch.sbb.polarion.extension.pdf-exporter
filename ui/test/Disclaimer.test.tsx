import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import Disclaimer from '../src/pages/Disclaimer';
import { DISCLAIMER_URL } from '../src/services/articles';
import { installFetchMock } from './mockFetch';

// The Usage Disclaimer page. Unlike About and User Guide the article has no REST endpoint: it is read
// as a static file from this extension's app webapp, so the tests pin that URL and the behaviour when
// the file was never generated - the case the JSP page it replaces handled with a link to GitHub.

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

const html = (body: string) => ({
  method: 'GET',
  match: /disclaimer\.html$/,
  respond: () => new Response(body, { status: 200, headers: { 'Content-Type': 'text/html' } }),
});

describe('Disclaimer', () => {
  it('renders the build-generated article', async () => {
    const fetchMock = installFetchMock([html('<h1>Usage Disclaimer</h1><p>Provided as is.</p>')]);
    render(<Disclaimer />);

    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());
    expect(document.body.textContent).toContain('Provided as is.');
    // The article comes from the app webapp, not from the REST base.
    expect(String(fetchMock.mock.calls[0][0])).toBe(DISCLAIMER_URL);
  });

  it('points at the online source when the article was not generated', async () => {
    installFetchMock([{ method: 'GET', match: /disclaimer\.html$/, respond: () => new Response('', { status: 404 }) }]);
    render(<Disclaimer />);

    await vi.waitFor(() => expect(document.body.textContent).toContain('No disclaimer has been generated'));
    const link = document.querySelector<HTMLAnchorElement>('a[target="_blank"]');
    expect(link?.href).toContain('/DISCLAIMER.md');
  });

  it('treats an empty file as missing rather than rendering a blank page', async () => {
    // Polarion answers a path it does not recognise with its own page, and a servlet that found
    // nothing answers empty - either way there is no article to show.
    installFetchMock([html('   ')]);
    render(<Disclaimer />);

    await vi.waitFor(() => expect(document.body.textContent).toContain('No disclaimer has been generated'));
  });

  it('survives a failing request', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new Error('network down'))),
    );
    render(<Disclaimer />);

    await vi.waitFor(() => expect(document.body.textContent).toContain('No disclaimer has been generated'));
  });
});
