import { pageViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';
import { dropdownsUpgraded } from './visualHelpers';

// The CSS settings page: one named configuration at a time, the custom stylesheet on one tab and the
// built-in one read-only on the other. The named-configuration selector,
// the editor and the toolbar are shared react-sbb-polarion components covered there; what this pins is
// what this page owns - the content document it sends, the default CSS it offers to keep when the custom
// CSS becomes the only one, and what the read-only tabs show.

const origUrl = window.location.pathname + window.location.search;

const CUSTOM = 'h1 { color: red; }';
const DEFAULT = 'body { font-family: sans-serif; }';

const routes = (overrides: Route[] = []): Route[] => [
  ...overrides,
  { method: 'GET', match: /\/settings\/css\/names\?/, json: [{ name: 'Default', scope: '' }] },
  {
    method: 'GET',
    match: /\/settings\/css\/names\/[^/]+\/content/,
    json: { css: CUSTOM, disableDefaultCss: false, bundleTimestamp: '2026-07-01 10:00' },
  },
  {
    method: 'GET',
    match: /\/settings\/css\/default-content/,
    json: { css: DEFAULT, disableDefaultCss: false, defaultHash: 'css-hash' },
  },
  { method: 'PUT', match: /\/settings\/css\/names\/[^/]+\/content/, json: {} },
  { method: 'GET', match: /\/settings\/css\/names\/[^/]+\/revisions/, json: [] },
];

const open = (list: Route[] = routes()) => {
  const fetchMock = installFetchMock(list);
  window.history.replaceState({}, '', '?feature=css&embedded=true&scope=project/elibrary/');
  render(<App />);
  return fetchMock;
};

const editor = (id: string) => document.querySelector<HTMLTextAreaElement>(`#${id}`)!;
const clickButton = async (label: string) => {
  const button = Array.from(document.querySelectorAll<HTMLElement>('button, .sbb-btn')).find(
    (b) => b.textContent?.trim() === label,
  )!;
  await userEvent.click(button);
};
const clickTab = async (label: string) => {
  await userEvent.click(
    Array.from(document.querySelectorAll<HTMLElement>('.tabs .tab')).find((t) => t.textContent?.trim() === label)!,
  );
};
const answerDialog = async (label: string) => {
  await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
  Array.from(document.querySelectorAll<HTMLButtonElement>('.rsp-modal-footer .sbb-btn'))
    .find((b) => (b.textContent ?? '').trim() === label)!
    .click();
};
const savedBody = (fetchMock: ReturnType<typeof installFetchMock>) => {
  const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
  return put ? (JSON.parse(String(put[1]!.body)) as Record<string, unknown>) : undefined;
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe('CSS page', () => {
  it('loads the selected configuration into the editor', async () => {
    open();
    await vi.waitFor(() => expect(editor('custom-css-input')).not.toBeNull());
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));
    expect(document.body.textContent).toContain('PDF Exporter: CSS');
    expect(document.querySelector<HTMLInputElement>('#use-default-css')!.checked).toBe(true);
  });

  it('shows the built-in stylesheet read-only on the second tab', async () => {
    open();
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));

    await clickTab('Default CSS');

    await vi.waitFor(() => expect(editor('default-css-input')).not.toBeNull());
    expect(editor('default-css-input').value).toBe(DEFAULT);
    expect(editor('default-css-input').readOnly).toBe(true);
  });

  it('saves the stylesheet and the choice of the custom CSS only as one document', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));

    await userEvent.click(document.querySelector<HTMLInputElement>('#disable-default-css')!);
    await answerDialog('Cancel');
    await userEvent.fill(editor('custom-css-input'), 'p { margin: 0; }');
    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      expect(String(put![0])).toContain('/settings/css/names/Default/content?scope=project%2Felibrary%2F');
      expect(savedBody(fetchMock)).toEqual({ css: 'p { margin: 0; }', disableDefaultCss: true });
    });
  });

  it('puts the default CSS in front of the custom CSS when that becomes the only one', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));

    await userEvent.click(document.querySelector<HTMLInputElement>('#disable-default-css')!);
    await answerDialog('OK');

    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(`${DEFAULT}\n\n${CUSTOM}`));
    await clickButton('Save');

    await vi.waitFor(() =>
      expect(savedBody(fetchMock)).toEqual({
        css: `${DEFAULT}\n\n${CUSTOM}`,
        disableDefaultCss: true,
        defaultHash: 'css-hash',
      }),
    );
  });

  it('offers inserting and comparing the default CSS only while the custom CSS is the only one', async () => {
    open();
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));
    const insert = () => document.querySelector<HTMLButtonElement>('.insert-default-css');
    expect(insert()).toBeNull();
    expect(document.querySelector('.compare-with-default-button')).toBeNull();

    await userEvent.click(document.querySelector<HTMLInputElement>('#disable-default-css')!);
    await answerDialog('Cancel');
    await vi.waitFor(() => expect(insert()).not.toBeNull());
    expect(document.querySelector('.compare-with-default-button')).not.toBeNull();

    await userEvent.fill(editor('custom-css-input'), '');
    await clickButton('Insert default CSS');
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(DEFAULT));
  });

  it('says when the default CSS changed since the custom CSS was copied from it, and compares them', async () => {
    open(
      routes([
        {
          method: 'GET',
          match: /\/settings\/css\/names\/[^/]+\/content/,
          json: { css: CUSTOM, disableDefaultCss: true, defaultHash: 'former', defaultChanged: true },
        },
      ]),
    );
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));
    expect(document.querySelector('.default-changed')).not.toBeNull();

    await clickButton('Compare with default');
    await vi.waitFor(() => expect(document.querySelector('.compare-with-default .diff-removed')).not.toBeNull());
    expect(document.querySelector('.compare-with-default .diff-removed')!.textContent).toContain(DEFAULT);
    expect(document.querySelector('.compare-with-default .diff-added')!.textContent).toContain(CUSTOM);
  });

  it('marks a changed default CSS as reviewed, to be saved with the configuration', async () => {
    const fetchMock = open(
      routes([
        {
          method: 'GET',
          match: /\/settings\/css\/names\/[^/]+\/content/,
          json: { css: CUSTOM, disableDefaultCss: true, defaultHash: 'former', defaultChanged: true },
        },
      ]),
    );
    await vi.waitFor(() => expect(document.querySelector('.default-changed')).not.toBeNull());

    await clickButton('Mark as reviewed');
    await vi.waitFor(() => expect(document.querySelector('.default-changed')).toBeNull());
    await clickButton('Save');

    await vi.waitFor(() =>
      expect(savedBody(fetchMock)).toEqual({ css: CUSTOM, disableDefaultCss: true, defaultHash: 'css-hash' }),
    );
  });

  it('offers nothing from the default CSS it could not read', async () => {
    open([
      { method: 'GET', match: /\/settings\/css\/names\?/, json: [{ name: 'Default', scope: '' }] },
      {
        method: 'GET',
        match: /\/settings\/css\/names\/[^/]+\/content/,
        json: { css: CUSTOM, disableDefaultCss: true, defaultHash: 'former', defaultChanged: true },
      },
      { method: 'GET', match: /\/settings\/css\/default-content/, json: { message: 'nope' }, status: 500 },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.default-changed')).not.toBeNull());

    // Marking as reviewed would drop the version the custom CSS remembers, and nothing could be inserted.
    expect(document.querySelector<HTMLButtonElement>('.mark-as-reviewed')!.disabled).toBe(true);
    expect(document.querySelector<HTMLButtonElement>('.insert-default-css')!.disabled).toBe(true);
    expect(document.querySelector<HTMLButtonElement>('.compare-with-default-button')!.disabled).toBe(true);
  });

  it('reports a failing save instead of pretending it worked', async () => {
    const fetchMock = open(
      routes([
        { method: 'PUT', match: /\/settings\/css\/names\/[^/]+\/content/, json: { message: 'read-only' }, status: 400 },
      ]),
    );
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));

    await clickButton('Save');

    await vi.waitFor(() => expect(document.body.textContent).toContain('read-only'));
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(true);
  });

  it('says so when the built-in stylesheet cannot be read', async () => {
    open([
      { method: 'GET', match: /\/settings\/css\/names\?/, json: [{ name: 'Default', scope: '' }] },
      {
        method: 'GET',
        match: /\/settings\/css\/names\/[^/]+\/content/,
        json: { css: CUSTOM, disableDefaultCss: false },
      },
      { method: 'GET', match: /\/settings\/css\/default-content/, json: { message: 'nope' }, status: 500 },
    ]);

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
  });
});

