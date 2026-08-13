import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// The two pages this app wires from shared components: the User Guide (RSP `UserGuide` over generic's
// /user-guide endpoint) and Authorization (RSP `AuthorizationSettings` over this extension's
// `authorization` setting). What is worth asserting here is the wiring - which endpoints are called
// and with which setting name - since the components themselves are covered in the library.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

const authorizationRoutes = (globalRoles: string[], projectRoles: string[], selected: string[] = []) => [
  { method: 'GET', match: /\/roles\?/, json: { globalRoles, projectRoles } },
  {
    method: 'GET',
    match: /\/settings\/authorization\/names\/Default\/content/,
    json: { globalRoles: selected, projectRoles: [] },
  },
  { method: 'PUT', match: /\/settings\/authorization\/names\/Default\/content/, json: {} },
];

describe('User Guide page', () => {
  it('renders the article generic serves', async () => {
    const fetchMock = installFetchMock([
      { method: 'GET', match: /\/user-guide$/, respond: () => new Response('<h1>User Guide</h1><p>How to.</p>') },
    ]);
    window.history.replaceState({}, '', '?feature=user-guide&embedded=true');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());
    expect(document.body.textContent).toContain('How to.');
    expect(String(fetchMock.mock.calls[0][0])).toBe('/polarion/pdf-exporter/rest/internal/user-guide');
  });
});

describe('Authorization page', () => {
  it('lists the roles of the scope with the stored ones checked', async () => {
    installFetchMock(authorizationRoutes(['admin', 'developer'], ['project_admin'], ['admin']));
    window.history.replaceState({}, '', '?feature=authorization&embedded=true&scope=project/elibrary/');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelectorAll('.roles-list input[type="checkbox"]').length).toBe(3));
    const checked = Array.from(document.querySelectorAll<HTMLLabelElement>('.roles-list label'))
      .filter((l) => l.querySelector<HTMLInputElement>('input')!.checked)
      .map((l) => l.textContent);
    expect(checked).toEqual(['admin']);
    expect(document.body.textContent).toContain('PDF Exporter: Authorization');
    // Global and project roles are two groups; the project one only appears when the scope has roles.
    expect(document.querySelectorAll('.roles-group').length).toBe(2);
  });

  it('saves the selection to this extension’s authorization setting', async () => {
    const fetchMock = installFetchMock(authorizationRoutes(['admin', 'developer'], []));
    window.history.replaceState({}, '', '?feature=authorization&embedded=true&scope=');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelectorAll('.roles-list input[type="checkbox"]').length).toBe(2));

    await userEvent.click(document.querySelectorAll<HTMLInputElement>('.roles-list input[type="checkbox"]')[1]);
    const save = Array.from(document.querySelectorAll<HTMLElement>('button, .sbb-btn')).find(
      (b) => b.textContent?.trim() === 'Save',
    )!;
    await userEvent.click(save);

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      expect(String(put![0])).toContain('/settings/authorization/names/Default/content');
      expect(String(put![1]!.body)).toContain('developer');
    });
  });

  it('reports a scope whose roles cannot be read', async () => {
    installFetchMock([{ method: 'GET', match: /\/roles\?/, json: { message: 'no such scope' }, status: 400 }]);
    window.history.replaceState({}, '', '?feature=authorization&embedded=true');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('.alert-error, .alert')).not.toBeNull());
  });
});
