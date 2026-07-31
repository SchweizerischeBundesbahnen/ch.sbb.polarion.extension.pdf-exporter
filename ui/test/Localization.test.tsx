import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The Localization page: the translation table it saves, the rows an administrator adds and removes,
// and the XLIFF export/import - the one part of this page that is not the generic named-settings shape,
// and the one an end-to-end test in Polarion cannot exercise (it downloads a file).

const origUrl = window.location.pathname + window.location.search;

// The backend stores the translations in a sorted map and the page keeps the order it is given, so the
// fixture is sorted too - the row indexes below are that order.
const STORED = {
  translations: {
    Approved: [
      { language: 'de', value: 'Genehmigt' },
      { language: 'fr', value: 'Approuvé' },
      { language: 'it', value: 'Approvato' },
    ],
    Draft: [
      { language: 'de', value: 'Entwurf' },
      { language: 'fr', value: 'Brouillon' },
      { language: 'it', value: 'Bozza' },
    ],
  },
};

const baseRoutes = (): Route[] => [
  {
    method: 'GET',
    match: /\/settings\/localization\/names\?/,
    json: [{ name: 'Default', scope: 'project/elibrary/' }],
  },
  { method: 'GET', match: /\/settings\/localization\/names\/[^/]+\/content/, json: STORED },
  { method: 'GET', match: /\/settings\/localization\/default-content/, json: { translations: { Open: [] } } },
  { method: 'GET', match: /\/settings\/localization\/names\/[^/]+\/revisions/, json: [] },
  { method: 'PUT', match: /\/settings\/localization\/names\/[^/]+\/content/, json: {} },
];

/** The first matching route wins, so an override has to come before the defaults it replaces. */
const routesWith = (...overrides: Route[]): Route[] => [...overrides, ...baseRoutes()];

const open = (routes = baseRoutes()) => {
  const fetchMock = installFetchMock(routes);
  window.history.replaceState({}, '', '?feature=localization&embedded=true&scope=project/elibrary/');
  render(<App />);
  return fetchMock;
};

const rows = () => Array.from(document.querySelectorAll('.translations-table tbody tr'));
const cell = (row: number, language: string) =>
  rows()[row].querySelector<HTMLInputElement>(`input[data-language="${language}"]`)!;
const englishTexts = () => rows().map((row) => row.querySelector<HTMLInputElement>('input[data-language="en"]')!.value);

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

/** Picks a file in a hidden file input the way the browser would, so React sees a `change`. */
const pickFile = (inputId: string, file: File) => {
  const input = document.querySelector<HTMLInputElement>(`#${inputId}`)!;
  const transfer = new DataTransfer();
  transfer.items.add(file);
  input.files = transfer.files;
  input.dispatchEvent(new Event('change', { bubbles: true }));
};

const loaded = async () => vi.waitFor(() => expect(rows().length).toBe(2));

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-localization=; path=/; max-age=0';
});

