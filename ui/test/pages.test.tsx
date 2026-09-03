import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// The two pages this app wires from shared components: the User Guide (RSP `UserGuide` over generic's
// /user-guide endpoint) and Authorization (RSP `AuthorizationSettings` over this extension's
// `authorization` setting, each role set a multi-select dropdown). What is worth asserting here is the
// wiring - which endpoints are called and with which setting name - since the components themselves are
// covered in the library.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  document.querySelectorAll('.sd-portal').forEach((portal) => portal.remove());
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

/** The dropdown opens, closes and picks on mousedown, so the interactions here drive that event. */
const mousedown = (node: Element) =>
  node.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));

/** Each role set is a multi-select SearchableSelect, which inserts itself right after the <select> the
 *  component ids. Addressing it from that id keeps these helpers off the page order. */
const trigger = (kind: 'global' | 'project'): HTMLElement => {
  const container = document.querySelector(`#${kind}-roles`)?.nextElementSibling;
  if (!(container instanceof HTMLElement)) {
    throw new Error(`no ${kind} roles control`);
  }
  return container.querySelector<HTMLElement>('.sd-trigger-multi')!;
};

/** The roles one control offers, in the order it lists them. The popup renders its options only while
 *  open, and every dropdown keeps its own portal in the body - hence the open, and the aria-controls. */
const listedRoles = (kind: 'global' | 'project'): string[] => {
  mousedown(trigger(kind));
  const listbox = document.getElementById(trigger(kind).getAttribute('aria-controls')!)!;
  const labels = Array.from(listbox.querySelectorAll('.option')).map((option) => (option.textContent ?? '').trim());
  mousedown(trigger(kind));
  return labels;
};

/** The roles granted in one control, as the chips painted on its trigger. */
const granted = (kind: 'global' | 'project'): string[] =>
  Array.from(trigger(kind).querySelectorAll('.sd-chip-label')).map((chip) => (chip.textContent ?? '').trim());

/** Ticks (or unticks) one role and waits for its chip to follow, which is what proves React took the
 *  change - so a Save right after reads the new selection rather than the previous render's. */
async function toggleRole(kind: 'global' | 'project', role: string) {
  const wasGranted = granted(kind).includes(role);
  mousedown(trigger(kind));
  const listbox = document.getElementById(trigger(kind).getAttribute('aria-controls')!)!;
  const option = Array.from(listbox.querySelectorAll('.option')).find((o) => (o.textContent ?? '').trim() === role);
  if (!option) {
    throw new Error(`no option for role "${role}"`);
  }
  mousedown(option);
  mousedown(trigger(kind));
  await vi.waitFor(() => expect(granted(kind).includes(role)).toBe(!wasGranted));
}

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
  it('lists the roles of the scope with the stored ones granted', async () => {
    installFetchMock(authorizationRoutes(['admin', 'developer'], ['project_admin'], ['admin']));
    window.history.replaceState({}, '', '?feature=authorization&embedded=true&scope=project/elibrary/');
    render(<App />);

    // Both controls, not just the first: they are upgraded asynchronously, and an assertion made
    // between the two reads the page mid-upgrade.
    await vi.waitFor(() => expect(document.querySelectorAll('.roles-group .sd-trigger-multi')).toHaveLength(2));
    expect(listedRoles('global')).toEqual(['admin', 'developer']);
    expect(listedRoles('project')).toEqual(['project_admin']);
    expect(granted('global')).toEqual(['admin']);
    expect(granted('project')).toEqual([]);
    expect(document.body.textContent).toContain('PDF Exporter: Authorization');
    // Global and project roles are two groups; the project one only appears when the scope has roles.
    expect(document.querySelectorAll('.roles-group').length).toBe(2);
  });

  it('saves the selection to this extension’s authorization setting', async () => {
    const fetchMock = installFetchMock(authorizationRoutes(['admin', 'developer'], []));
    window.history.replaceState({}, '', '?feature=authorization&embedded=true&scope=');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelectorAll('.roles-group .sd-trigger-multi')).toHaveLength(1));

    await toggleRole('global', 'developer');
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
