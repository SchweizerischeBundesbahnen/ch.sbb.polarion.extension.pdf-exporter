import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The cover page: the shared templates page over an HTML and a CSS editor, plus the one thing only
// this page has - the predefined templates the extension ships, which an administrator can persist
// into the current scope as a new configuration.

const origUrl = window.location.pathname + window.location.search;

const STORED = { useCustomValues: true, templateHtml: '<h1>$title</h1>', templateCss: 'h1 { font-size: 40px; }' };

const routes = (overrides: Route[] = []): Route[] => [
  ...overrides,
  { method: 'GET', match: /\/settings\/cover-page\/names\?/, json: [{ name: 'Default', scope: 'project/elibrary/' }] },
  { method: 'GET', match: /\/settings\/cover-page\/names\/[^/]+\/content/, json: STORED },
  {
    method: 'GET',
    match: /\/settings\/cover-page\/default-content/,
    json: { useCustomValues: false, templateHtml: '<h1>default</h1>', templateCss: 'h1 {}' },
  },
  { method: 'GET', match: /\/settings\/cover-page\/templates$/, json: ['Corporate', 'Minimal'] },
  { method: 'POST', match: /\/settings\/cover-page\/templates\/[^/?]+/, json: {} },
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
    await userEvent.click(
      Array.from(document.querySelectorAll<HTMLElement>('button, .sbb-btn')).find(
        (b) => b.textContent?.trim() === 'Save',
      )!,
    );

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')!;
      expect(JSON.parse(String(put[1]!.body))).toEqual({
        useCustomValues: true,
        templateHtml: '<h1>$project</h1>',
        templateCss: 'h1 { font-size: 40px; }',
      });
    });
  });

  it('persists a predefined template into this scope', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));
    await vi.waitFor(() => expect(document.querySelector('.predefined-templates')).not.toBeNull());

    await userEvent.click(
      Array.from(
        document.querySelectorAll<HTMLElement>('.predefined-templates button, .predefined-templates .sbb-btn'),
      ).find((b) => b.textContent?.trim() === 'Persist')!,
    );

    await vi.waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')!;
      expect(String(post[0])).toContain('/settings/cover-page/templates/Corporate?scope=project%2Felibrary%2F');
    });
  });

  it('hides the predefined pane when the extension ships none', async () => {
    open(routes([{ method: 'GET', match: /\/settings\/cover-page\/templates$/, json: [] }]));

    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));
    expect(document.querySelector('.predefined-templates')).toBeNull();
  });

  it('deletes the images of a configuration before the configuration itself', async () => {
    // The images are separate files in SVN found through the setting's name, so deleting the setting
    // first would strand them there. The legacy page did this as a preDeleteCallback.
    const fetchMock = open();
    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));

    await userEvent.click(
      Array.from(document.querySelectorAll<HTMLElement>('.configurations-pane .sbb-btn')).find(
        (b) => b.textContent?.trim() === 'Delete',
      )!,
    );
    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
    Array.from(document.querySelectorAll<HTMLButtonElement>('.rsp-modal-footer .sbb-btn'))
      .find((b) => (b.textContent ?? '').trim() === 'Delete')!
      .click();

    await vi.waitFor(() => {
      const deletes = fetchMock.mock.calls.filter(([, init]) => init?.method === 'DELETE').map(([u]) => String(u));
      expect(deletes.length).toBe(2);
      expect(deletes[0]).toContain('/settings/cover-page/names/Default/images?scope=project%2Felibrary%2F');
      expect(deletes[1]).toContain('/settings/cover-page/names/Default?scope=project%2Felibrary%2F');
    });
  });

  it('keeps the configuration when its images cannot be deleted', async () => {
    const fetchMock = open(
      routes([{ method: 'DELETE', match: /\/settings\/cover-page\/names\/[^/]+\/images/, json: {}, status: 500 }]),
    );
    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));

    await userEvent.click(
      Array.from(document.querySelectorAll<HTMLElement>('.configurations-pane .sbb-btn')).find(
        (b) => b.textContent?.trim() === 'Delete',
      )!,
    );
    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
    Array.from(document.querySelectorAll<HTMLButtonElement>('.rsp-modal-footer .sbb-btn'))
      .find((b) => (b.textContent ?? '').trim() === 'Delete')!
      .click();

    await vi.waitFor(() => expect(document.querySelector('.configurations-pane .alert-error')).not.toBeNull());
    const deletes = fetchMock.mock.calls.filter(([, init]) => init?.method === 'DELETE').map(([u]) => String(u));
    expect(deletes.every((u) => u.includes('/images'))).toBe(true);
  });

  it('says the images are gone when the configuration itself survives a failed deletion', async () => {
    // The two are separate requests; if the second fails, the configuration is still there but its
    // images are not, and the shared pane can only report "deletion failed".
    open(routes([{ method: 'DELETE', match: /\/settings\/cover-page\/names\/[^/?]+\?/, json: {}, status: 500 }]));
    await vi.waitFor(() => expect(html().value).toBe('<h1>$title</h1>'));

    await userEvent.click(
      Array.from(document.querySelectorAll<HTMLElement>('.configurations-pane .sbb-btn')).find(
        (b) => b.textContent?.trim() === 'Delete',
      )!,
    );
    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
    Array.from(document.querySelectorAll<HTMLButtonElement>('.rsp-modal-footer .sbb-btn'))
      .find((b) => (b.textContent ?? '').trim() === 'Delete')!
      .click();

    await vi.waitFor(() => expect(document.body.textContent).toContain('were deleted, but the configuration itself'));
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
