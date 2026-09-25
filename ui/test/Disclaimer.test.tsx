import { pageViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import Disclaimer from '../src/pages/Disclaimer';
import { installFetchMock } from './mockFetch';

// The Usage Disclaimer page reads generic's /disclaimer endpoint, like About reads its own. What the tests pin is that it goes through the REST base and what happens when the
// extension ships no disclaimer - the endpoint answers empty, and the page then links to GitHub the
// way the JSP page it replaces did. A failed request is an error, not a missing disclaimer.

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
    // A GitHub file link needs the blob/<ref> segment; the repository root plus the file name answers 404.
    expect(link?.href).toBe(
      'https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/blob/main/DISCLAIMER.md',
    );
  });

  it('treats a blank article as missing rather than rendering an empty page', async () => {
    installFetchMock([article('   ')]);
    render(<Disclaimer />);

    await vi.waitFor(() => expect(document.body.textContent).toContain('No disclaimer has been generated'));
  });

  it('reports a non-OK response as an error, not as a missing disclaimer', async () => {
    installFetchMock([article('<p>ignored</p>', 500)]);
    render(<Disclaimer />);

    await vi.waitFor(() => expect(document.querySelector('.alert.alert-error')?.textContent).toContain('HTTP 500'));
    expect(document.body.textContent).not.toContain('No disclaimer has been generated');
  });

  it('reports a failing request as an error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new Error('network down'))),
    );
    render(<Disclaimer />);

    await vi.waitFor(() =>
      expect(document.querySelector('.alert.alert-error')?.textContent).toContain('Failed to load the disclaimer'),
    );
    expect(document.body.textContent).not.toContain('No disclaimer has been generated');
  });
});

describe('Disclaimer, accessibility', () => {
  it('has no WCAG A/AA violations with the article', async () => {
    installFetchMock([article('<h1>Usage Disclaimer</h1><p>Provided as is.</p>')]);
    render(<Disclaimer />);
    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with the link to the online source', async () => {
    installFetchMock([article('')]);
    render(<Disclaimer />);
    await vi.waitFor(() => expect(document.body.textContent).toContain('No disclaimer has been generated'));
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with the error of a failed request', async () => {
    installFetchMock([article('<p>ignored</p>', 500)]);
    render(<Disclaimer />);
    await vi.waitFor(() => expect(document.querySelector('.alert.alert-error')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });
});
