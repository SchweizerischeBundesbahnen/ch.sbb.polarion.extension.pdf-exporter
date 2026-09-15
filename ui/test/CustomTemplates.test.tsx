import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The two pages built on CustomTemplatesPage: the filename templates (one setting, no configuration
// selector) and the header/footer cells (named configurations). What they own, and what is asserted
// here, is the document they send - the choice between built-in and custom templates plus one field per
// editor, stored whatever the choice says - and what the page does around it: the custom templates
// read-only while the built-in ones are chosen, copying and comparing the built-in ones, and the
// questions it asks before a save that would leave the export without them.

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
  defaultHash: 'filename-hash',
};

const filenameRoutes = (overrides: Route[] = []): Route[] => [
  ...overrides,
  { method: 'GET', match: /\/settings\/filename-template\/names\/Default\/content/, json: FILENAME_STORED },
  { method: 'GET', match: /\/settings\/filename-template\/default-content/, json: FILENAME_DEFAULTS },
  { method: 'PUT', match: /\/settings\/filename-template\/names\/Default\/content/, json: {} },
  { method: 'GET', match: /\/settings\/filename-template\/names\/[^/]+\/revisions/, json: [] },
];

const HEADER_FOOTER_STORED = {
  useCustomValues: true,
  headerLeft: 'left',
  headerCenter: '',
  headerRight: '',
  footerLeft: '',
  footerCenter: 'page $n',
  footerRight: '',
};

