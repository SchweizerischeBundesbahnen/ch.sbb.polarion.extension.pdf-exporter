import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The Webhooks page: the installation-wide switch that decides whether it exists at all, the table of
// endpoints it stores, and the auth block each row can carry.

const origUrl = window.location.pathname + window.location.search;

const STORED = {
  webhookConfigs: [
    { url: 'https://my.domain.com/rewrite', authType: 'BEARER_TOKEN', authTokenName: 'vault-entry' },
    { url: 'https://my.domain.com/plain', authType: null, authTokenName: null },
  ],
};

const baseRoutes = (): Route[] => [
  { method: 'GET', match: /\/webhooks\/status/, json: { enabled: true } },
  { method: 'GET', match: /\/settings\/webhooks\/names\?/, json: [{ name: 'Default', scope: 'project/elibrary/' }] },
  { method: 'GET', match: /\/settings\/webhooks\/names\/[^/]+\/content/, json: STORED },
  { method: 'GET', match: /\/settings\/webhooks\/default-content/, json: { webhookConfigs: [] } },
  { method: 'GET', match: /\/settings\/webhooks\/names\/[^/]+\/revisions/, json: [] },
  { method: 'PUT', match: /\/settings\/webhooks\/names\/[^/]+\/content/, json: {} },
];

/** The first matching route wins, so an override has to come before the defaults it replaces. */
const routesWith = (...overrides: Route[]): Route[] => [...overrides, ...baseRoutes()];

const open = (routes = baseRoutes()) => {
  const fetchMock = installFetchMock(routes);
  window.history.replaceState({}, '', '?feature=webhooks&embedded=true&scope=project/elibrary/');
  render(<App />);
  return fetchMock;
};

const rows = () => Array.from(document.querySelectorAll('.webhooks-table tbody tr'));
const urlInput = (row: number) => rows()[row].querySelector<HTMLInputElement>('.webhook-url input')!;
const authCheckbox = (row: number) => rows()[row].querySelector<HTMLInputElement>('input[type="checkbox"]')!;
const tokenNameInput = (row: number) =>
  rows()[row].querySelector<HTMLInputElement>('input[aria-label="Polarion Vault entry name"]');

const clickButton = async (label: string) => {
  const button = Array.from(document.querySelectorAll<HTMLElement>('button, .sbb-btn')).find(
    (b) => b.textContent?.trim() === label,
  )!;
  await userEvent.click(button);
};

const answerDialog = async (label: string) => {
  await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
  Array.from(document.querySelectorAll<HTMLButtonElement>('.rsp-modal-footer .sbb-btn'))
    .find((b) => (b.textContent ?? '').trim() === label)!
    .click();
};

const loaded = async () => vi.waitFor(() => expect(rows().length).toBe(2));

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-webhooks=; path=/; max-age=0';
});

