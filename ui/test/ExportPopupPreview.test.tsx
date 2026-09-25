import { pageViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import ExportPopupPreview from '../src/pages/ExportPopupPreview';
import { popupRoutes } from './exportPopupSamples';
import { installFetchMock } from './mockFetch';

// The export dialog's development harness: a document picked from the project, the document and export type
// chosen, and the real dialog opened through openExportPopup into a shadow root of its own.

const DOCUMENTS = [{ attributes: { moduleFolder: 'Default Space', moduleName: 'Cross Link Issue' } }];

const origUrl = window.location.pathname + window.location.search;

beforeEach(() => {
  document.cookie = 'pdf-exporter-dev-document=; path=/; max-age=0';
});

afterEach(() => {
  cleanup();
  document.querySelectorAll('body > div').forEach((element) => {
    if (element.shadowRoot) element.remove();
  });
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-style-package=; path=/; max-age=0';
});

const open = async () => {
  installFetchMock([
    { method: 'GET', match: /\/projects\/[^/]+\/documents/, json: { data: DOCUMENTS } },
    ...popupRoutes(),
  ]);
  window.history.replaceState({}, '', `?feature=export-popup&scope=${encodeURIComponent('project/elibrary/')}`);
  render(<ExportPopupPreview />);
  const select = await vi.waitFor(() => {
    const found = document.querySelector<HTMLSelectElement>('#dev-document-select');
    expect(found?.querySelectorAll('option').length).toBeGreaterThan(1);
    return found!;
  });
  await vi.waitFor(() =>
    expect(document.querySelectorAll('.searchable-dropdown').length).toBe(document.querySelectorAll('select').length),
  );
  return select;
};

describe('the export dialog development harness, accessibility', () => {
  it('has no WCAG A/AA violations before a document is picked', async () => {
    await open();
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with the dialog opened for a picked document', async () => {
    const select = await open();
    select.value = 'Default Space/Cross Link Issue';
    select.dispatchEvent(new Event('change', { bubbles: true }));
    const button = await vi.waitFor(() => {
      const found = Array.from(document.querySelectorAll<HTMLButtonElement>('.preview-controls button')).find(
        (b) => b.textContent === 'Open Export to PDF',
      );
      expect(found?.disabled).toBe(false);
      return found!;
    });
    await userEvent.click(button);
    await vi.waitFor(() =>
      expect(
        (document.body.lastElementChild as HTMLElement).shadowRoot?.querySelector('#popup-style-package-select'),
      ).toBeTruthy(),
    );
    expect(await pageViolations()).toEqual([]);
  });
});
