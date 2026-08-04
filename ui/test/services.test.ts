import { afterEach, describe, expect, it, vi } from 'vitest';
import { DISCLAIMER_URL, fetchArticle } from '../src/services/articles';
import { getCookie, setCookie } from '../src/services/cookies';
import { isEmbedded } from '../src/services/params';
import { fetchProjects } from '../src/services/projects';
import { getProjectIdFromScope, getScope } from '../src/services/scope';
import { installFetchMock, jsonResponse } from './mockFetch';

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'pe-test=; path=/; max-age=0';
});

describe('cookies', () => {
  it('round-trips a value and returns null for a missing one', () => {
    expect(getCookie('pe-test')).toBeNull();
    setCookie('pe-test', 'hello world');
    expect(getCookie('pe-test')).toBe('hello world');
  });
});

describe('scope', () => {
  it('normalizes a trailing slash and extracts the project id', () => {
    window.history.replaceState({}, '', '?scope=project/elibrary');
    expect(getScope()).toBe('project/elibrary/');
    expect(getProjectIdFromScope(getScope())).toBe('elibrary');
  });

  it('returns empty for global scope', () => {
    window.history.replaceState({}, '', '?');
    expect(getScope()).toBe('');
    expect(getProjectIdFromScope('')).toBe('');
  });
});

describe('params', () => {
  it('is embedded only when ?embedded=true', () => {
    window.history.replaceState({}, '', '?embedded=true');
    expect(isEmbedded()).toBe(true);
    window.history.replaceState({}, '', '?');
    expect(isEmbedded()).toBe(false);
  });
});

describe('fetchProjects', () => {
  it('maps and sorts the JSON:API project list', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/polarion\/rest\/v1\/projects/,
        json: {
          data: [
            { id: 'zeta', attributes: { name: 'Zeta' } },
            { id: 'alpha', attributes: { name: 'Alpha' } },
          ],
        },
      },
    ]);
    const projects = await fetchProjects();
    expect(projects.map((p) => p.id)).toEqual(['alpha', 'zeta']);
  });

  it('throws on a non-OK response', async () => {
    installFetchMock([{ method: 'GET', match: /\/projects/, respond: () => jsonResponse({}, 401) }]);
    await expect(fetchProjects()).rejects.toThrow();
  });

  it('sends the bearer token when configured', async () => {
    vi.stubEnv('VITE_BEARER_TOKEN', 'tok');
    const fetchMock = installFetchMock([{ method: 'GET', match: /\/projects/, json: { data: [] } }]);
    await fetchProjects();
    expect((fetchMock.mock.calls[0][1]?.headers as Record<string, string>)['Authorization']).toBe('Bearer tok');
  });

  it('falls back through the id/name fields and drops rows without an id', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/projects/,
        json: {
          data: [
            { attributes: { id: 'beta', name: 'Beta' } }, // id from attributes.id
            { id: 'gamma' }, // name falls back to the id
            { attributes: {} }, // no id anywhere -> dropped
          ],
        },
      },
    ]);
    expect(await fetchProjects()).toEqual([
      { id: 'beta', name: 'Beta' },
      { id: 'gamma', name: 'gamma' },
    ]);
  });

  it('returns an empty list when the response has no data array', async () => {
    installFetchMock([{ method: 'GET', match: /\/projects/, json: {} }]);
    expect(await fetchProjects()).toEqual([]);
  });
});

describe('fetchArticle', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('returns the article body', async () => {
    installFetchMock([{ method: 'GET', match: /disclaimer\.html$/, respond: () => new Response('<p>text</p>') }]);
    await expect(fetchArticle(DISCLAIMER_URL)).resolves.toBe('<p>text</p>');
  });

  it('returns null for a missing or empty article', async () => {
    installFetchMock([{ method: 'GET', match: /a\.html$/, respond: () => new Response('', { status: 404 }) }]);
    await expect(fetchArticle('/a.html')).resolves.toBeNull();
    installFetchMock([{ method: 'GET', match: /b\.html$/, respond: () => new Response('\n  \n') }]);
    await expect(fetchArticle('/b.html')).resolves.toBeNull();
  });
});
