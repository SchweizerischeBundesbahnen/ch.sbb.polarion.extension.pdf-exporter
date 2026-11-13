import { pageViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page, userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';
import { dropdownsUpgraded } from './visualHelpers';

// The Webhooks page: the installation-wide switch that decides whether it exists at all, the list of
// endpoints it stores, and the auth block each row can carry.

const origUrl = window.location.pathname + window.location.search;
const VIEWPORT = { width: window.innerWidth, height: window.innerHeight };

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

const rows = () => Array.from(document.querySelectorAll('.webhooks-list .webhook-row'));
const urlInput = (row: number) => rows()[row].querySelector<HTMLInputElement>('.webhook-url input')!;
// By id, not by "the first checkbox of the row": the fail-export one comes before it.
const authCheckbox = (row: number) =>
  rows()[row].querySelector<HTMLInputElement>('input[type="checkbox"][id^="webhook-auth-"]')!;
const failExportCheckbox = (row: number) =>
  rows()[row].querySelector<HTMLInputElement>('input[id^="webhook-fail-export-"]')!;
const authGroup = (row: number) => rows()[row].querySelector<HTMLElement>('.webhook-auth')!;
const failLabel = (row: number) => rows()[row].querySelector<HTMLElement>('.webhook-fail-export')!;
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

afterEach(async () => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-webhooks=; path=/; max-age=0';
  // A viewport set by a layout test stays with the runner, so the tests after it would measure that one.
  await page.viewport(VIEWPORT.width, VIEWPORT.height);
});

describe('Webhooks page', () => {
  it('says the feature is off instead of showing an unusable table', async () => {
    open(routesWith({ method: 'GET', match: /\/webhooks\/status/, json: { enabled: false } }));

    await vi.waitFor(() => expect(document.querySelector('.webhooks-disabled')).not.toBeNull());
    expect(document.body.textContent).toContain('Webhooks are not enabled');
    expect(document.querySelector('.webhooks-list')).toBeNull();
  });

  it('says so when it cannot read whether webhooks are enabled', async () => {
    open(routesWith({ method: 'GET', match: /\/webhooks\/status/, json: {}, status: 500 }));

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
    // Not "the feature is off": a failed read is not an answer, and claiming one would be a lie the
    // page cannot back up.
    expect(document.querySelector('.webhooks-disabled')).toBeNull();
    expect(document.querySelector('.webhooks-list')).toBeNull();
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

    await vi.waitFor(() => expect(document.querySelector('.webhooks-list')).not.toBeNull());
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

  it('leaves the rest of the row in place when auth is switched on', async () => {
    await page.viewport(1400, 800);
    open();
    await loaded();

    const before = failExportCheckbox(1).getBoundingClientRect();
    await userEvent.click(authCheckbox(1));
    await vi.waitFor(() => expect(tokenNameInput(1)).not.toBeNull());

    // The auth controls are the last part of the row, so revealing them moves nothing before them.
    const after = failExportCheckbox(1).getBoundingClientRect();
    expect(after.left).toBe(before.left);
    expect(after.top).toBe(before.top);
  });

  it('leaves the same gap between every control of a row', async () => {
    await page.viewport(1400, 800);
    open();
    await loaded();

    const box = (selector: string) => rows()[0].querySelector(selector)!.getBoundingClientRect();
    const url = box('.webhook-url input');
    const fail = box('.webhook-fail-export');
    const authLabel = box('.webhook-auth > label');
    // The element the user sees, not the select kept for assistive technology.
    const dropdown = box('.webhook-auth-type > :not([aria-hidden="true"])');
    const tokenName = box('.webhook-token-name');

    // The dropdown fills the width reserved for it, so no leftover widens the gap after it.
    expect(dropdown.width).toBe(box('.webhook-auth-type').width);

    const gaps = [
      fail.left - url.right,
      authLabel.left - fail.right,
      dropdown.left - authLabel.right,
      tokenName.left - dropdown.right,
    ];
    expect(gaps.map(Math.round)).toEqual([8, 8, 8, 8]);
  });

  it('wraps a row that does not fit instead of squeezing its controls', async () => {
    await page.viewport(1400, 800);
    open();
    await loaded();
    const wideAuthTop = authCheckbox(0).getBoundingClientRect().top;
    const wideAuthWidth = authGroup(0).getBoundingClientRect().width;
    const wideFailWidth = failLabel(0).getBoundingClientRect().width;
    expect(failExportCheckbox(0).getBoundingClientRect().top).toBe(wideAuthTop);

    await page.viewport(700, 800);
    await vi.waitFor(() => expect(authCheckbox(0).getBoundingClientRect().top).toBeGreaterThan(wideAuthTop));

    // The auth controls moved to a line of their own rather than everything shrinking to fit.
    expect(failLabel(0).getBoundingClientRect().width).toBe(wideFailWidth);
    expect(authGroup(0).getBoundingClientRect().width).toBe(wideAuthWidth);
    expect(document.documentElement.scrollWidth).toBeLessThanOrEqual(700);
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
          { url: 'https://my.domain.com/rewrite', authType: null, authTokenName: null, failExportOnError: false },
          { url: 'https://my.domain.com/renamed', authType: null, authTokenName: null, failExportOnError: false },
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
        failExportOnError: false,
      });
    });
  });

  it('reads and saves whether a webhook fails the export on error', async () => {
    const fetchMock = open(
      routesWith({
        method: 'GET',
        match: /\/settings\/webhooks\/names\/[^/]+\/content/,
        json: {
          webhookConfigs: [
            { url: 'https://my.domain.com/validate', failExportOnError: true },
            { url: 'https://my.domain.com/plain' },
          ],
        },
      }),
    );
    await loaded();

    expect(failExportCheckbox(0).checked).toBe(true);
    // A webhook stored before the option existed keeps the old behavior.
    expect(failExportCheckbox(1).checked).toBe(false);

    await userEvent.click(failExportCheckbox(0));
    await userEvent.click(failExportCheckbox(1));
    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      const configs = JSON.parse(String(put![1]!.body)).webhookConfigs as { failExportOnError: boolean }[];
      expect(configs.map((c) => c.failExportOnError)).toEqual([false, true]);
    });
  });

  it('removes the webhook whose delete button is pressed', async () => {
    open();
    await loaded();

    await userEvent.click(rows()[0].querySelector<HTMLElement>('.toolbar-button')!);

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
    await vi.waitFor(() => expect(document.querySelector('.webhooks-list')).not.toBeNull());

    await clickButton('Save');

    expect(rows().length).toBe(0);
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });
});

