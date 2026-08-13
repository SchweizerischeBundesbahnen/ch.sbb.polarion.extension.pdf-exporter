import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import Disclaimer from '../src/pages/Disclaimer';
import { installFetchMock } from './mockFetch';

// The Usage Disclaimer page reads generic's /disclaimer endpoint, like About and User Guide read
// theirs. What the tests pin is that it goes through the REST base and what happens when the
// extension ships no disclaimer - the endpoint answers empty, and the page then links to GitHub the
// way the JSP page it replaces did.

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

const article = (body: string, status = 200) => ({
  method: 'GET',
  match: /\/disclaimer$/,
  respond: () => new Response(body, { status, headers: { 'Content-Type': 'text/html' } }),
});

describe('Disclaimer', () => {
  it('renders the build-generated article', async () => {
    const fetchMock = installFetchMock([article('<h1>Usage Disclaimer</h1><p>Provided as is.</p>')]);
    render(<Disclaimer />);

    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());
    expect(document.body.textContent).toContain('Provided as is.');
    // Served by the extension's own REST API, not by a static path carrying the app context.
    expect(String(fetchMock.mock.calls[0][0])).toBe('/polarion/pdf-exporter/rest/internal/disclaimer');
  });

  it('points at the online source when the extension ships no disclaimer', async () => {
    // The endpoint answers 200 with an empty body - that is how "not generated" is signalled.
    installFetchMock([article('')]);
    render(<Disclaimer />);

    await vi.waitFor(() => expect(document.body.textContent).toContain('No disclaimer has been generated'));
    const link = document.querySelector<HTMLAnchorElement>('a[target="_blank"]');
    expect(link?.href).toContain('/DISCLAIMER.md');
  });

  it('treats a blank article as missing rather than rendering an empty page', async () => {
    installFetchMock([article('   ')]);
    render(<Disclaimer />);

    await vi.waitFor(() => expect(document.body.textContent).toContain('No disclaimer has been generated'));
  });

  it('treats a non-OK response as missing', async () => {
    installFetchMock([article('<p>ignored</p>', 404)]);
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
