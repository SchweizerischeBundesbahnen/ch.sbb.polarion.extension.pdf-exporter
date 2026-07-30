import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import App from '../src/App';
import { findFeature } from '../src/features';
import { installFetchMock, jsonResponse } from './mockFetch';

// The top-level feature router: `?feature=<id>` selects a page, anything unmatched (incl. bare `/`)
// renders the dev Landing stub. Also covers the About wrapper (feeds the shared RSP About component
// this app's endpoints) and the findFeature lookup.

const origUrl = window.location.pathname + window.location.search;

const PROJECTS = { data: [{ id: 'elibrary', attributes: { name: 'E-Library' } }] };

const aboutRoutes = () => [
  {
    method: 'GET',
    match: /\/version$/,
    json: { bundleName: 'PDF Exporter', bundleVendor: 'SBB', bundleVersion: '1.0.0' },
  },
  { method: 'GET', match: /\/configuration-properties$/, json: { properties: [], obsoleteProperties: [] } },
  { method: 'GET', match: /\/configuration-status/, json: [] },
  { method: 'GET', match: /\/readme$/, respond: () => new Response('<h1>Readme</h1>', { status: 200 }) },
];

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'pdf-exporter-dev-scope=; path=/; max-age=0';
});

describe('findFeature', () => {
  it('matches a known id and returns undefined otherwise', () => {
    // The ids are the contract with hivemodule.xml: an extender pointing at an id that is not here
    // opens a blank page in Polarion.
    expect(findFeature('about')?.id).toBe('about');
    expect(findFeature('disclaimer')?.id).toBe('disclaimer');
    expect(findFeature('user-guide')?.id).toBe('user-guide');
    expect(findFeature('style-package-weights')?.id).toBe('style-package-weights');
    expect(findFeature('authorization')?.id).toBe('authorization');
    expect(findFeature('nope')).toBeUndefined();
    expect(findFeature(null)).toBeUndefined();
  });
});

describe('App router', () => {
  it('renders the Landing stub with every feature linked when no feature is selected', async () => {
    installFetchMock([{ method: 'GET', match: /\/polarion\/rest\/v1\/projects/, json: PROJECTS }]);
    window.history.replaceState({}, '', '?'); // no feature -> Landing
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.landing')).not.toBeNull());
    const links = Array.from(document.querySelectorAll<HTMLAnchorElement>('.feature-list a')).map((a) =>
      a.getAttribute('href'),
    );
    expect(links).toEqual([
      '?feature=about',
      '?feature=disclaimer',
      '?feature=user-guide',
      '?feature=css',
      '?feature=cover-page',
      '?feature=header-footer',
      '?feature=filename',
      '?feature=style-package-weights',
      '?feature=authorization',
    ]);
  });

  it('pre-selects the scope from the ?scope param so feature links carry it', async () => {
    installFetchMock([{ method: 'GET', match: /\/polarion\/rest\/v1\/projects/, json: PROJECTS }]);
    window.history.replaceState({}, '', '?scope=project/elibrary');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.landing')).not.toBeNull());
    await vi.waitFor(() => {
      const hrefs = Array.from(document.querySelectorAll<HTMLAnchorElement>('.feature-list a')).map((a) =>
        a.getAttribute('href'),
      );
      expect(hrefs.some((h) => h?.includes('scope=project%2Felibrary%2F'))).toBe(true);
    });
  });

  it('shows an error when projects cannot be loaded', async () => {
    installFetchMock([
      { method: 'GET', match: /\/polarion\/rest\/v1\/projects/, respond: () => jsonResponse({}, 401) },
    ]);
    window.history.replaceState({}, '', '?');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.landing .alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('Could not load projects');
  });

  it('renders the About page for ?feature=about', async () => {
    installFetchMock(aboutRoutes());
    window.history.replaceState({}, '', '?feature=about&embedded=true');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.about-table')).not.toBeNull());
    expect(document.body.textContent).toContain('PDF Exporter');
    expect(document.querySelector('.about-page .app-icon')).not.toBeNull();
  });
});