describe('Webhooks page, accessibility', () => {
  const settled = async () => {
    await loaded();
    await vi.waitFor(() => expect(dropdownsUpgraded()).toBe(true));
  };

  it('has no WCAG A/AA violations with a row with auth and one without', async () => {
    open();
    await settled();
    expect(await pageViolations()).toEqual([]);
  });

  // The auth type sits in a table cell with no label of its own, and axe would accept a name on the hidden
  // native <select> alone.
  it('names the auth type of a row', async () => {
    open();
    await settled();
    expect(rows()[0].querySelector('.webhook-auth-type .sd-trigger')).toHaveAccessibleName('Authentication type');
  });

  it('has no WCAG A/AA violations with an invalid URL warned about', async () => {
    open();
    await settled();
    await userEvent.fill(urlInput(1), 'my.domain.com/plain');
    await vi.waitFor(() => expect(document.querySelector('.invalid-webhook')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with the feature off', async () => {
    open(routesWith({ method: 'GET', match: /\/webhooks\/status/, json: { enabled: false } }));
    await vi.waitFor(() => expect(document.querySelector('.webhooks-disabled')).not.toBeNull());
    await vi.waitFor(() => expect(dropdownsUpgraded()).toBe(true));
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations when the status cannot be read', async () => {
    open(routesWith({ method: 'GET', match: /\/webhooks\/status/, json: {}, status: 500 }));
    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
    await vi.waitFor(() => expect(dropdownsUpgraded()).toBe(true));
    expect(await pageViolations()).toEqual([]);
  });
});
