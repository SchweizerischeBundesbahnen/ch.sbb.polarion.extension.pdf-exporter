import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import useRemote from '../src/services/useRemote';

// useRemote is exercised end-to-end through the panel/pages; this covers its error branch directly (a
// fetch rejection is turned into a 503 "network error" Response rather than throwing) and that it
// targets the session-based /internal base when no bearer token is set.

let api: ReturnType<typeof useRemote>;
function Capture() {
  api = useRemote();
  return null;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe('useRemote', () => {
  it('returns a 503 network-error response when fetch rejects', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new Error('down');
      }),
    );
    render(<Capture />);
    await vi.waitFor(() => expect(api).toBeTruthy());
    const res = await api.sendRequest({ method: 'GET', url: '/version' });
    expect(res.status).toBe(503);
    expect((await res.json()).errorMessage).toContain('Network error');
  });

  it('targets the extension REST base and appends the endpoint path', async () => {
    // The base is `/internal` (session) or `/api` (when VITE_BEARER_TOKEN is set); assert the parts
    // that hold regardless of whether a dev token is configured.
    const fetchMock = vi.fn(async () => new Response('{}', { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);
    render(<Capture />);
    await vi.waitFor(() => expect(api).toBeTruthy());
    await api.sendRequest({ method: 'GET', url: '/version' });
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toMatch(/\/polarion\/pdf-exporter\/rest\/(internal|api)\/version$/);
  });

  it('uses the token-authenticated /api base and sends the Authorization header when a token is set', async () => {
    vi.stubEnv('VITE_BEARER_TOKEN', 'tok123');
    const fetchMock = vi.fn(async () => new Response('{}', { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);
    // Reset the captured hook so waitFor blocks until THIS test's <Capture> re-renders (the module-level
    // `api` still holds the previous test's token-less instance otherwise).
    api = undefined as unknown as ReturnType<typeof useRemote>;
    render(<Capture />);
    await vi.waitFor(() => expect(api).toBeTruthy());
    await api.sendRequest({ method: 'GET', url: '/version', contentType: 'application/json' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/polarion/pdf-exporter/rest/api/version');
    expect((init!.headers as Record<string, string>)['Authorization']).toBe('Bearer tok123');
  });
});
