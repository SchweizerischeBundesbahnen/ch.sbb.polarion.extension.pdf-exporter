import type { SendRequest } from '@grigoriev/react-sbb-polarion';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { loadDocumentLanguage, loadPanelData, loadStylePackage } from '../src/formext/panelData';
import { installFetchMock, jsonResponse } from './mockFetch';
import type { Route } from './mockFetch';
import { SAMPLE_DOCUMENT } from './sidePanelSamples';

// What the server-rendered panel used to have substituted into its markup, now read over REST. These are
// the endpoints the DLE toolbar popup has always used, so what is asserted here is that the panel asks
// them the same questions - and what it does when one of them will not answer.

// The loader takes its transport as a parameter, so this stands in for the panel's `useRemote()` - which
// is a hook and cannot be called outside a component. What that hook itself does is covered by
// useRemote.test.tsx.
const sendRequest: SendRequest = ({ method, url, body, contentType }) =>
  fetch(`/polarion/pdf-exporter/rest/internal${url}`, {
    method,
    headers: contentType ? { 'Content-Type': contentType } : {},
    body,
  });

const names = (...values: string[]) => values.map((name) => ({ name, scope: 'project/elibrary/' }));

const baseRoutes = (): Route[] => [
  { method: 'POST', match: /\/settings\/style-package\/suitable-names/, json: names('Specification', 'Default') },
  { method: 'GET', match: /\/settings\/cover-page\/names/, json: names('Default') },
  { method: 'GET', match: /\/settings\/css\/names/, json: names('Default', 'SBB') },
  { method: 'GET', match: /\/settings\/header-footer\/names/, json: names('Default') },
  { method: 'GET', match: /\/settings\/localization\/names/, json: names('Default') },
  { method: 'GET', match: /\/settings\/webhooks\/names/, json: names('Default') },
  { method: 'GET', match: /\/link-role-names/, json: ['relates_to', 'verifies'] },
  { method: 'POST', match: /\/export-filename/, respond: () => new Response('E-Library Doc.pdf') },
  { method: 'GET', match: /\/document-language/, respond: () => new Response('de') },
  { method: 'GET', match: /\/webhooks\/status/, json: { enabled: true } },
  { method: 'GET', match: /\/permissions\/export/, json: { permitted: true } },
];

