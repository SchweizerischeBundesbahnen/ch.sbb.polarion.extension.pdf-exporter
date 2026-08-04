import { vi } from 'vitest';

// Mocks the extension's REST layer at the global `fetch` boundary (useRemote -> fetch). Each route
// matches on HTTP method + a URL regex; the first match wins. Unmatched requests resolve to a 404 with
// an errorMessage so a missing mock is obvious in a failing assertion rather than hanging.

export interface Route {
  method?: string;
  match: RegExp;
  /** Static JSON body (200 unless `status` given). */
  json?: unknown;
  status?: number;
  /** Full control: build the Response from the request. */
  respond?: (url: string, init?: RequestInit) => Response;
}

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

export type FetchMock = ReturnType<typeof vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>>;

/** Install a fetch mock for the given routes; returns the spy so tests can assert calls. */
export function installFetchMock(routes: Route[]): FetchMock {
  const fn = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>((input, init) => {
    const url = typeof input === 'string' ? input : input.toString();
    const method = (init?.method ?? 'GET').toUpperCase();
    for (const route of routes) {
      if ((route.method ?? 'GET').toUpperCase() === method && route.match.test(url)) {
        return Promise.resolve(
          route.respond ? route.respond(url, init) : jsonResponse(route.json ?? {}, route.status ?? 200),
        );
      }
    }
    return Promise.resolve(jsonResponse({ errorMessage: `unmocked ${method} ${url}` }, 404));
  });
  vi.stubGlobal('fetch', fn);
  return fn;
}
