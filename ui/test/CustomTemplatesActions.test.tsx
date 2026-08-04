import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

// The toolbar of the shared templates page: Cancel with its confirmation, the revisions list and
// reverting to one, and what a failing save does. Driven through the Header and Footer page, the one
// that has named configurations - the filename page shares the component and differs only in that.

const origUrl = window.location.pathname + window.location.search;

const STORED = {
  useCustomValues: true,
  headerLeft: 'stored left',
  headerCenter: '',
  headerRight: '',
  footerLeft: '',
  footerCenter: '',
  footerRight: '',
};

const routes = (overrides: Route[] = []): Route[] => [
  ...overrides,
  {
    method: 'GET',
    match: /\/settings\/header-footer\/names\?/,
    json: [{ name: 'Default', scope: 'project/elibrary/' }],
  },
  { method: 'GET', match: /\/settings\/header-footer\/names\/[^/]+\/content/, json: STORED },
  { method: 'GET', match: /\/settings\/header-footer\/default-content/, json: { ...STORED, headerLeft: 'built-in' } },
  {
    method: 'GET',
    match: /\/settings\/header-footer\/names\/[^/]+\/revisions/,
    json: [{ name: '1234', date: '2026-07-01 10:00', author: 'admin' }],
  },
  { method: 'PUT', match: /\/settings\/header-footer\/names\/[^/]+\/content/, json: {} },
];

const open = (list: Route[] = routes()) => {
  const fetchMock = installFetchMock(list);
  window.history.replaceState({}, '', '?feature=header-footer&embedded=true&scope=project/elibrary/');
  render(<App />);
  return fetchMock;
};

const headerLeft = () => document.querySelector<HTMLTextAreaElement>('#custom-headerLeft')!;
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

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-header-footer=; path=/; max-age=0';
});

describe('Templates page toolbar', () => {
  it('reloads the stored templates when the edit is cancelled', async () => {
    open();
    await vi.waitFor(() => expect(headerLeft().value).toBe('stored left'));
    await userEvent.fill(headerLeft(), 'unsaved');

    await clickButton('Cancel');
    await answerDialog('OK');

    await vi.waitFor(() => expect(headerLeft().value).toBe('stored left'));
  });

  it('keeps the edit when the confirmation is dismissed', async () => {
    open();
    await vi.waitFor(() => expect(headerLeft().value).toBe('stored left'));
    await userEvent.fill(headerLeft(), 'unsaved');

    await clickButton('Cancel');
    await answerDialog('Cancel');

    expect(headerLeft().value).toBe('unsaved');
  });

  it('loads a revision into the form without saving it', async () => {
    const fetchMock = open();
    await vi.waitFor(() => expect(headerLeft().value).toBe('stored left'));

    await clickButton('Revisions');
    await vi.waitFor(() => expect(document.querySelector('.revert-to-revision-button')).not.toBeNull());
    await userEvent.click(document.querySelector<HTMLElement>('.revert-to-revision-button')!);

    await vi.waitFor(() => expect(fetchMock.mock.calls.some(([u]) => String(u).includes('revision=1234'))).toBe(true));
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('reports a failing save', async () => {
    open(
      routes([
        {
          method: 'PUT',
          match: /\/settings\/header-footer\/names\/[^/]+\/content/,
          json: { message: 'scope is read-only' },
          status: 400,
        },
      ]),
    );
    await vi.waitFor(() => expect(headerLeft().value).toBe('stored left'));

    await clickButton('Save');

    await vi.waitFor(() => expect(document.body.textContent).toContain('scope is read-only'));
  });
});
