import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The two pages built on CustomTemplatesPage: the filename templates (one setting, no configuration
// selector) and the header/footer cells (named configurations). What they own, and what is asserted
// here, is the document they send: the opt-in flag plus one field per editor - and the fact that
// switching the opt-in off clears the templates, which is what the legacy pages stored too.

const origUrl = window.location.pathname + window.location.search;

const FILENAME_STORED = {
  useCustomValues: true,
  documentNameTemplate: 'doc-$id',
  reportNameTemplate: 'report-$id',
  testRunNameTemplate: 'run-$id',
};
const FILENAME_DEFAULTS = {
  useCustomValues: false,
  documentNameTemplate: '$document.id',
  reportNameTemplate: '$page.id',
  testRunNameTemplate: '$testRun.id',
};

const filenameRoutes = (overrides: Route[] = []): Route[] => [
  ...overrides,
  { method: 'GET', match: /\/settings\/filename-template\/names\/Default\/content/, json: FILENAME_STORED },
  { method: 'GET', match: /\/settings\/filename-template\/default-content/, json: FILENAME_DEFAULTS },
  { method: 'PUT', match: /\/settings\/filename-template\/names\/Default\/content/, json: {} },
  { method: 'GET', match: /\/settings\/filename-template\/names\/[^/]+\/revisions/, json: [] },
];

const headerFooterRoutes = (): Route[] => [
  { method: 'GET', match: /\/settings\/header-footer\/names\?/, json: [{ name: 'Default', scope: '' }] },
  {
    method: 'GET',
    match: /\/settings\/header-footer\/names\/[^/]+\/content/,
    json: {
      useCustomValues: true,
      headerLeft: 'left',
      headerCenter: '',
      headerRight: '',
      footerLeft: '',
      footerCenter: 'page $n',
      footerRight: '',
    },
  },
  {
    method: 'GET',
    match: /\/settings\/header-footer\/default-content/,
    json: {
      useCustomValues: false,
      headerLeft: 'DEFAULT LEFT',
      headerCenter: '',
      headerRight: '',
      footerLeft: '',
      footerCenter: '',
      footerRight: '',
    },
  },
  { method: 'PUT', match: /\/settings\/header-footer\/names\/[^/]+\/content/, json: {} },
  { method: 'GET', match: /\/settings\/header-footer\/names\/[^/]+\/revisions/, json: [] },
];

const open = (feature: string, routes: Route[]) => {
  const fetchMock = installFetchMock(routes);
  window.history.replaceState({}, '', `?feature=${feature}&embedded=true&scope=project/elibrary/`);
  render(<App />);
  return fetchMock;
};

const field = (id: string) => document.querySelector<HTMLTextAreaElement>(`#${id}`)!;
const clickButton = async (label: string) => {
  const button = Array.from(document.querySelectorAll<HTMLElement>('button, .sbb-btn')).find(
    (b) => b.textContent?.trim() === label,
  )!;
  await userEvent.click(button);
};
const savedBody = (fetchMock: ReturnType<typeof installFetchMock>) => {
  const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')!;
  return JSON.parse(String(put[1]!.body)) as Record<string, unknown>;
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-header-footer=; path=/; max-age=0';
});

describe('Filename template page', () => {
  it('loads the single setting without a configuration selector', async () => {
    open('filename', filenameRoutes());

    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('doc-$id'));
    expect(field('custom-reportNameTemplate').value).toBe('report-$id');
    expect(field('custom-testRunNameTemplate').value).toBe('run-$id');
    // No named configurations for this feature, so no pane.
    expect(document.querySelector('.configurations-pane')).toBeNull();
    expect(document.body.textContent).toContain('Supported special variables');
  });

  it('shows the built-in templates read-only on the second tab', async () => {
    open('filename', filenameRoutes());
    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('doc-$id'));

    await userEvent.click(Array.from(document.querySelectorAll<HTMLElement>('.tabs .tab')).at(-1)!);

    await vi.waitFor(() => expect(field('default-documentNameTemplate')).not.toBeNull());
    expect(field('default-documentNameTemplate').value).toBe('$document.id');
    expect(field('default-documentNameTemplate').readOnly).toBe(true);
  });

  it('saves the three templates with the opt-in flag', async () => {
    const fetchMock = open('filename', filenameRoutes());
    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('doc-$id'));

    await userEvent.fill(field('custom-documentNameTemplate'), '$project-$id');
    await clickButton('Save');

    await vi.waitFor(() => expect(fetchMock.mock.calls.some(([, i]) => i?.method === 'PUT')).toBe(true));
    expect(savedBody(fetchMock)).toEqual({
      useCustomValues: true,
      documentNameTemplate: '$project-$id',
      reportNameTemplate: 'report-$id',
      testRunNameTemplate: 'run-$id',
    });
  });

  it('clears the templates when the opt-in is switched off', async () => {
    // The legacy page stored empty strings rather than keeping values nothing reads.
    const fetchMock = open('filename', filenameRoutes());
    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('doc-$id'));

    await userEvent.click(document.querySelector<HTMLInputElement>('#use-custom-values')!);
    await clickButton('Save');

    await vi.waitFor(() => expect(fetchMock.mock.calls.some(([, i]) => i?.method === 'PUT')).toBe(true));
    expect(savedBody(fetchMock)).toEqual({
      useCustomValues: false,
      documentNameTemplate: '',
      reportNameTemplate: '',
      testRunNameTemplate: '',
    });
  });

  it('reports a setting it cannot read', async () => {
    open('filename', [
      {
        method: 'GET',
        match: /\/settings\/filename-template\/names\/Default\/content/,
        json: { message: 'nope' },
        status: 500,
      },
      { method: 'GET', match: /\/settings\/filename-template\/default-content/, json: FILENAME_DEFAULTS },
    ]);

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
  });
});

describe('Header and footer page', () => {
  it('edits six cells over a named configuration', async () => {
    const fetchMock = open('header-footer', headerFooterRoutes());

    await vi.waitFor(() => expect(field('custom-headerLeft').value).toBe('left'));
    expect(document.querySelector('.configurations-pane')).not.toBeNull();
    expect(document.querySelectorAll('.template-editor').length).toBe(6);

    await userEvent.fill(field('custom-footerRight'), 'page $n of $total');
    await clickButton('Save');

    await vi.waitFor(() => expect(fetchMock.mock.calls.some(([, i]) => i?.method === 'PUT')).toBe(true));
    expect(savedBody(fetchMock)).toEqual({
      useCustomValues: true,
      headerLeft: 'left',
      headerCenter: '',
      headerRight: '',
      footerLeft: '',
      footerCenter: 'page $n',
      footerRight: 'page $n of $total',
    });
  });
});
