import { pageViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';
import { dropdownsUpgraded } from './visualHelpers';

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
  {
    method: 'GET',
    match: /\/settings\/cover-page\/templates\/Corporate\/content/,
    json: { templateHtml: '<h1>corporate</h1>', templateCss: '', defaultHash: 'corporate-hash' },
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

  it('compares with a predefined template without persisting anything', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));
    await vi.waitFor(() => expect(document.querySelector('#copy-source-select')).not.toBeNull());

    await clickButton('Compare with default');

    await vi.waitFor(() => expect(document.querySelector('.compare-with-default .side-by-side')).not.toBeNull());
    expect(document.querySelector('.compare-with-default .diff-removed')!.textContent).toBe('<h1>corporate</h1>');
    // The column names the template the custom templates are compared with.
    expect(document.querySelector('.compare-with-default th')!.textContent).toBe('Default: Corporate');
    // Reading a template for a comparison is a GET: only a copy persists the images of a template.
    expect(
      fetchMock.mock.calls.some(
        ([u, init]) =>
          (init?.method ?? 'GET') === 'GET' && String(u).includes('/settings/cover-page/templates/Corporate/content'),
      ),
    ).toBe(true);
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('compares with the template chosen in Copy from, even when the custom templates were copied from another', async () => {
    const fetchMock = open(
      routes([
        {
          method: 'GET',
          match: /\/settings\/cover-page\/names\/[^/]+\/content/,
          json: { ...STORED, defaultHash: 'minimal-hash', defaultSource: 'Minimal' },
        },
      ]),
    );
    const select = () => document.querySelector<HTMLSelectElement>('select#copy-source-select')!;
    await vi.waitFor(() => expect(select().value).toBe('Minimal'));

    select().value = 'Corporate';
    select().dispatchEvent(new Event('change', { bubbles: true }));
    await clickButton('Compare with default');

    await vi.waitFor(() =>
      expect(document.querySelector('.compare-with-default th')?.textContent).toBe('Default: Corporate'),
    );
    const reads = fetchMock.mock.calls.filter(([, init]) => (init?.method ?? 'GET') === 'GET').map(([u]) => String(u));
    expect(reads.some((u) => u.includes('/settings/cover-page/templates/Corporate/content'))).toBe(true);
    expect(reads.some((u) => u.includes('/settings/cover-page/templates/Minimal/content'))).toBe(false);
  });

  it('reviews the template the custom templates were copied from, not the one chosen to copy', async () => {
    const fetchMock = open(
      routes([
        {
          method: 'GET',
          match: /\/settings\/cover-page\/names\/[^/]+\/content/,
          json: { ...STORED, defaultHash: 'former-minimal-hash', defaultSource: 'Minimal', defaultChanged: true },
        },
        {
          method: 'GET',
          match: /\/settings\/cover-page\/templates\/Minimal\/content/,
          json: {
            templateHtml: '<h1>minimal</h1>',
            templateCss: '',
            defaultHash: 'minimal-hash',
            defaultSource: 'Minimal',
          },
        },
      ]),
    );
    await vi.waitFor(() => expect(document.querySelector('.default-changed')?.textContent).toContain('"Minimal"'));
    const select = document.querySelector<HTMLSelectElement>('select#copy-source-select')!;
    await vi.waitFor(() => expect(select.value).toBe('Minimal'));

    select.value = 'Corporate';
    select.dispatchEvent(new Event('change', { bubbles: true }));
    await userEvent.click(document.querySelector<HTMLElement>('.mark-as-reviewed')!);
    await vi.waitFor(() => expect(document.querySelector('.default-changed')).toBeNull());
    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')!;
      expect(JSON.parse(String(put[1]!.body))).toMatchObject({ defaultHash: 'minimal-hash', defaultSource: 'Minimal' });
    });
  });

  it('reviews the chosen template when the custom templates do not say where they were copied from', async () => {
    const fetchMock = open(
      routes([
        {
          method: 'GET',
          match: /\/settings\/cover-page\/names\/[^/]+\/content/,
          json: { ...STORED, defaultHash: 'former-hash', defaultChanged: true },
        },
        {
          method: 'GET',
          match: /\/settings\/cover-page\/templates\/Minimal\/content/,
          json: {
            templateHtml: '<h1>minimal</h1>',
            templateCss: '',
            defaultHash: 'minimal-hash',
            defaultSource: 'Minimal',
          },
        },
      ]),
    );
    await vi.waitFor(() =>
      expect(document.querySelector('.default-changed')?.textContent).toContain('Choose it in "Copy from"'),
    );
    await vi.waitFor(() => expect(document.querySelector('#copy-source-select')).not.toBeNull());

    const select = document.querySelector<HTMLSelectElement>('select#copy-source-select')!;
    select.value = 'Minimal';
    select.dispatchEvent(new Event('change', { bubbles: true }));
    await userEvent.click(document.querySelector<HTMLElement>('.mark-as-reviewed')!);
    await vi.waitFor(() => expect(document.querySelector('.default-changed')).toBeNull());
    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')!;
      expect(JSON.parse(String(put[1]!.body))).toMatchObject({ defaultHash: 'minimal-hash', defaultSource: 'Minimal' });
    });
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

describe('Cover page, accessibility', () => {
  const loaded = async () => {
    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));
    await vi.waitFor(() => expect(document.querySelector('#copy-source-select')).not.toBeNull());
    await vi.waitFor(() => expect(dropdownsUpgraded()).toBe(true));
  };

  it('has no WCAG A/AA violations with the custom templates and a template to copy from', async () => {
    open();
    await loaded();
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations while a copy is confirmed', async () => {
    open();
    await loaded();
    await clickButton('Copy');
    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with a predefined template compared', async () => {
    open();
    await loaded();
    await clickButton('Compare with default');
    await vi.waitFor(() => expect(document.querySelector('.compare-with-default .side-by-side')).not.toBeNull());
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations when the predefined templates cannot be read', async () => {
    open(
      routes([{ method: 'GET', match: /\/settings\/cover-page\/templates$/, json: { message: 'nope' }, status: 500 }]),
    );
    await vi.waitFor(() =>
      expect(document.body.textContent).toContain('Error occurred loading the list of predefined'),
    );
    await vi.waitFor(() => expect(dropdownsUpgraded()).toBe(true));
    expect(await pageViolations()).toEqual([]);
  });
});
