import { useCallback, useMemo } from 'react';

const REST_PATH = '/polarion/pdf-exporter/rest';

interface RequestParams {
  method: string;
  url: string;
  body?: BodyInit;
  contentType?: string;
}

/**
 * Thin wrapper over fetch for the extension's REST API. In `vite dev` requests are proxied to a real
 * Polarion (see vite.config.js); a personal access token in VITE_BEARER_TOKEN switches to the
 * token-authenticated `/api` endpoints, otherwise the session-based `/internal` endpoints are used.
 */
export default function useRemote() {
  const token = import.meta.env.VITE_BEARER_TOKEN;
  const restBase = useMemo(() => `${REST_PATH}${token ? '/api' : '/internal'}`, [token]);

  const authHeaders = useCallback(
    (extra?: Record<string, string>): Record<string, string> => {
      const headers: Record<string, string> = { ...extra };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      return headers;
    },
    [token],
  );

  const networkErrorResponse = () =>
    new Response(
      JSON.stringify({ errorMessage: 'Network error occurred. Be sure Polarion is started and accessible.' }),
      {
        status: 503,
        headers: { 'Content-Type': 'application/json' },
      },
    );

  const send = useCallback(
    (url: string, { method, body, contentType }: Omit<RequestParams, 'url'>): Promise<Response> => {
      const headers = authHeaders(contentType ? { 'Content-Type': contentType } : undefined);
      return fetch(url, { method, mode: 'cors', cache: 'no-cache', headers, body }).catch(networkErrorResponse);
    },
    [authHeaders],
  );

  /** Call an endpoint relative to the REST base (e.g. `/version`, `/configuration-properties`). */
  const sendRequest = useCallback(
    ({ url, ...rest }: RequestParams): Promise<Response> => send(`${restBase}${url}`, rest),
    [restBase, send],
  );

  /**
   * Call a URL the server handed out, with the same authentication as {@link sendRequest}.
   *
   * The conversion endpoints answer a job submission with an absolute `Location` header and expect it to be
   * polled as given, so that URL cannot go through the REST base - but it still needs the bearer token when
   * one is configured, which a bare `fetch` would leave out.
   */
  const sendAbsoluteRequest = useCallback(
    ({ url, ...rest }: RequestParams): Promise<Response> => send(url, rest),
    [send],
  );

  return { sendRequest, sendAbsoluteRequest };
}
