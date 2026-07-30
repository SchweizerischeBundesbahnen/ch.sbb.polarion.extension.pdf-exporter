import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The rest of the CSS page's toolbar: Cancel (with its confirmation), the revisions list and
// reverting to one, plus the named-configuration operations this page hands to the shared pane -
// create, rename and delete all go through this extension's settings endpoints, and the URLs are the
// one part of that contract the page owns.

const origUrl = window.location.pathname + window.location.search;

const STORED = 'h1 { color: red; }';

const baseRoutes = (): Route[] => [
  { method: 'GET', match: /\/settings\/css\/names\?/, json: [{ name: 'Default', scope: 'project/elibrary/' }] },
  { method: 'GET', match: /\/settings\/css\/names\/[^/]+\/content/, json: { css: STORED, disableDefaultCss: false } },
  { method: 'GET', match: /\/settings\/css\/default-content/, json: { css: 'body {}', disableDefaultCss: false } },
  {
    method: 'GET',
    match: /\/settings\/css\/names\/[^/]+\/revisions/,
    json: [{ name: '4242', date: '2026-07-01 10:00' }],
  },
  { method: 'PUT', match: /\/settings\/css\/names\/[^/]+\/content/, json: {} },
  { method: 'POST', match: /\/settings\/css\/names\/[^/]+/, json: {} },
  { method: 'DELETE', match: /\/settings\/css\/names\/[^/]+/, json: {} },
];

const editor = () => document.querySelector<HTMLTextAreaElement>('#custom-css-input')!;

const open = (routes = baseRoutes()) => {
  const fetchMock = installFetchMock(routes);
  window.history.replaceState({}, '', '?feature=css&embedded=true&scope=project/elibrary/');
  render(<App />);
  return fetchMock;
};

const clickButton = async (label: string) => {
  const button = Array.from(document.querySelectorAll<HTMLElement>('button, .sbb-btn')).find(
    (b) => b.textContent?.trim() === label,
  )!;
  await userEvent.click(button);
};

/** The shared confirmation dialog. Its confirm button is labelled per call - "OK" for the page's own
 *  Cancel, "Delete" for the pane's deletion - so the label is a parameter. */
const answerDialog = async (label: string) => {
  await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
  const target = Array.from(document.querySelectorAll<HTMLButtonElement>('.rsp-modal-footer .sbb-btn')).find(
    (b) => (b.textContent ?? '').trim() === label,
  )!;
  target.click();
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-css=; path=/; max-age=0';
});

describe('CSS page actions', () => {
  it('reloads the stored stylesheet when the edit is cancelled', async () => {
    open();
    await vi.waitFor(() => expect(editor().value).toBe(STORED));
    await userEvent.fill(editor(), 'not saved');

    await clickButton('Cancel');
    await answerDialog('OK');

    await vi.waitFor(() => expect(editor().value).toBe(STORED));
  });

  it('keeps the edit when the cancellation is dismissed', async () => {
    open();
    await vi.waitFor(() => expect(editor().value).toBe(STORED));
    await userEvent.fill(editor(), 'still editing');

    await clickButton('Cancel');
    await answerDialog('Cancel');

    expect(editor().value).toBe('still editing');
  });

  it('lists the revisions and loads the one picked, without saving it', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(editor().value).toBe(STORED));

    await clickButton('Revisions');

    await vi.waitFor(() => expect(document.querySelector('.revision-number')).not.toBeNull());
    await userEvent.click(document.querySelector<HTMLElement>('.revert-to-revision-button')!);

    await vi.waitFor(() => {
      const revisionCall = fetchMock.mock.calls.find(([u]) => String(u).includes('revision=4242'));
      expect(revisionCall).toBeDefined();
    });
    // Reverting only fills the form: nothing is written until Save.
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('clears the load error once the failed read succeeds', async () => {
    // The banner belongs to whichever read failed: Cancel retries the configuration, so it clears a
    // configuration error - and would leave a built-in-stylesheet error standing, since nothing
    // retried that one.
    let fail = true;
    installFetchMock([
      { method: 'GET', match: /\/settings\/css\/names\?/, json: [{ name: 'Default', scope: 'project/elibrary/' }] },
      { method: 'GET', match: /\/settings\/css\/default-content/, json: { css: 'body {}' } },
      {
        method: 'GET',
        match: /\/settings\/css\/names\/[^/]+\/content/,
        respond: () =>
          fail
            ? new Response(JSON.stringify({ message: 'nope' }), { status: 500 })
            : new Response(JSON.stringify({ css: STORED, disableDefaultCss: false }), { status: 200 }),
      },
    ]);
    window.history.replaceState({}, '', '?feature=css&embedded=true&scope=project/elibrary/');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());

    fail = false;
    await clickButton('Cancel');
    await answerDialog('OK');

    await vi.waitFor(() => expect(editor().value).toBe(STORED));
    expect(document.querySelector('.notifications .alert-error')).toBeNull();
  });

  it('creates a configuration through this extension’s endpoint', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(editor().value).toBe(STORED));

    await clickButton('Add new');
    const nameInput = document.querySelector<HTMLInputElement>('.config-edit-row input[type="text"]')!;
    await userEvent.fill(nameInput, 'Compact');
    const confirmCreate = Array.from(document.querySelectorAll<HTMLElement>('.config-edit-row .sbb-btn')).find(
      (b) => b.textContent?.trim() === 'Save',
    )!;
    await userEvent.click(confirmCreate);

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([u, init]) => init?.method === 'PUT' && String(u).includes('Compact'));
      expect(put).toBeDefined();
      expect(String(put![0])).toContain('/settings/css/names/Compact/content?scope=project%2Felibrary%2F');
    });
  });

  it('deletes a configuration once the deletion is confirmed', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(editor().value).toBe(STORED));

    await clickButton('Delete');
    await answerDialog('Delete');

    await vi.waitFor(() => {
      const del = fetchMock.mock.calls.find(([, init]) => init?.method === 'DELETE');
      expect(del).toBeDefined();
      expect(String(del![0])).toContain('/settings/css/names/Default?scope=project%2Felibrary%2F');
    });
  });
});