const headerFooterRoutes = (overrides: Route[] = []): Route[] => [
  ...overrides,
  { method: 'GET', match: /\/settings\/header-footer\/names\?/, json: [{ name: 'Default', scope: '' }] },
  { method: 'GET', match: /\/settings\/header-footer\/names\/[^/]+\/content/, json: HEADER_FOOTER_STORED },
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
      defaultHash: 'header-footer-hash',
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
const radio = (id: string) => document.querySelector<HTMLInputElement>(`#${id}`)!;
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
const puts = (fetchMock: ReturnType<typeof installFetchMock>) =>
  fetchMock.mock.calls.filter(([, init]) => init?.method === 'PUT');
const savedBody = (fetchMock: ReturnType<typeof installFetchMock>) =>
  JSON.parse(String(puts(fetchMock)[0][1]!.body)) as Record<string, unknown>;

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
    // A configuration using the built-in templates opens on their tab.
    open(
      'filename',
      filenameRoutes([
        {
          method: 'GET',
          match: /\/settings\/filename-template\/names\/Default\/content/,
          json: { ...FILENAME_STORED, useCustomValues: false },
        },
      ]),
    );

    await vi.waitFor(() => expect(field('default-documentNameTemplate')).not.toBeNull());
    expect(field('default-documentNameTemplate').value).toBe('$document.id');
    expect(field('default-documentNameTemplate').readOnly).toBe(true);
  });

  it('saves the three templates with the choice', async () => {
    const fetchMock = open('filename', filenameRoutes());
    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('doc-$id'));

    await userEvent.fill(field('custom-documentNameTemplate'), '$project-$id');
    await clickButton('Save');

    await vi.waitFor(() => expect(puts(fetchMock).length).toBe(1));
    expect(savedBody(fetchMock)).toEqual({
      useCustomValues: true,
      documentNameTemplate: '$project-$id',
      reportNameTemplate: 'report-$id',
      testRunNameTemplate: 'run-$id',
    });
  });

  it('keeps the templates when the default ones are chosen', async () => {
    // Choosing the built-in templates must not throw away what the administrator wrote.
    const fetchMock = open('filename', filenameRoutes());
    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('doc-$id'));

    await userEvent.click(radio('use-default-values'));
    await clickButton('Save');

    await vi.waitFor(() => expect(puts(fetchMock).length).toBe(1));
    expect(savedBody(fetchMock)).toEqual({
      useCustomValues: false,
      documentNameTemplate: 'doc-$id',
      reportNameTemplate: 'report-$id',
      testRunNameTemplate: 'run-$id',
    });
  });

  it('opens the tab of the chosen templates, and disables the tab of the other ones', async () => {
    const tab = (label: string) =>
      Array.from(document.querySelectorAll<HTMLLIElement>('.tabs .tab')).find((t) => t.textContent?.trim() === label)!;
    const tabDisabled = (label: string) => tab(label).querySelector<HTMLInputElement>('input[type="radio"]')!.disabled;

    open('filename', filenameRoutes());
    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('doc-$id'));
    expect(document.querySelector('.copy-from-default')).not.toBeNull();
    expect(tabDisabled('Custom Templates')).toBe(false);
    expect(tabDisabled('Default Templates')).toBe(true);

    await userEvent.click(radio('use-default-values'));
    await vi.waitFor(() => expect(field('default-documentNameTemplate')).not.toBeNull());
    expect(field('custom-documentNameTemplate')).toBeNull();
    expect(tabDisabled('Custom Templates')).toBe(true);
    expect(tabDisabled('Default Templates')).toBe(false);

    // The custom templates do not apply, so their tab does not open.
    tab('Custom Templates').querySelector('label')!.click();
    expect(field('custom-documentNameTemplate')).toBeNull();

    await userEvent.click(radio('use-custom-values'));
    await vi.waitFor(() => expect(field('custom-documentNameTemplate').readOnly).toBe(false));
    expect(document.querySelector('.copy-from-default')).not.toBeNull();
    expect(tabDisabled('Default Templates')).toBe(true);
  });

  it('copies the default templates over the custom ones once the replacement is confirmed', async () => {
    const fetchMock = open('filename', filenameRoutes());
    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('doc-$id'));

    await clickButton('Copy from default');
    await answerDialog('OK');

    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('$document.id'));
    await clickButton('Save');

    // The hash says which version of the built-in templates the custom ones started from.
    await vi.waitFor(() => expect(puts(fetchMock).length).toBe(1));
    expect(savedBody(fetchMock)).toEqual({
      useCustomValues: true,
      documentNameTemplate: '$document.id',
      reportNameTemplate: '$page.id',
      testRunNameTemplate: '$testRun.id',
      defaultHash: 'filename-hash',
    });
  });

  it('keeps the custom templates when the replacement is dismissed', async () => {
    open('filename', filenameRoutes());
    await vi.waitFor(() => expect(field('custom-documentNameTemplate').value).toBe('doc-$id'));

    await clickButton('Copy from default');
    await answerDialog('Cancel');

    expect(field('custom-documentNameTemplate').value).toBe('doc-$id');
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

    await vi.waitFor(() => expect(puts(fetchMock).length).toBe(1));
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

  it('asks before saving a custom header and footer with every cell empty', async () => {
    const fetchMock = open('header-footer', headerFooterRoutes());
    await vi.waitFor(() => expect(field('custom-headerLeft').value).toBe('left'));

    await userEvent.fill(field('custom-headerLeft'), '');
    await userEvent.fill(field('custom-footerCenter'), '');
    await clickButton('Save');

    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')?.textContent).toContain('Save anyway'));
    await answerDialog('Cancel');
    expect(puts(fetchMock).length).toBe(0);

    await clickButton('Save');
    await answerDialog('OK');
    await vi.waitFor(() => expect(puts(fetchMock).length).toBe(1));
  });

  it('says when the default changed and compares the custom cells with it', async () => {
    open(
      'header-footer',
      headerFooterRoutes([
        {
          method: 'GET',
          match: /\/settings\/header-footer\/names\/[^/]+\/content/,
          json: { ...HEADER_FOOTER_STORED, defaultHash: 'former', defaultChanged: true },
        },
      ]),
    );
    await vi.waitFor(() => expect(field('custom-headerLeft').value).toBe('left'));
    expect(document.querySelector('.default-changed')).not.toBeNull();

    await clickButton('Compare with default');

    await vi.waitFor(() => expect(document.querySelector('.compare-with-default .side-by-side')).not.toBeNull());
    const diff = document.querySelector('.compare-with-default')!;
    // The default on the left, the custom value on the right, facing each other in one row.
    const row = diff.querySelector('.side-by-side tbody tr')!;
    expect(row.querySelector('.diff-removed')!.textContent).toBe('DEFAULT LEFT');
    expect(row.querySelector('.diff-added')!.textContent).toBe('left');
    expect(diff.querySelector('.side-by-side thead')!.textContent).toBe('DefaultCustom');
    expect(diff.textContent).toContain('No differences.');

    await clickButton('Close');
    await vi.waitFor(() => expect(document.querySelector('.compare-with-default .side-by-side')).toBeNull());
  });

  it('marks a changed default as reviewed, to be saved with the configuration', async () => {
    const fetchMock = open(
      'header-footer',
      headerFooterRoutes([
        {
          method: 'GET',
          match: /\/settings\/header-footer\/names\/[^/]+\/content/,
          json: { ...HEADER_FOOTER_STORED, defaultHash: 'former', defaultChanged: true },
        },
      ]),
    );
    await vi.waitFor(() => expect(document.querySelector('.default-changed')).not.toBeNull());

    await clickButton('Mark as reviewed');
    await vi.waitFor(() => expect(document.querySelector('.default-changed')).toBeNull());
    expect(puts(fetchMock).length).toBe(0);

    await clickButton('Save');
    await vi.waitFor(() => expect(puts(fetchMock).length).toBe(1));
    expect(savedBody(fetchMock).defaultHash).toBe('header-footer-hash');
  });

  it('opens a configuration using the default templates on their tab, without the changed notice', async () => {
    open(
      'header-footer',
      headerFooterRoutes([
        {
          method: 'GET',
          match: /\/settings\/header-footer\/names\/[^/]+\/content/,
          json: { ...HEADER_FOOTER_STORED, useCustomValues: false, defaultChanged: true },
        },
      ]),
    );
    await vi.waitFor(() => expect(field('default-headerLeft')?.value).toBe('DEFAULT LEFT'));
    expect(field('custom-headerLeft')).toBeNull();
    expect(document.querySelector('.default-changed')).toBeNull();
  });
});
