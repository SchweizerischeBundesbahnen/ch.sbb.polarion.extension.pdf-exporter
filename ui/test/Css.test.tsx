import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The CSS settings page: one named configuration at a time, the custom stylesheet on one tab and the
// built-in one read-only on the other. The named-configuration selector, the editor and the toolbar
// are shared react-sbb-polarion components covered there; what this pins is what this page owns - the
// content document it sends, and that the read-only tab shows the defaults rather than the custom CSS.

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
  { method: 'GET', match: /\/settings\/css\/default-content/, json: { css: DEFAULT, disableDefaultCss: false } },
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
  });

  it('shows the built-in stylesheet read-only on the second tab', async () => {
    open();
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));

    await userEvent.click(document.querySelector<HTMLElement>('.tabs label[for*="default"], .tabs .tab:last-child')!);

    await vi.waitFor(() => expect(editor('default-css-input')).not.toBeNull());
    expect(editor('default-css-input').value).toBe(DEFAULT);
    expect(editor('default-css-input').readOnly).toBe(true);
  });

  it('saves the stylesheet and the default-CSS flag as one document', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(editor('custom-css-input').value).toBe(CUSTOM));

    await userEvent.click(document.querySelector<HTMLInputElement>('#disable-default-css')!);
    await userEvent.fill(editor('custom-css-input'), 'p { margin: 0; }');
    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      expect(String(put![0])).toContain('/settings/css/names/Default/content?scope=project%2Felibrary%2F');
      expect(JSON.parse(String(put![1]!.body))).toEqual({ css: 'p { margin: 0; }', disableDefaultCss: true });
    });
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