const routesWith = (...overrides: Route[]): Route[] => [...overrides, ...baseRoutes()];

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('loading the panel data', () => {
  it('reads the style packages, the option lists and the rest of what the panel offers', async () => {
    installFetchMock(baseRoutes());

    const data = await loadPanelData(sendRequest, SAMPLE_DOCUMENT);

    // Weight order is the server's; the panel preselects the head of it, as the server-rendered panel did
    expect(data.stylePackages.map((option) => option.id)).toEqual(['Specification', 'Default']);
    expect(data.childNames.css.map((option) => option.id)).toEqual(['Default', 'SBB']);
    expect(data.roles.map((option) => option.id)).toEqual(['relates_to', 'verifies']);
    expect(data.fileName).toBe('E-Library Doc.pdf');
    expect(data.documentLanguage).toBe('de');
    expect(data.webhooksEnabled).toBe(true);
    expect(data.exportPermission).toBe('granted');
  });

  it('asks for the style packages of this document, the way the endpoint wants it', async () => {
    const fetchMock = installFetchMock(baseRoutes());

    await loadPanelData(sendRequest, SAMPLE_DOCUMENT);

    const call = fetchMock.mock.calls.find(([url]) => String(url).includes('suitable-names'))!;
    expect(JSON.parse(String(call[1]!.body))).toEqual([
      { projectId: 'elibrary', spaceId: 'Default Space', documentName: 'Cross Link Issue' },
    ]);
  });

  it('fails when the style packages cannot be read: there would be nothing to choose from', async () => {
    installFetchMock(routesWith({ method: 'POST', match: /suitable-names/, status: 500 }));

    await expect(loadPanelData(sendRequest, SAMPLE_DOCUMENT)).rejects.toThrow();
  });

  it('fails when an option list cannot be read, for the same reason', async () => {
    installFetchMock(routesWith({ method: 'GET', match: /\/settings\/css\/names/, status: 500 }));

    await expect(loadPanelData(sendRequest, SAMPLE_DOCUMENT)).rejects.toThrow();
  });

  it('falls back rather than fails on the reads that only decide a detail', async () => {
    installFetchMock(
      routesWith(
        { method: 'GET', match: /\/link-role-names/, status: 500 },
        { method: 'POST', match: /\/export-filename/, status: 500 },
        { method: 'GET', match: /\/document-language/, status: 500 },
        { method: 'GET', match: /\/webhooks\/status/, status: 500 },
      ),
    );

    const data = await loadPanelData(sendRequest, SAMPLE_DOCUMENT);

    // No roles means the roles group is not offered; the rest is a panel the user can still work with
    expect(data.roles).toEqual([]);
    expect(data.fileName).toBe('');
    expect(data.documentLanguage).toBeNull();
    expect(data.webhooksEnabled).toBe(false);
  });

  it('grants the export only on an explicit permission', async () => {
    installFetchMock(baseRoutes());

    await expect(loadPanelData(sendRequest, SAMPLE_DOCUMENT)).resolves.toMatchObject({
      exportPermission: 'granted',
    });
  });

  it('reports a refused permission, so the buttons can say why they are disabled', async () => {
    installFetchMock(routesWith({ method: 'GET', match: /\/permissions\/export/, json: { permitted: false } }));

    const data = await loadPanelData(sendRequest, SAMPLE_DOCUMENT);

    expect(data.exportPermission).toBe('denied');
  });

  it('does not grant the export on a permission that could not be read', async () => {
    // Fail closed, as generic's DLE toolbar engine does for this same endpoint. `unknown` rather than
    // `denied`, so the panel does not tell the user they are not allowed when it does not know that.
    installFetchMock(routesWith({ method: 'GET', match: /\/permissions\/export/, status: 503 }));

    const data = await loadPanelData(sendRequest, SAMPLE_DOCUMENT);

    expect(data.exportPermission).toBe('unknown');
  });

  it('does not grant the export on a body that does not say `true`', async () => {
    // A malformed or truthy-but-not-true answer is not a grant either; generic reads it the same way.
    for (const body of [{}, { permitted: null }, { permitted: 'yes' }, { permitted: 1 }]) {
      installFetchMock(routesWith({ method: 'GET', match: /\/permissions\/export/, json: body }));

      const data = await loadPanelData(sendRequest, SAMPLE_DOCUMENT);

      expect(data.exportPermission).toBe('denied');
    }
  });

  it('reads a document language only for the document it is asked about', async () => {
    const fetchMock = installFetchMock(baseRoutes());

    await loadDocumentLanguage(sendRequest, { ...SAMPLE_DOCUMENT, revision: '4711' });

    const url = String(fetchMock.mock.calls.find(([called]) => String(called).includes('document-language'))![0]);
    expect(url).toContain('projectId=elibrary');
    expect(url).toContain('documentName=Cross+Link+Issue');
    expect(url).toContain('revision=4711');
  });

  it('reports a document with no language as having none', async () => {
    installFetchMock(routesWith({ method: 'GET', match: /\/document-language/, respond: () => new Response('') }));

    await expect(loadDocumentLanguage(sendRequest, SAMPLE_DOCUMENT)).resolves.toBeNull();
  });

  it('reads one style package by name, in the document scope', async () => {
    const fetchMock = installFetchMock([
      {
        method: 'GET',
        match: /\/settings\/style-package\/names\/[^/]+\/content/,
        respond: () => jsonResponse({ exposeSettings: true }),
      },
    ]);

    const content = await loadStylePackage(sendRequest, 'My Package', 'project/elibrary/');

    expect(content.exposeSettings).toBe(true);
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/names/My%20Package/content');
    expect(url).toContain('scope=project%2Felibrary%2F');
  });

  it('reports a style package that cannot be read', async () => {
    installFetchMock([{ method: 'GET', match: /style-package\/names/, status: 404 }]);

    await expect(loadStylePackage(sendRequest, 'Gone', 'project/elibrary/')).rejects.toThrow('HTTP 404');
  });
});
