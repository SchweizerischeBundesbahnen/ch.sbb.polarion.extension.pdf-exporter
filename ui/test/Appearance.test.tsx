import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The Appearance page: which "Export to PDF" toolbar buttons the extension draws, read per scope.
// What this pins is what the page owns - the document it saves, and how a project is moved between its
// own configuration and the one of the Default Repository, which is an absent setting rather than a
// stored flag.

const origUrl = window.location.pathname + window.location.search;

const ALL_ON = {
  liveDocButton: true,
  liveReportButton: true,
  liveReportAutoExpandTools: false,
  classicWikiButton: true,
  classicWikiAutoExpandTools: false,
};

/** @param names what `settings/appearance/names` answers - the scope each `Default` was found in. */
const routes = (names: { name: string; scope: string }[], content = ALL_ON): Route[] => [
  { method: 'GET', match: /\/settings\/appearance\/names\?/, json: names },
  { method: 'GET', match: /\/settings\/appearance\/names\/Default\/content/, json: content },
  { method: 'GET', match: /\/settings\/appearance\/default-content/, json: ALL_ON },
  { method: 'GET', match: /\/settings\/appearance\/names\/Default\/revisions/, json: [] },
  { method: 'PUT', match: /\/settings\/appearance\/names\/Default\/content/, json: {} },
  { method: 'DELETE', match: /\/settings\/appearance\/names\/Default/, json: {} },
];

const open = (list: Route[], scope = 'project/elibrary/') => {
  const fetchMock = installFetchMock(list);
  window.history.replaceState({}, '', `?feature=appearance&embedded=true&scope=${scope}`);
  render(<App />);
  return fetchMock;
};

const checkbox = (id: string) => document.querySelector<HTMLInputElement>(`#${id}`)!;

const clickButton = async (label: string) => {
  const button = Array.from(document.querySelectorAll<HTMLElement>('button, .sbb-btn')).find(
    (b) => b.textContent?.trim() === label,
  )!;
  await userEvent.click(button);
};

const savedBody = (fetchMock: ReturnType<typeof installFetchMock>) => {
  const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
  return put ? (JSON.parse(String(put[1]!.body)) as Record<string, boolean>) : undefined;
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe('Appearance page', () => {
  it('saves every flag of the project it is opened in', async () => {
    const fetchMock = open(routes([{ name: 'Default', scope: 'project/elibrary/' }]));
    await vi.waitFor(() => expect(checkbox('live-doc-button')).toBeTruthy());

    await userEvent.click(checkbox('live-doc-button'));
    await userEvent.click(checkbox('live-report-expand'));
    await clickButton('Save');

    await vi.waitFor(() => expect(savedBody(fetchMock)).toBeDefined());
    expect(savedBody(fetchMock)).toEqual({ ...ALL_ON, liveDocButton: false, liveReportAutoExpandTools: true });
    expect(fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')![0]).toContain(
      'scope=project%2Felibrary%2F',
    );
  });

  it('starts out inheriting when the project stores nothing of its own', async () => {
    open(routes([{ name: 'Default', scope: '' }]));
    await vi.waitFor(() => expect(checkbox('appearance-inherit')).toBeTruthy());

    expect(checkbox('appearance-inherit').checked).toBe(true);
    // The values shown are the inherited ones, and not editable while they are inherited.
    expect(document.querySelector<HTMLFieldSetElement>('.appearance-options')!.disabled).toBe(true);
  });

  it('drops the project configuration when the project goes back to inheriting', async () => {
    const fetchMock = open(routes([{ name: 'Default', scope: 'project/elibrary/' }]));
    await vi.waitFor(() => expect(checkbox('appearance-inherit')).toBeTruthy());
    expect(checkbox('appearance-inherit').checked).toBe(false);

    await userEvent.click(checkbox('appearance-inherit'));
    await clickButton('Save');

    await vi.waitFor(() => expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(true));
    // Deleted, not saved: inheriting is the absence of a setting in this scope.
    expect(savedBody(fetchMock)).toBeUndefined();
  });

  it('offers no inherit option in the Default Repository, which has nothing to inherit from', async () => {
    open(routes([{ name: 'Default', scope: '' }]), '');
    await vi.waitFor(() => expect(checkbox('live-doc-button')).toBeTruthy());

    expect(document.querySelector('#appearance-inherit')).toBeNull();
    expect(document.querySelector<HTMLFieldSetElement>('.appearance-options')!.disabled).toBe(false);
  });

  it('clears the expand option when its button is switched off', async () => {
    const fetchMock = open(routes([{ name: 'Default', scope: 'project/elibrary/' }]));
    await vi.waitFor(() => expect(checkbox('classic-wiki-expand')).toBeTruthy());

    await userEvent.click(checkbox('classic-wiki-expand'));
    expect(checkbox('classic-wiki-expand').checked).toBe(true);

    // Left ticked behind a disabled control it would read as something that still happens.
    await userEvent.click(checkbox('classic-wiki-button'));
    expect(checkbox('classic-wiki-expand').checked).toBe(false);

    await clickButton('Save');
    await vi.waitFor(() => expect(savedBody(fetchMock)).toBeDefined());
    expect(savedBody(fetchMock)).toMatchObject({ classicWikiButton: false, classicWikiAutoExpandTools: false });
  });

  it('keeps the expand option when its button is switched back on', async () => {
    open(routes([{ name: 'Default', scope: 'project/elibrary/' }]));
    await vi.waitFor(() => expect(checkbox('live-report-button')).toBeTruthy());

    await userEvent.click(checkbox('live-report-expand'));
    await userEvent.click(checkbox('live-report-button'));
    await userEvent.click(checkbox('live-report-button'));

    // Switching the button back on restores nothing: the option was cleared, not remembered.
    expect(checkbox('live-report-button').checked).toBe(true);
    expect(checkbox('live-report-expand').checked).toBe(false);
  });

  it('disables the expand option of a toolbar whose button is switched off', async () => {
    open(routes([{ name: 'Default', scope: 'project/elibrary/' }]));
    await vi.waitFor(() => expect(checkbox('classic-wiki-button')).toBeTruthy());

    expect(checkbox('classic-wiki-expand').disabled).toBe(false);
    await userEvent.click(checkbox('classic-wiki-button'));
    expect(checkbox('classic-wiki-expand').disabled).toBe(true);
  });
});