describe('CSS page, accessibility', () => {
  const loaded = async () => {
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));
    await vi.waitFor(() => expect(dropdownsUpgraded()).toBe(true));
  };

  it('has no WCAG A/AA violations with a configuration loaded', async () => {
    open();
    await loaded();
    expect(await pageViolations()).toEqual([]);
  });

  // axe accepts the placeholder as a name, so the custom editor passes it unnamed.
  it('names both editors', async () => {
    open();
    await loaded();
    expect(editor('custom-css-input')).toHaveAccessibleName('Custom CSS');
    await clickTab('Default CSS');
    await vi.waitFor(() => expect(editor('default-css-input')).not.toBeNull());
    expect(editor('default-css-input')).toHaveAccessibleName('Default CSS');
  });

  it('has no WCAG A/AA violations on the built-in stylesheet tab', async () => {
    open();
    await loaded();
    await clickTab('Default CSS');
    await vi.waitFor(() => expect(editor('default-css-input')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations while asking whether to drop the default CSS', async () => {
    open();
    await loaded();
    await userEvent.click(document.querySelector<HTMLInputElement>('#disable-default-css')!);
    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with a changed default CSS compared', async () => {
    open(
      routes([
        {
          method: 'GET',
          match: /\/settings\/css\/names\/[^/]+\/content/,
          json: { css: CUSTOM, disableDefaultCss: true, defaultHash: 'former', defaultChanged: true },
        },
      ]),
    );
    await loaded();
    await clickButton('Compare with default');
    await vi.waitFor(() => expect(document.querySelector('.compare-with-default .diff-removed')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with a load error shown', async () => {
    open([
      { method: 'GET', match: /\/settings\/css\/names\?/, json: [{ name: 'Default', scope: '' }] },
      {
        method: 'GET',
        match: /\/settings\/css\/names\/[^/]+\/content/,
        json: { css: CUSTOM, disableDefaultCss: false },
      },
      { method: 'GET', match: /\/settings\/css\/default-content/, json: { message: 'nope' }, status: 500 },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
    await vi.waitFor(() => expect(dropdownsUpgraded()).toBe(true));
    expect(await pageViolations()).toEqual([]);
  });
});
