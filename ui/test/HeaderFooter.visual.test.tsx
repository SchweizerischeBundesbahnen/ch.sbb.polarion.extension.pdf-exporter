import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import type { Route } from './mockFetch';
import { filled, found, snapshotFeature } from './visualHelpers';

// Docker-only snapshots of the header and footer page: six editors in two rows of three, which is the
// layout the legacy page had and the one thing a CSS change here would silently break. One snapshot per
// state of "Different first page": off, on with the custom parts open, on with the first page parts open, and
// stored on while the default header and footer is chosen, where it shows off and disabled.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-header-footer=; path=/; max-age=0';
});

const routes = (differentFirstPage: boolean, useCustomValues = true): Route[] => [
  {
    method: 'GET',
    match: /\/settings\/header-footer\/names\?/,
    json: [{ name: 'Default', scope: 'project/elibrary/' }],
  },
  {
    method: 'GET',
    match: /\/settings\/header-footer\/names\/[^/]+\/content/,
    json: {
      useCustomValues,
      headerLeft: '{{ PROJECT_NAME }}',
      headerCenter: '{{ DOCUMENT_TITLE }}',
      headerRight: '{{ REVISION }}',
      footerLeft: '{{ TIMESTAMP }}',
      footerCenter: '{{ PAGE_NUMBER }} / {{ PAGES_TOTAL_COUNT }}',
      footerRight: '{{ PRODUCT_NAME }}',
      differentFirstPage,
      firstPageHeaderLeft: '',
      firstPageHeaderCenter: '<b>{{ DOCUMENT_TITLE }}</b>',
      firstPageHeaderRight: '',
      firstPageFooterLeft: '',
      firstPageFooterCenter: 'Confidential',
      firstPageFooterRight: '',
    },
  },
  {
    method: 'GET',
    match: /\/settings\/header-footer\/default-content/,
    json: {
      useCustomValues: false,
      headerLeft: '',
      headerCenter: '',
      headerRight: '',
      footerLeft: '',
      footerCenter: '',
      footerRight: '',
      differentFirstPage: false,
    },
  },
  { method: 'GET', match: /\/settings\/header-footer\/names\/[^/]+\/revisions/, json: [] },
];

/** Opens the tab of the first page parts. */
const openFirstPageTab = async () => {
  const tab = Array.from(document.querySelectorAll<HTMLLIElement>('.tabs .tab')).find(
    (t) => t.textContent?.trim() === 'First Page Templates',
  )!;
  await userEvent.click(tab.querySelector('label')!);
  await vi.waitFor(() => expect(filled('#custom-firstPageHeaderCenter')()).toBe(true));
};

describe.skipIf(!__PIXEL_REFERENCES__)('Header and footer page visual', () => {
  it('six cells of a custom header and footer', async () => {
    await snapshotFeature('header-footer', routes(false), filled('#custom-headerLeft'), 'header-footer-loaded');
    expect(true).toBe(true);
  });

  it('a different first page, on the custom parts', async () => {
    await snapshotFeature(
      'header-footer',
      routes(true),
      filled('#custom-headerLeft'),
      'header-footer-different-first-page',
    );
    expect(true).toBe(true);
  });

  it('a different first page, on the first page parts', async () => {
    await snapshotFeature(
      'header-footer',
      routes(true),
      filled('#custom-headerLeft'),
      'header-footer-first-page-tab',
      undefined,
      openFirstPageTab,
    );
    expect(true).toBe(true);
  });

  it('a different first page stored, with the default header and footer', async () => {
    await snapshotFeature(
      'header-footer',
      routes(true, false),
      found('#default-headerLeft'),
      'header-footer-default-with-first-page',
    );
    expect(true).toBe(true);
  });
});
