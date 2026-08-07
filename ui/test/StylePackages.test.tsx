import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { FetchMock, Route } from './mockFetch';

// The Style Packages page: the four child settings a package points at, the ~30 switches it carries,
// and what each of them writes into the stored document.

const origUrl = window.location.pathname + window.location.search;

const SCOPE = 'project/elibrary/';

/** Names of a child setting: one of this scope, one inherited from the global scope. */
const childNames = (name: string) => [
  { name: 'Default', scope: SCOPE },
  { name, scope: '' },
];

const STORED = {
  matchingQuery: 'type:testrun',
  weight: 42.5,
  exposeSettings: true,
  coverPage: 'Fancy cover',
  css: 'Default',
  headerFooter: 'Default',
  localization: 'Default',
  webhooks: 'Rewriter',
  headersColor: '#004d73',
  paperSize: 'A3',
  orientation: 'LANDSCAPE',
  pdfVariant: 'PDF_A_2U',
  imageDensity: 'DPI_300',
  fullFonts: true,
  fitToPage: true,
  renderComments: 'ALL',
  includeUnreferencedComments: true,
  renderNativeComments: false,
  cutEmptyWorkitemAttributes: true,
  specificChapters: '1,2',
  metadataFields: 'docOwner',
  customNumberedListStyles: '1ai',
  language: 'fr',
  linkedWorkitemRoles: ['relates_to'],
  linkRoleDirection: 'DIRECT',
  workItemsQuery: 'type:requirement',
  attachmentsFilter: '*.pdf',
  testcaseFieldId: 'withAttachments',
  embedAttachments: true,
  exposePageWidthValidation: true,
};

const baseRoutes = (): Route[] => [
  { method: 'GET', match: /\/webhooks\/status/, json: { enabled: true } },
  { method: 'GET', match: /\/link-role-names/, json: ['relates_to', 'verifies'] },
  { method: 'GET', match: /\/settings\/cover-page\/names\?/, json: childNames('Fancy cover') },
  { method: 'GET', match: /\/settings\/css\/names\?/, json: childNames('Compact') },
  { method: 'GET', match: /\/settings\/header-footer\/names\?/, json: childNames('With logo') },
  { method: 'GET', match: /\/settings\/localization\/names\?/, json: childNames('German') },
  { method: 'GET', match: /\/settings\/webhooks\/names\?/, json: childNames('Rewriter') },
  {
    method: 'GET',
    match: /\/settings\/style-package\/names\?/,
    // "Test runs" first, so the pane preselects a package that has a matching query of its own.
    json: [
      { name: 'Test runs', scope: SCOPE },
      { name: 'Default', scope: SCOPE },
    ],
  },
  { method: 'GET', match: /\/settings\/style-package\/names\/[^/]+\/content/, json: STORED },
  { method: 'GET', match: /\/settings\/style-package\/default-content/, json: { weight: 50 } },
  { method: 'GET', match: /\/settings\/style-package\/names\/[^/]+\/revisions/, json: [] },
  { method: 'PUT', match: /\/settings\/style-package\/names\/[^/]+\/content/, json: {} },
];

/** The first matching route wins, so an override has to come before the defaults it replaces. */
const routesWith = (...overrides: Route[]): Route[] => [...overrides, ...baseRoutes()];

const open = (routes = baseRoutes()) => {
  const fetchMock = installFetchMock(routes);
  window.history.replaceState({}, '', `?feature=style-package&embedded=true&scope=${SCOPE}`);
  render(<App />);
  return fetchMock;
};

const field = <T extends HTMLElement>(selector: string) => document.querySelector<T>(selector);
const input = (id: string) => document.querySelector<HTMLInputElement>(`#${id}`)!;
const select = (id: string) => document.querySelector<HTMLSelectElement>(`#${id}`);

/** The form is filled once the selected style package's content has landed. */
const loaded = async () => vi.waitFor(() => expect(input('style-package-weight').value).toBe('42.5'));

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

/** Picks a value on a SearchableSelect through the <select> it keeps as its source of truth. */
const choose = async (element: HTMLSelectElement, value: string) => {
  element.value = value;
  element.dispatchEvent(new Event('change', { bubbles: true }));
  await vi.waitFor(() => expect(element.value).toBe(value));
};

const pick = (id: string, value: string) => choose(select(id)!, value);

