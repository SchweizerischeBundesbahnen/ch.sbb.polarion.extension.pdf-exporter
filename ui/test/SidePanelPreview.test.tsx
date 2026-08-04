import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import SidePanelPreview from '../src/pages/SidePanelPreview';
import { installFetchMock, jsonResponse } from './mockFetch';
import type { Route } from './mockFetch';

// The side panel's development harness. It is dev-only scaffolding, but the part that decides what the
// panel is pointed at is not: the project it takes from the scope, the document list it offers, the
// document it remembers, and the editor hash it writes for the one that was picked. That hash is what the
// product's export context parses, so a wrong one means the harness silently exercises the wrong document.

const DOCUMENTS = [
  { attributes: { moduleFolder: 'Default Space', moduleName: 'Cross Link Issue' } },
  { attributes: { moduleFolder: 'Specs', moduleName: 'Requirements' } },
];

const documentRoutes = (): Route[] => [
  { method: 'GET', match: /\/projects\/[^/]+\/documents/, json: { data: DOCUMENTS } },
];

/** The panel itself needs Polarion; these keep its own loads from being unmocked 404 noise. */
const panelRoutes = (): Route[] => [
  { method: 'POST', match: /suitable-names/, json: [{ name: 'Default', scope: 'project/elibrary/' }] },
  { method: 'GET', match: /\/settings\/[^/]+\/names/, json: [{ name: 'Default', scope: 'project/elibrary/' }] },
  { method: 'GET', match: /style-package\/names\/[^/]+\/content/, json: { exposeSettings: false } },
  { method: 'GET', match: /\/link-role-names/, json: [] },
  { method: 'POST', match: /\/export-filename/, respond: () => new Response('Doc.pdf') },
  { method: 'GET', match: /\/document-language/, respond: () => new Response('') },
  { method: 'GET', match: /\/webhooks\/status/, respond: () => jsonResponse({ enabled: false }) },
  { method: 'GET', match: /\/permissions\/export/, json: { permitted: true } },
];

const open = (scope = 'project/elibrary/', routes: Route[] = [...documentRoutes(), ...panelRoutes()]) => {
  const fetchMock = installFetchMock(routes);
  window.history.replaceState({}, '', `?feature=side-panel&scope=${encodeURIComponent(scope)}`);
  render(<SidePanelPreview />);
  return fetchMock;
};

const text = () => document.querySelector('.page, .app')?.textContent ?? document.body.textContent ?? '';
const select = () => document.querySelector<HTMLSelectElement>('#dev-document-select');
const pick = (value: string) => {
  const element = select()!;
  element.value = value;
  element.dispatchEvent(new Event('change', { bubbles: true }));
};

const documentsLoaded = () => vi.waitFor(() => expect(select()?.querySelectorAll('option').length).toBeGreaterThan(1));

beforeEach(() => {
  // A leftover selection from another test would preselect a document and mount the panel unasked.
  document.cookie = 'pdf-exporter-dev-document=; path=/; max-age=0';
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', window.location.pathname);
});

describe('the side panel development harness', () => {
  it('asks for a project before anything else, documents being listed per project', async () => {
    open('');

    await vi.waitFor(() => expect(text()).toContain('Pick a project on the'));
    expect(select()).toBeNull();
  });

  it('offers the documents of the selected project', async () => {
    const fetchMock = open();
    await documentsLoaded();

    const options = Array.from(select()!.querySelectorAll('option')).map((option) => option.textContent);
    expect(options).toContain('Default Space / Cross Link Issue');
    expect(options).toContain('Specs / Requirements');
    expect(String(fetchMock.mock.calls[0][0])).toContain('/projects/elibrary/documents');
  });

  it('shows no panel until a document is picked', async () => {
    open();
    await documentsLoaded();

    expect(document.querySelector('#side-panel-preview-host')).toBeNull();
  });

  it('writes the editor hash of the picked document and mounts the real panel into it', async () => {
    open();
    await documentsLoaded();

    pick('Default Space/Cross Link Issue');

    const host = await vi.waitFor(() => {
      const found = document.querySelector('#side-panel-preview-host');
      expect(found).not.toBeNull();
      return found!;
    });
    // The hash a real editor would have, which is what the product's export context reads
    expect(window.location.hash).toBe('#/project/elibrary/wiki/Default%20Space/Cross%20Link%20Issue');
    // Mounted for real: its own shadow root, not markup in the page
    await vi.waitFor(() => expect(host.shadowRoot).not.toBeNull());
    // `?feature=` survives, the app routing on the search parameters rather than the hash
    expect(window.location.search).toContain('feature=side-panel');
  });

  it('re-points the hash when another document is picked', async () => {
    open();
    await documentsLoaded();
    pick('Default Space/Cross Link Issue');
    await vi.waitFor(() => expect(window.location.hash).toContain('Cross%20Link%20Issue'));

    pick('Specs/Requirements');

    await vi.waitFor(() => expect(window.location.hash).toBe('#/project/elibrary/wiki/Specs/Requirements'));
  });

  it('remembers the document that was picked, against the project it was picked in', async () => {
    open();
    await documentsLoaded();

    pick('Specs/Requirements');

    await vi.waitFor(() => expect(document.cookie).toContain('elibrary%7CSpecs%2FRequirements'));
  });

  it('preselects the document remembered for this project', async () => {
    document.cookie = 'pdf-exporter-dev-document=elibrary%7CSpecs%2FRequirements; path=/';

    open();
    await documentsLoaded();

    expect(select()!.value).toBe('Specs/Requirements');
    // Preselected means mounted: the harness is where it was left off
    await vi.waitFor(() => expect(document.querySelector('#side-panel-preview-host')).not.toBeNull());
  });

  it('ignores a document remembered for another project', async () => {
    document.cookie = 'pdf-exporter-dev-document=other%7CSpecs%2FRequirements; path=/';

    open();
    await documentsLoaded();

    expect(select()!.value).toBe('');
    expect(document.querySelector('#side-panel-preview-host')).toBeNull();
  });

  it('ignores a remembered document the project no longer offers', async () => {
    document.cookie = 'pdf-exporter-dev-document=elibrary%7CGone%2FDeleted; path=/';

    open();
    await documentsLoaded();

    expect(select()!.value).toBe('');
  });

  it('says what to configure when the document list cannot be read', async () => {
    open('project/elibrary/', [{ method: 'GET', match: /\/documents/, status: 401 }, ...panelRoutes()]);

    await vi.waitFor(() => expect(text()).toContain('Could not load the documents'));
    expect(text()).toContain('VITE_BEARER_TOKEN');
  });

  it('says so when the project has no documents at all', async () => {
    open('project/elibrary/', [{ method: 'GET', match: /\/documents/, json: { data: [] } }, ...panelRoutes()]);

    await vi.waitFor(() => expect(text()).toContain('no documents to export'));
  });

  it('warns when the document list was cut off at the page cap', async () => {
    open('project/elibrary/', [
      {
        method: 'GET',
        match: /\/documents|\/next/,
        respond: () =>
          jsonResponse({ data: DOCUMENTS, links: { next: 'http://polarion.example/polarion/rest/v1/next' } }),
      },
      ...panelRoutes(),
    ]);

    await vi.waitFor(() => expect(text()).toContain('the list was cut off there'));
  });
});