describe('Localization page', () => {
  it('shows every stored text with its three translations', async () => {
    open();
    await loaded();

    expect(englishTexts()).toEqual(['Approved', 'Draft']);
    expect(cell(1, 'de').value).toBe('Entwurf');
    expect(cell(1, 'fr').value).toBe('Brouillon');
    expect(cell(1, 'it').value).toBe('Bozza');
  });

  it('flags a translation that is missing', async () => {
    open(
      routesWith({
        method: 'GET',
        match: /\/settings\/localization\/names\/[^/]+\/content/,
        json: { translations: { Draft: [{ language: 'de', value: 'Entwurf' }] } },
      }),
    );
    await vi.waitFor(() => expect(rows().length).toBe(1));

    expect(cell(0, 'de').classList.contains('empty-value')).toBe(false);
    expect(cell(0, 'fr').classList.contains('empty-value')).toBe(true);
  });

  it('saves the edited table, keyed by the English text', async () => {
    const fetchMock = open();
    await loaded();

    await userEvent.fill(cell(0, 'de'), 'Freigegeben');
    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      const body = JSON.parse(String(put![1]!.body)) as typeof STORED;
      expect(body.translations.Approved).toEqual([
        { language: 'de', value: 'Freigegeben' },
        { language: 'fr', value: 'Approuvé' },
        { language: 'it', value: 'Approvato' },
      ]);
    });
  });

  it('adds a row and drops it again when it is left without an English text', async () => {
    const fetchMock = open();
    await loaded();

    await userEvent.click(document.querySelector<HTMLElement>('[aria-label="Add a translation"]')!);
    await vi.waitFor(() => expect(rows().length).toBe(3));
    await userEvent.fill(cell(2, 'de'), 'Verworfen');

    await clickButton('Save');

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      // A row with no English text has no key to be stored under, so it never reaches the setting.
      expect(Object.keys(JSON.parse(String(put![1]!.body)).translations)).toEqual(['Approved', 'Draft']);
    });
  });

  it('removes the row whose delete button is pressed', async () => {
    open();
    await loaded();

    await userEvent.click(rows()[0].querySelector<HTMLElement>('.row-actions button')!);

    await vi.waitFor(() => expect(englishTexts()).toEqual(['Draft']));
  });

  it('downloads the XLIFF of one language', async () => {
    const downloads: string[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      downloads.push(this.download);
    });
    const fetchMock = open(
      routesWith({
        method: 'GET',
        match: /\/settings\/localization\/names\/[^/]+\/download/,
        respond: () => new Response('<xliff version="2.0"/>', { status: 200 }),
      }),
    );
    await loaded();

    await userEvent.click(document.querySelectorAll<HTMLElement>('.language-files button')[1]);

    await vi.waitFor(() => expect(downloads).toEqual(['fr.xlf']));
    const call = fetchMock.mock.calls.find(([u]) => String(u).includes('/download'))!;
    expect(String(call[0])).toContain('/settings/localization/names/Default/download?language=fr');
  });

  it('reports a download that fails', async () => {
    open(
      routesWith({ method: 'GET', match: /\/settings\/localization\/names\/[^/]+\/download/, json: {}, status: 500 }),
    );
    await loaded();

    await userEvent.click(document.querySelectorAll<HTMLElement>('.language-files button')[0]);

    await vi.waitFor(() =>
      expect(document.body.textContent).toContain('Error downloading translations file for language de'),
    );
  });

  it('merges an imported language into the table without saving it', async () => {
    const fetchMock = open(
      routesWith({
        method: 'POST',
        match: /\/settings\/localization\/upload/,
        // "Approved" is retranslated, "Draft" is absent from the file, "Rejected" is new.
        json: { Approved: 'Freigegeben', Rejected: 'Abgelehnt' },
      }),
    );
    await loaded();

    pickFile('file-de', new File(['<xliff version="2.0"/>'], 'de.xlf', { type: 'application/xml' }));

    await vi.waitFor(() => expect(englishTexts()).toEqual(['Approved', 'Draft', 'Rejected']));
    expect(cell(0, 'de').value).toBe('Freigegeben');
    // The file is the whole language, not a patch: a text it does not mention loses its translation.
    expect(cell(1, 'de').value).toBe('');
    expect(cell(2, 'de').value).toBe('Abgelehnt');
    // The other languages of the new row stay empty, and nothing is stored until Save.
    expect(cell(2, 'fr').value).toBe('');
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);

    const upload = fetchMock.mock.calls.find(([u]) => String(u).includes('/upload'))!;
    expect(String(upload[0])).toContain('/settings/localization/upload?language=de&scope=project%2Felibrary%2F');
    expect(upload[1]!.body).toBeInstanceOf(FormData);
  });

  it('discards an import that finished after other data was loaded', async () => {
    let release: (() => void) | undefined;
    installFetchMock(
      routesWith({
        method: 'POST',
        match: /\/settings\/localization\/upload/,
        json: { Approved: 'Freigegeben', Rejected: 'Abgelehnt' },
      }),
    );
    const mocked = globalThis.fetch;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        if (String(input).includes('/upload')) {
          await new Promise<void>((resolve) => {
            release = resolve;
          });
        }
        return (mocked as typeof globalThis.fetch)(input, init);
      }),
    );
    window.history.replaceState({}, '', '?feature=localization&embedded=true&scope=project/elibrary/');
    render(<App />);
    await loaded();

    pickFile('file-de', new File(['<xliff version="2.0"/>'], 'de.xlf', { type: 'application/xml' }));
    // The administrator does not wait: the built-in translations replace the table under the upload.
    await clickButton('Default');
    await answerDialog('OK');
    await vi.waitFor(() => expect(englishTexts()).toEqual(['Open']));

    release?.();

    await vi.waitFor(() => expect(document.body.textContent).toContain('was discarded'));
    // The import belonged to the table it started from; it must not land in this one.
    expect(englishTexts()).toEqual(['Open']);
  });

  it('reports an import the backend rejects', async () => {
    open(
      routesWith({
        method: 'POST',
        match: /\/settings\/localization\/upload/,
        json: { message: 'not XLIFF' },
        status: 400,
      }),
    );
    await loaded();

    pickFile('file-it', new File(['nonsense'], 'it.xlf', { type: 'application/xml' }));

    await vi.waitFor(() =>
      expect(document.body.textContent).toContain('Error occurred while uploading translation file for language it'),
    );
  });

  it('loads the built-in translations when the default is confirmed', async () => {
    open();
    await loaded();

    await clickButton('Default');
    await answerDialog('OK');

    await vi.waitFor(() => expect(englishTexts()).toEqual(['Open']));
  });

  it('keeps the table when reverting to the default is dismissed', async () => {
    open();
    await loaded();

    await clickButton('Default');
    await answerDialog('Cancel');

    expect(englishTexts()).toEqual(['Approved', 'Draft']);
  });

  it('says so when the built-in translations cannot be read', async () => {
    open(routesWith({ method: 'GET', match: /\/settings\/localization\/default-content/, json: {}, status: 500 }));
    await loaded();

    await clickButton('Default');
    await answerDialog('OK');

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
  });

  it('reloads the stored translations when the edit is cancelled', async () => {
    open();
    await loaded();
    await userEvent.fill(cell(0, 'de'), 'not saved');

    await clickButton('Cancel');
    await answerDialog('OK');

    await vi.waitFor(() => expect(cell(0, 'de').value).toBe('Genehmigt'));
  });

  it('keeps the edit when the cancellation is dismissed', async () => {
    open();
    await loaded();
    await userEvent.fill(cell(0, 'de'), 'still editing');

    await clickButton('Cancel');
    await answerDialog('Cancel');

    expect(cell(0, 'de').value).toBe('still editing');
  });

  it('says so when the stored translations cannot be read again', async () => {
    let fail = false;
    open(
      routesWith({
        method: 'GET',
        match: /\/settings\/localization\/names\/[^/]+\/content/,
        respond: () => new Response(JSON.stringify(fail ? { message: 'nope' } : STORED), { status: fail ? 500 : 200 }),
      }),
    );
    await loaded();

    fail = true;
    await clickButton('Cancel');
    await answerDialog('OK');

    await vi.waitFor(() => expect(document.querySelector('.notifications .alert-error')).not.toBeNull());
  });

  it('lists the revisions and loads the one picked, without saving it', async () => {
    const fetchMock = open(
      routesWith({
        method: 'GET',
        match: /\/settings\/localization\/names\/[^/]+\/revisions/,
        json: [{ name: '4242', date: '2026-07-01 10:00' }],
      }),
    );
    await loaded();

    await clickButton('Revisions');
    await vi.waitFor(() => expect(document.querySelector('.revision-number')).not.toBeNull());
    await userEvent.click(document.querySelector<HTMLElement>('.revert-to-revision-button')!);

    await vi.waitFor(() => expect(fetchMock.mock.calls.some(([u]) => String(u).includes('revision=4242'))).toBe(true));
    // Reverting only fills the table: nothing is written until Save.
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('reports the message of a save the backend rejects', async () => {
    open(
      routesWith({
        method: 'PUT',
        match: /\/settings\/localization\/names\/[^/]+\/content/,
        json: { message: 'translations are not valid' },
        status: 400,
      }),
    );
    await loaded();

    await clickButton('Save');

    await vi.waitFor(() => expect(document.body.textContent).toContain('translations are not valid'));
  });

  it('reads a setting whose entries are incomplete', async () => {
    open(
      routesWith({
        method: 'GET',
        match: /\/settings\/localization\/names\/[^/]+\/content/,
        // A configuration written by an older version: no entries at all for one text, a language
        // without a value for another, and a language this page does not show.
        json: {
          translations: {
            Closed: null,
            Open: [{ language: 'de' }, { language: 'es', value: 'Abierto' }],
          },
        },
      }),
    );

    await vi.waitFor(() => expect(englishTexts()).toEqual(['Closed', 'Open']));
    expect(cell(1, 'de').value).toBe('');
  });

  it('reads a setting that carries no translations at all', async () => {
    open(routesWith({ method: 'GET', match: /\/settings\/localization\/names\/[^/]+\/content/, json: {} }));

    await vi.waitFor(() => expect(document.querySelector('.translations-table')).not.toBeNull());
    expect(rows().length).toBe(0);
  });

  it('writes nothing while the scope has no configuration at all', async () => {
    const fetchMock = open(routesWith({ method: 'GET', match: /\/settings\/localization\/names\?/, json: [] }));
    await vi.waitFor(() => expect(document.querySelector('.translations-table')).not.toBeNull());

    await clickButton('Save');
    await userEvent.click(document.querySelectorAll<HTMLElement>('.language-files button')[0]);

    expect(rows().length).toBe(0);
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
    expect(fetchMock.mock.calls.some(([u]) => String(u).includes('/download'))).toBe(false);
  });
});