/** The configuration selector of the pane, which carries no id of its own. */
const selectPackage = (name: string) =>
  choose(document.querySelector<HTMLSelectElement>('.configurations-pane select')!, name);

/** Selects exactly `values` on a multi-select SearchableSelect. */
const pickAll = async (id: string, values: string[]) => {
  const element = select(id)!;
  for (const option of Array.from(element.options)) {
    option.selected = values.includes(option.value);
  }
  element.dispatchEvent(new Event('change', { bubbles: true }));
  await vi.waitFor(() => expect(Array.from(element.selectedOptions).map((o) => o.value)).toEqual(values));
};

/**
 * Types into a control userEvent cannot drive, e.g. the color swatch. The value goes in through the
 * prototype setter: React overrides the instance one to remember what it last rendered, and assigning
 * through that would make React treat the following event as a no-op.
 */
const nativeValueSetter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!;

const setValue = async (id: string, value: string) => {
  const element = input(id);
  nativeValueSetter.call(element, value);
  element.dispatchEvent(new Event('input', { bubbles: true }));
  await vi.waitFor(() => expect(element.value).toBe(value));
};

const savedBody = async (fetchMock: FetchMock): Promise<Record<string, unknown>> => {
  let body: Record<string, unknown> | undefined;
  await vi.waitFor(() => {
    const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
    expect(put).toBeDefined();
    body = JSON.parse(String(put![1]!.body)) as Record<string, unknown>;
  });
  return body!;
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-style-package=; path=/; max-age=0';
});