describe('Webhooks page', () => {
  it('says the feature is off instead of showing an unusable table', async () => {
    open(routesWith({ method: 'GET', match: /\/webhooks\/status/, json: { enabled: false } }));

    await vi.waitFor(() => expect(document.querySelector('.webhooks-disabled')).not.toBeNull());
    expect(document.body.textContent).toContain('Webhooks are not enabled');
    expect(document.querySelector('.webhooks-table')).toBeNull();
  });

  it('says so when it cannot read whether webhooks are enabled', async () => {
    open(routesWith({ method: 'GET', match: /\/webhooks\/status/, json: {}, status: 500 }));

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
    // Not "the feature is off": a failed read is not an answer, and claiming one would be a lie the
    // page cannot back up.
    expect(document.querySelector('.webhooks-disabled')).toBeNull();
    expect(document.querySelector('.webhooks-table')).toBeNull();
  });

  it('says so when the status is not JSON at all', async () => {
    open(
      routesWith({
        method: 'GET',
        match: /\/webhooks\/status/,
        respond: () => new Response('<html>login</html>', { status: 200 }),
      }),
    );

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
    expect(document.querySelector('.webhooks-disabled')).toBeNull();
  });

  it('reads a setting whose webhooks are incomplete', async () => {
    open(
      routesWith({
        method: 'GET',
        match: /\/settings\/webhooks\/names\/[^/]+\/content/,
        // A configuration written by an older version: no URL, and auth without the vault entry name.
        json: { webhookConfigs: [{}, { authType: 'BASIC_AUTH' }] },
      }),
    );
    await loaded();

    expect(urlInput(0).value).toBe('');
    expect(authCheckbox(0).checked).toBe(false);
    expect(tokenNameInput(1)!.value).toBe('');
  });

  it('shows an empty table when the setting has no webhooks entry', async () => {
    open(routesWith({ method: 'GET', match: /\/settings\/webhooks\/names\/[^/]+\/content/, json: {} }));

    await vi.waitFor(() => expect(document.querySelector('.webhooks-table')).not.toBeNull());
    expect(rows().length).toBe(0);
  });

  it('shows a row per webhook, with the auth block only where there is auth', async () => {
    open();
    await loaded();

    expect(urlInput(0).value).toBe('https://my.domain.com/rewrite');
    expect(authCheckbox(0).checked).toBe(true);
    expect(tokenNameInput(0)!.value).toBe('vault-entry');
    expect(document.querySelector('#webhook-auth-type-0')).not.toBeNull();

    expect(authCheckbox(1).checked).toBe(false);
    expect(tokenNameInput(1)).toBeNull();
  });

  it('reveals the auth block when auth is switched on', async () => {
    open();
    await loaded();

    await userEvent.click(authCheckbox(1));

    await vi.waitFor(() => expect(tokenNameInput(1)).not.toBeNull());
  });

  it('warns about a URL that is not one, without blocking the edit', async () => {
    open();
    await loaded();

    await userEvent.fill(urlInput(1), 'my.domain.com/plain');

    await vi.waitFor(() => expect(document.querySelector('.invalid-webhook')).not.toBeNull());
    expect(urlInput(1).value).toBe('my.domain.com/plain');
  });

  it('saves the table, dropping the credentials of a row whose auth is off', async () => {
    const fetchMock = open();
    await loaded();

    await userEvent.fill(urlInput(1), 'https://my.domain.com/renamed');
    await userEvent.click(authCheckbox(0));
    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      expect(JSON.parse(String(put![1]!.body))).toEqual({
        webhookConfigs: [
          // Unchecking Auth clears the stored credentials rather than keeping values nothing reads.
          { url: 'https://my.domain.com/rewrite', authType: null, authTokenName: null },
          { url: 'https://my.domain.com/renamed', authType: null, authTokenName: null },
        ],
      });
    });
  });

  it('keeps the chosen auth type of a new webhook', async () => {
    const fetchMock = open();
    await loaded();

    await userEvent.click(document.querySelector<HTMLElement>('[aria-label="Add a webhook"]')!);
    await vi.waitFor(() => expect(rows().length).toBe(3));
    await userEvent.fill(urlInput(2), 'https://my.domain.com/new');
    await userEvent.click(authCheckbox(2));
    await vi.waitFor(() => expect(document.querySelector('#webhook-auth-type-2')).not.toBeNull());

    const select = document.querySelector<HTMLSelectElement>('#webhook-auth-type-2')!;
    select.value = 'BASIC_AUTH';
    select.dispatchEvent(new Event('change', { bubbles: true }));
    await userEvent.fill(tokenNameInput(2)!, 'basic-entry');

    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      const configs = JSON.parse(String(put![1]!.body)).webhookConfigs as unknown[];
      expect(configs[2]).toEqual({
        url: 'https://my.domain.com/new',
        authType: 'BASIC_AUTH',
        authTokenName: 'basic-entry',
      });
    });
  });

  it('removes the webhook whose delete button is pressed', async () => {
    open();
    await loaded();

    await userEvent.click(rows()[0].querySelector<HTMLElement>('.row-actions button')!);

    await vi.waitFor(() => expect(rows().length).toBe(1));
    expect(urlInput(0).value).toBe('https://my.domain.com/plain');
  });

  it('empties the table when the default is confirmed', async () => {
    open();
    await loaded();

    await clickButton('Default');
    await answerDialog('OK');

    await vi.waitFor(() => expect(rows().length).toBe(0));
  });

  it('reloads the stored webhooks when the edit is cancelled', async () => {
    open();
    await loaded();
    await userEvent.fill(urlInput(0), 'https://my.domain.com/not-saved');

    await clickButton('Cancel');
    await answerDialog('OK');

    await vi.waitFor(() => expect(urlInput(0).value).toBe('https://my.domain.com/rewrite'));
  });

  it('says so when the stored webhooks cannot be read again', async () => {
    let fail = false;
    open(
      routesWith({
        method: 'GET',
        match: /\/settings\/webhooks\/names\/[^/]+\/content/,
        respond: () => new Response(JSON.stringify(fail ? { message: 'nope' } : STORED), { status: fail ? 500 : 200 }),
      }),
    );
    await loaded();

    fail = true;
    await clickButton('Cancel');
    await answerDialog('OK');

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
  });

  it('says so when the built-in webhooks cannot be read', async () => {
    open(routesWith({ method: 'GET', match: /\/settings\/webhooks\/default-content/, json: {}, status: 500 }));
    await loaded();

    await clickButton('Default');
    await answerDialog('OK');

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
  });

  it('lists the revisions and loads the one picked, without saving it', async () => {
    const fetchMock = open(
      routesWith({
        method: 'GET',
        match: /\/settings\/webhooks\/names\/[^/]+\/revisions/,
        json: [{ name: '4242', date: '2026-07-01 10:00' }],
      }),
    );
    await loaded();

    await clickButton('Revisions');
    await vi.waitFor(() => expect(document.querySelector('.revision-number')).not.toBeNull());
    await userEvent.click(document.querySelector<HTMLElement>('.revert-to-revision-button')!);

    await vi.waitFor(() => expect(fetchMock.mock.calls.some(([u]) => String(u).includes('revision=4242'))).toBe(true));
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('reports the message of a save the backend rejects', async () => {
    open(
      routesWith({
        method: 'PUT',
        match: /\/settings\/webhooks\/names\/[^/]+\/content/,
        json: { message: 'webhook url is not allowed' },
        status: 400,
      }),
    );
    await loaded();

    await clickButton('Save');

    await vi.waitFor(() => expect(document.body.textContent).toContain('webhook url is not allowed'));
  });

  it('writes nothing while the scope has no configuration at all', async () => {
    const fetchMock = open(routesWith({ method: 'GET', match: /\/settings\/webhooks\/names\?/, json: [] }));
    await vi.waitFor(() => expect(document.querySelector('.webhooks-table')).not.toBeNull());

    await clickButton('Save');

    expect(rows().length).toBe(0);
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });
});
