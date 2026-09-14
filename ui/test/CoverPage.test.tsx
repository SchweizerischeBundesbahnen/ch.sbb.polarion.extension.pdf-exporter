import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The cover page: the shared templates page over an HTML and a CSS editor, plus the one thing only
// this page has - the predefined templates the extension ships, any of which an administrator copies
// into the configuration being edited.

const origUrl = window.location.pathname + window.location.search;

const STORED = { useCustomValues: true, templateHtml: '<h1>$title</h1>', templateCss: 'h1 { font-size: 40px; }' };

const routes = (overrides: Route[] = []): Route[] => [
  ...overrides,
  { method: 'GET', match: /\/settings\/cover-page\/names\?/, json: [{ name: 'Default', scope: 'project/elibrary/' }] },
  { method: 'GET', match: /\/settings\/cover-page\/names\/[^/]+\/content/, json: STORED },
  {
    method: 'GET',
    match: /\/settings\/cover-page\/default-content/,
    json: {
      useCustomValues: false,
      templateHtml: '<h1>default</h1>',
      templateCss: 'h1 {}',
      defaultHash: 'default-hash',
    },
  },
  { method: 'GET', match: /\/settings\/cover-page\/templates$/, json: ['Corporate', 'Minimal'] },
  {
    method: 'POST',
    match: /\/settings\/cover-page\/templates\/Minimal\/content/,
    json: { templateHtml: '<h1>minimal</h1>', templateCss: '', defaultHash: 'minimal-hash' },
  },
  { method: 'PUT', match: /\/settings\/cover-page\/names\/[^/]+\/content/, json: {} },
  { method: 'GET', match: /\/settings\/cover-page\/names\/[^/]+\/revisions/, json: [] },
  { method: 'DELETE', match: /\/settings\/cover-page\/names\/[^/]+\/images/, json: {} },
  { method: 'DELETE', match: /\/settings\/cover-page\/names\/[^/?]+/, json: {} },
];

const open = (list: Route[] = routes()) => {
  const fetchMock = installFetchMock(list);
  window.history.replaceState({}, '', '?feature=cover-page&embedded=true&scope=project/elibrary/');
  render(<App />);
  return fetchMock;
};

const html = () => document.querySelector<HTMLTextAreaElement>('#custom-templateHtml')!;
const clickButton = async (label: string) => {
  await userEvent.click(
    Array.from(document.querySelectorAll<HTMLElement>('button, .sbb-btn')).find(
      (b) => b.textContent?.trim() === label,
    )!,
  );
};
const answerDialog = async (label: string) => {
  await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
  Array.from(document.querySelectorAll<HTMLButtonElement>('.rsp-modal-footer .sbb-btn'))
    .find((b) => (b.textContent ?? '').trim() === label)!
    .click();
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-cover-page=; path=/; max-age=0';
});

describe('Cover page', () => {
  it('edits the HTML and the CSS of the stored configuration', async () => {
    const fetchMock = open();

    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));
    expect(document.querySelector<HTMLTextAreaElement>('#custom-templateCss')!.value).toBe('h1 { font-size: 40px; }');

    await userEvent.fill(html(), '<h1>$project</h1>');
    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')!;
      expect(JSON.parse(String(put[1]!.body))).toEqual({
        useCustomValues: true,
        templateHtml: '<h1>$project</h1>',
        templateCss: 'h1 { font-size: 40px; }',
      });
    });
  });

  it('copies the chosen predefined template into the configuration', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));
    await vi.waitFor(() => expect(document.querySelector('#copy-source-select')).not.toBeNull());

    // No "English" among the templates here, so the first one is offered first.
    const select = document.querySelector<HTMLSelectElement>('select#copy-source-select')!;
    expect(select.value).toBe('Corporate');
    select.value = 'Minimal';
    select.dispatchEvent(new Event('change', { bubbles: true }));

    await clickButton('Copy');
    await answerDialog('OK');

    await vi.waitFor(() => expect(html().value).toBe('<h1>minimal</h1>'));
    expect(
      fetchMock.mock.calls.some(([u]) =>
        String(u).includes('/settings/cover-page/templates/Minimal/content?scope=project%2Felibrary%2F&name=Default'),
      ),
    ).toBe(true);
    // Copying only fills the form: nothing is written until Save.
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('copies the default cover page when the extension ships no predefined templates', async () => {
    open(routes([{ method: 'GET', match: /\/settings\/cover-page\/templates$/, json: [] }]));

    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));
    expect(document.querySelector('#copy-source-select')).toBeNull();

    await clickButton('Copy from default');
    await answerDialog('OK');
    await vi.waitFor(() => expect(html().value).toBe('<h1>default</h1>'));
  });

  it('asks before saving a custom cover page without HTML', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));

    await userEvent.fill(html(), '');
    await clickButton('Save');

    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')?.textContent).toContain('blank cover page'));
    await answerDialog('Cancel');
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('deletes a configuration with a single request', async () => {
    // The images used to be a second, separate call from here, which left a cover page without them
    // whenever the setting deletion failed afterwards. CoverPageSettings.delete removes both now, so
    // the page issues one request and cannot produce that state.
    const fetchMock = open();
    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));

    await userEvent.click(
      Array.from(document.querySelectorAll<HTMLElement>('.configurations-pane .sbb-btn')).find(
        (b) => b.textContent?.trim() === 'Delete',
      )!,
    );
    await answerDialog('Delete');

    await vi.waitFor(() => {
      const deletes = fetchMock.mock.calls.filter(([, init]) => init?.method === 'DELETE').map(([u]) => String(u));
      expect(deletes).toEqual([
        expect.stringContaining('/settings/cover-page/names/Default?scope=project%2Felibrary%2F'),
      ]);
    });
  });

  it('says so when the predefined templates cannot be read', async () => {
    open(
      routes([{ method: 'GET', match: /\/settings\/cover-page\/templates$/, json: { message: 'nope' }, status: 500 }]),
    );

    await vi.waitFor(() =>
      expect(document.body.textContent).toContain('Error occurred loading the list of predefined'),
    );
  });
});