describe('Style Packages page', () => {
  it('fills every control from the stored style package', async () => {
    open();
    await loaded();

    expect(input('matching-query').value).toBe('type:testrun');
    expect(input('exposeSettings').checked).toBe(true);
    expect(input('cover-page-checkbox').checked).toBe(true);
    expect(select('cover-page-select')!.value).toBe('Fancy cover');
    expect(select('css-select')!.value).toBe('Default');
    expect(select('paper-size-select')!.value).toBe('A3');
    expect(select('orientation-select')!.value).toBe('LANDSCAPE');
    expect(select('pdf-variant-select')!.value).toBe('PDF_A_2U');
    expect(select('image-density-select')!.value).toBe('DPI_300');
    expect(input('full-fonts').checked).toBe(true);
    expect(input('render-comments').checked).toBe(true);
    expect(select('render-comments-select')!.value).toBe('ALL');
    expect(input('include-unreferenced-comments').checked).toBe(true);
    expect(input('render-native-comments').checked).toBe(false);
    expect(input('watermark').checked).toBe(false);
    expect(input('cut-empty-wi-attributes').checked).toBe(true);
    expect(input('chapters').value).toBe('1,2');
    expect(input('metadata-fields-input').value).toBe('docOwner');
    expect(input('numbered-list-styles').value).toBe('1ai');
    expect(select('language-select')!.value).toBe('fr');
    expect(select('roles-direction-select')!.value).toBe('DIRECT');
    expect(input('work-items-query').value).toBe('type:requirement');
    expect(input('attachments-filter').value).toBe('*.pdf');
    expect(input('testcase-field-id').value).toBe('withAttachments');
    expect(input('embed-attachments').checked).toBe(true);
    expect(input('expose-page-width-validation').checked).toBe(true);
  });

  it('marks the child configurations that come from a parent scope', async () => {
    open();
    await loaded();

    const options = Array.from(select('css-select')!.options).map((o) => o.textContent);
    expect(options).toEqual(['Default', 'Compact (inherited)']);
  });

  it('falls back to Default for a child configuration the scope no longer offers', async () => {
    const fetchMock = open(
      routesWith({
        method: 'GET',
        match: /\/settings\/style-package\/names\/[^/]+\/content/,
        json: { ...STORED, css: 'Deleted one' },
      }),
    );
    await loaded();

    expect(select('css-select')!.value).toBe('Default');

    await clickButton('Save');
    expect((await savedBody(fetchMock)).css).toBe('Default');
  });

  it('hides the matching query of the Default style package, which applies to every document', async () => {
    open();
    await vi.waitFor(() => expect(field('#matching-query-container')).not.toBeNull());

    await selectPackage('Default');

    await vi.waitFor(() => expect(field('#matching-query-container')).toBeNull());
  });

  it('reveals the value of a switch only while it is on, without losing what was typed', async () => {
    open();
    await loaded();

    expect(input('chapters').disabled).toBe(false);
    await userEvent.click(input('specific-chapters'));

    await vi.waitFor(() => expect(input('chapters').disabled).toBe(true));
    // The text stays in the field: ticking the box again has to bring the old value back, not a blank.
    expect(input('chapters').value).toBe('1,2');
    expect(input('chapters').className).toContain('hidden');
  });

  it('drops the sub-controls of a switch that is off from the layout', async () => {
    open();
    await loaded();

    await userEvent.click(input('selected-roles'));
    await vi.waitFor(() => expect(select('roles-select')).toBeNull());
    expect(select('roles-direction-select')).toBeNull();

    await userEvent.click(input('download-attachments'));
    await vi.waitFor(() => expect(field('#attachments-filter')).toBeNull());
    expect(field('#testcase-field-id')).toBeNull();
    expect(field('#embed-attachments')).toBeNull();
  });

  it('seeds the attachments filter with every file when the switch is turned on', async () => {
    open(
      routesWith({
        method: 'GET',
        match: /\/settings\/style-package\/names\/[^/]+\/content/,
        json: { ...STORED, attachmentsFilter: null, testcaseFieldId: null, embedAttachments: false },
      }),
    );
    await loaded();
    expect(input('download-attachments').checked).toBe(false);

    await userEvent.click(input('download-attachments'));

    await vi.waitFor(() => expect(input('attachments-filter').value).toBe('*.*'));
  });

  it('normalizes the weight when the field is left', async () => {
    open();
    await loaded();

    await userEvent.fill(input('style-package-weight'), '150.55');
    input('style-package-weight').blur();
    await vi.waitFor(() => expect(input('style-package-weight').value).toBe('100'));

    await userEvent.fill(input('style-package-weight'), '7.77');
    input('style-package-weight').blur();
    await vi.waitFor(() => expect(input('style-package-weight').value).toBe('7.8'));

    await userEvent.fill(input('style-package-weight'), '');
    input('style-package-weight').blur();
    await vi.waitFor(() => expect(input('style-package-weight').value).toBe('50'));
  });

  it('saves the style package as it stands', async () => {
    const fetchMock = open();
    await loaded();

    await clickButton('Save');

    expect(await savedBody(fetchMock)).toEqual({
      matchingQuery: 'type:testrun',
      weight: 42.5,
      exposeSettings: true,
      coverPage: 'Fancy cover',
      css: 'Default',
      headerFooter: 'Default',
      localization: 'Default',
      webhooks: 'Rewriter',
      headersColor: '#004d73',
      paperSize: 'A3',
      orientation: 'LANDSCAPE',
      pdfVariant: 'PDF_A_2U',
      imageDensity: 'DPI_300',
      fitToPage: true,
      renderComments: 'ALL',
      renderNativeComments: false,
      includeUnreferencedComments: true,
      watermark: false,
      markReferencedWorkitems: false,
      cutEmptyChapters: false,
      cutEmptyWorkitemAttributes: true,
      cutLocalURLs: false,
      followHTMLPresentationalHints: false,
      specificChapters: '1,2',
      metadataFields: 'docOwner',
      customNumberedListStyles: '1ai',
      language: 'fr',
      languageCustomField: null,
      linkedWorkitemRoles: ['relates_to'],
      linkRoleDirection: 'DIRECT',
      exposePageWidthValidation: true,
      attachmentsFilter: '*.pdf',
      testcaseFieldId: 'withAttachments',
      embedAttachments: true,
      fullFonts: true,
      workItemsQuery: 'type:requirement',
    });
  });

  it('stores null for every switch that was turned off, not the value behind it', async () => {
    const fetchMock = open();
    await loaded();

    for (const id of [
      'cover-page-checkbox',
      'render-comments',
      'specific-chapters',
      'metadata-fields',
      'custom-list-styles',
      'localization',
      'selected-roles',
      'work-items-query-checkbox',
      'download-attachments',
    ]) {
      await userEvent.click(input(id));
    }
    await clickButton('Save');

    const body = await savedBody(fetchMock);
    expect(body).toMatchObject({
      coverPage: null,
      renderComments: null,
      specificChapters: null,
      metadataFields: null,
      customNumberedListStyles: null,
      language: null,
      linkedWorkitemRoles: null,
      linkRoleDirection: null,
      workItemsQuery: null,
      attachmentsFilter: null,
      testcaseFieldId: null,
      // Nothing is downloaded, so nothing can be embedded either.
      embedAttachments: false,
    });
  });

  it('points the style package at the child configurations that were picked', async () => {
    const fetchMock = open();
    await loaded();

    await pick('cover-page-select', 'Default');
    await pick('css-select', 'Compact');
    await pick('header-footer-select', 'With logo');
    await pick('localization-select', 'German');
    await pick('webhooks-select', 'Default');
    await clickButton('Save');

    // The stored value is the configuration's name; "(inherited)" is a label, not part of it.
    expect(await savedBody(fetchMock)).toMatchObject({
      coverPage: 'Default',
      css: 'Compact',
      headerFooter: 'With logo',
      localization: 'German',
      webhooks: 'Default',
    });
  });

  it('writes the changed dropdowns and switches', async () => {
    const fetchMock = open();
    await loaded();

    await pick('paper-size-select', 'LETTER');
    await pick('pdf-variant-select', 'PDF_UA_2');
    await pick('image-density-select', 'DPI_600');
    await userEvent.click(input('watermark'));
    await userEvent.click(input('cut-urls'));
    await clickButton('Save');

    expect(await savedBody(fetchMock)).toMatchObject({
      paperSize: 'LETTER',
      pdfVariant: 'PDF_UA_2',
      imageDensity: 'DPI_600',
      watermark: true,
      cutLocalURLs: true,
    });
  });

  it('carries every control of a style package built from scratch into the stored document', async () => {
    // Nothing but the weight is stored, so every switch starts off and every sub-control is hidden.
    const fetchMock = open(
      routesWith({ method: 'GET', match: /\/settings\/style-package\/names\/[^/]+\/content/, json: { weight: 50 } }),
    );
    await vi.waitFor(() => expect(input('style-package-weight').value).toBe('50'));

    for (const id of [
      'exposeSettings',
      'cover-page-checkbox',
      'webhooks-checkbox',
      'full-fonts',
      'fit-to-page',
      'presentational-hints',
      'render-comments',
      'watermark',
      'cut-empty-chapters',
      'cut-empty-wi-attributes',
      'cut-urls',
      'mark-referenced-workitems',
      'custom-list-styles',
      'specific-chapters',
      'metadata-fields',
      'localization',
      'selected-roles',
      'work-items-query-checkbox',
      'download-attachments',
      'expose-page-width-validation',
    ]) {
      await userEvent.click(input(id));
    }

    // The sub-controls the switches above revealed.
    await vi.waitFor(() => expect(field('#embed-attachments')).not.toBeNull());
    await userEvent.click(input('include-unreferenced-comments'));
    await userEvent.click(input('render-native-comments'));
    await userEvent.click(input('embed-attachments'));

    await userEvent.fill(input('matching-query'), 'type:document');
    await userEvent.fill(input('numbered-list-styles'), '1ai');
    await userEvent.fill(input('chapters'), '3,4');
    await userEvent.fill(input('metadata-fields-input'), 'docLanguage');
    await userEvent.fill(input('work-items-query'), 'type:task');
    await userEvent.fill(input('attachments-filter'), '*.docx');
    await userEvent.fill(input('testcase-field-id'), 'hasFiles');
    await setValue('headers-color', '#ff0000');

    await pick('orientation-select', 'LANDSCAPE');
    await pick('render-comments-select', 'ALL');
    await pick('language-select', 'it');
    await pick('roles-direction-select', 'REVERSE');
    await pickAll('roles-select', ['verifies']);

    await clickButton('Save');

    expect(await savedBody(fetchMock)).toEqual({
      matchingQuery: 'type:document',
      weight: 50,
      exposeSettings: true,
      coverPage: 'Default',
      css: 'Default',
      headerFooter: 'Default',
      localization: 'Default',
      webhooks: 'Default',
      headersColor: '#ff0000',
      paperSize: 'A4',
      orientation: 'LANDSCAPE',
      pdfVariant: 'PDF_A_2B',
      imageDensity: 'DPI_96',
      fitToPage: true,
      renderComments: 'ALL',
      renderNativeComments: true,
      includeUnreferencedComments: true,
      watermark: true,
      markReferencedWorkitems: true,
      cutEmptyChapters: true,
      cutEmptyWorkitemAttributes: true,
      cutLocalURLs: true,
      followHTMLPresentationalHints: true,
      specificChapters: '3,4',
      metadataFields: 'docLanguage',
      customNumberedListStyles: '1ai',
      language: 'it',
      languageCustomField: null,
      linkedWorkitemRoles: ['verifies'],
      linkRoleDirection: 'REVERSE',
      exposePageWidthValidation: true,
      attachmentsFilter: '*.docx',
      testcaseFieldId: 'hasFiles',
      embedAttachments: true,
      fullFonts: true,
      workItemsQuery: 'type:task',
    });
  });

  it('offers no webhooks configuration when the installation has webhooks off', async () => {
    open(routesWith({ method: 'GET', match: /\/webhooks\/status/, json: { enabled: false } }));
    await loaded();

    expect(field('#webhooks-checkbox')).toBeNull();
  });

  it('keeps the stored webhooks configuration even though the row is not shown', async () => {
    const fetchMock = open(routesWith({ method: 'GET', match: /\/webhooks\/status/, json: {}, status: 500 }));
    await loaded();
    expect(field('#webhooks-checkbox')).toBeNull();

    await clickButton('Save');

    // A status that could not be read is no reason to throw the reference away.
    expect((await savedBody(fetchMock)).webhooks).toBe('Rewriter');
  });

  it('says so when the names of the child configurations cannot be read', async () => {
    open(routesWith({ method: 'GET', match: /\/settings\/css\/names\?/, json: {}, status: 500 }));

    await vi.waitFor(() =>
      expect(document.body.textContent).toContain('error loading names of children configurations'),
    );
  });

  it('treats a child setting without a single configuration as an error too', async () => {
    open(routesWith({ method: 'GET', match: /\/settings\/header-footer\/names\?/, json: [] }));

    await vi.waitFor(() =>
      expect(document.body.textContent).toContain('error loading names of children configurations'),
    );
  });

  it('says so when the link role names cannot be read', async () => {
    open(routesWith({ method: 'GET', match: /\/link-role-names/, json: {}, status: 500 }));

    await vi.waitFor(() => expect(document.body.textContent).toContain('error loading link role names'));
    // The rest of the page still works: only the role picker is short of its options.
    await loaded();
  });

  it('loads the built-in values when the default is confirmed', async () => {
    open();
    await loaded();

    await clickButton('Default');
    await answerDialog('OK');

    await vi.waitFor(() => expect(input('style-package-weight').value).toBe('50'));
    expect(input('exposeSettings').checked).toBe(false);
    expect(select('paper-size-select')!.value).toBe('A4');
    expect(select('pdf-variant-select')!.value).toBe('PDF_A_2B');
  });

  it('reloads the stored style package when the edit is cancelled', async () => {
    open();
    await loaded();
    await userEvent.fill(input('matching-query'), 'not saved');

    await clickButton('Cancel');
    await answerDialog('OK');

    await vi.waitFor(() => expect(input('matching-query').value).toBe('type:testrun'));
  });

  it('says so when the stored style package cannot be read again', async () => {
    let fail = false;
    open(
      routesWith({
        method: 'GET',
        match: /\/settings\/style-package\/names\/[^/]+\/content/,
        respond: () => new Response(JSON.stringify(fail ? { message: 'nope' } : STORED), { status: fail ? 500 : 200 }),
      }),
    );
    await loaded();

    fail = true;
    await clickButton('Cancel');
    await answerDialog('OK');

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
  });

  it('says so when the built-in values cannot be read', async () => {
    open(routesWith({ method: 'GET', match: /\/settings\/style-package\/default-content/, json: {}, status: 500 }));
    await loaded();

    await clickButton('Default');
    await answerDialog('OK');

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
  });

  it('lists the revisions and loads the one picked, without saving it', async () => {
    const fetchMock = open(
      routesWith({
        method: 'GET',
        match: /\/settings\/style-package\/names\/[^/]+\/revisions/,
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
        match: /\/settings\/style-package\/names\/[^/]+\/content/,
        json: { message: 'weight is already taken' },
        status: 400,
      }),
    );
    await loaded();

    await clickButton('Save');

    await vi.waitFor(() => expect(document.body.textContent).toContain('weight is already taken'));
  });

  it('writes nothing while the scope has no style package at all', async () => {
    const fetchMock = open(routesWith({ method: 'GET', match: /\/settings\/style-package\/names\?/, json: [] }));
    await vi.waitFor(() => expect(field('#style-package-weight')).not.toBeNull());

    await clickButton('Save');

    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });
});
