import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import useNamedSettings from '../src/services/settings';
import { installFetchMock, jsonResponse } from './mockFetch';

// The named-settings hook every settings page of this extension is built on. The pages exercise the
// happy paths; what is worth pinning here is the shape of the calls the pages never make directly
// (rename), and how a failure is turned into a message - the legacy ExtensionContext accepted three
// different error bodies and the pages show whatever comes out.

type Content = { css: string };

/**
 * Renders a probe component so the hook runs under React, and hands back its API. Awaited:
 * vitest-browser-react renders asynchronously, so the hook has not run yet when render() returns.
 */
async function useApi(): Promise<ReturnType<typeof useNamedSettings<Content>>> {
  let api: ReturnType<typeof useNamedSettings<Content>> | undefined;
  function Probe() {
    api = useNamedSettings<Content>('css');
    return null;
  }
  render(<Probe />);
  await vi.waitFor(() => expect(api).toBeDefined());
  return api!;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('useNamedSettings', () => {
  it('renames a configuration by posting the new name', async () => {
    const fetchMock = installFetchMock([{ method: 'POST', match: /\/settings\/css\/names\/Old/, json: {} }]);

    await (await useApi()).renameConfiguration('Old', 'project/elibrary/', 'New name');

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe('/polarion/pdf-exporter/rest/internal/settings/css/names/Old?scope=project%2Felibrary%2F');
    expect(init!.body).toBe('New name');
  });

  it('asks for a specific revision when one is given', async () => {
    const fetchMock = installFetchMock([
      { method: 'GET', match: /\/settings\/css\/names\/Default\/content/, json: { css: '' } },
    ]);

    await (await useApi()).loadContent('Default', '', '4242');

    expect(String(fetchMock.mock.calls[0][0])).toContain('&revision=4242');
  });

  it('surfaces the message the backend sent, whichever field it used', async () => {
    const api = await useApi();

    installFetchMock([
      { method: 'GET', match: /default-content/, respond: () => jsonResponse({ message: 'no such scope' }, 400) },
    ]);
    await expect(api.loadDefaultContent()).rejects.toThrow('no such scope');

    installFetchMock([
      { method: 'GET', match: /default-content/, respond: () => jsonResponse({ errorMessage: 'not allowed' }, 403) },
    ]);
    await expect(api.loadDefaultContent()).rejects.toThrow('not allowed');

    installFetchMock([
      { method: 'GET', match: /default-content/, respond: () => new Response('plain text failure', { status: 500 }) },
    ]);
    await expect(api.loadDefaultContent()).rejects.toThrow('plain text failure');

    installFetchMock([{ method: 'GET', match: /default-content/, respond: () => new Response('', { status: 503 }) }]);
    await expect(api.loadDefaultContent()).rejects.toThrow('HTTP 503');
  });
});
