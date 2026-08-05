import type { Root } from 'react-dom/client';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { openExportPopup } from '../src/popup/mount';
import { popupRoutes } from './exportPopupSamples';
import { installFetchMock } from './mockFetch';

// How the export dialog gets onto a Polarion page: the two toolbar injectors and the report page's export
// button import assets/export-popup.js and call openExportPopup. It appends a host of its own to the body
// and mounts into a shadow root of it, so nothing may leak either way - the pages it opens on are
// Polarion's, and the report page also carries the bulk export widget's own root.
//
// Unlike the other popup suite this one lets the dialog read its real endpoints (behind a fetch mock), since
// what is under test is the mounting: the styles that reach the root, and the location read off the page URL.

const roots: Root[] = [];
const origHash = window.location.hash;

/** The shadow root of the host openExportPopup appended, which is the last child of the body. */
const shadow = () => (document.body.lastElementChild as HTMLElement | null)?.shadowRoot ?? null;

const loaded = () => vi.waitFor(() => expect(shadow()?.querySelector('#popup-style-package-select')).not.toBeNull());

const open = (options: Parameters<typeof openExportPopup>[0] = {}) => {
  installFetchMock(popupRoutes());
  const root = openExportPopup(options);
  roots.push(root);
  return root;
};

afterEach(() => {
  // Unmount before removing the host, so the dialog's own effects run their cleanup
  roots.splice(0).forEach((root) => root.unmount());
  document.querySelectorAll('body > div').forEach((element) => {
    if (element.shadowRoot) element.remove();
  });
  window.history.replaceState({}, '', `${window.location.pathname}${window.location.search}${origHash}`);
  vi.unstubAllGlobals();
  document.cookie = 'selected-style-package=; path=/; max-age=0';
});

describe('mounting the export dialog', () => {
  it('renders the dialog into a shadow root of a host of its own', async () => {
    open({ documentType: 'LIVE_DOC', location: location('LIVE_DOC') });
    await loaded();

    // The page around it sees none of it: the dialog's markup is behind the shadow boundary
    expect(document.querySelector('#popup-style-package-select')).toBeNull();
    expect(shadow()!.querySelector('.rsp-modal')).not.toBeNull();
  });

  it('gives the shadow root the styles the dialog needs', async () => {
    open({ location: location('LIVE_DOC') });
    await loaded();

    const styles = Array.from(shadow()!.querySelectorAll('style')).map((style) => style.textContent ?? '');
    // The design tokens and controls, which react-sbb-polarion's stylesheet brings ...
    expect(styles.some((css) => css.includes('--sbb-checkbox-checked'))).toBe(true);
    // ... the base font, which nothing inside a shadow root inherits from the page ...
    expect(styles.some((css) => css.includes('--sbb-control-font-family'))).toBe(true);
    // ... and the dialog's own two-column layout, which used to be a page stylesheet
    expect(styles.some((css) => css.includes('.flex-column'))).toBe(true);
  });

  it('carries the classes the dialog CSS and the tokens are scoped to', async () => {
    open({ location: location('LIVE_DOC') });
    await loaded();

    const container = shadow()!.querySelector('div')!;
    expect(container.className).toBe('pdf-exporter form-wrapper sbb-ui');
  });

  it('styles the dialog from inside the shadow root, the page having no rules for it', async () => {
    open({ location: location('LIVE_DOC') });
    await loaded();

    // Rules only export-popup.css states, checked as computed style: they prove the stylesheet is in effect
    // inside the root, not merely present as text.
    expect(getComputedStyle(shadow()!.querySelector('.property-wrapper')!).display).toBe('flex');
    expect(getComputedStyle(shadow()!.querySelector('.flex-column')!).width).toBe('340px');
  });

  it('widens the dialog beyond what the shared modal caps itself at', async () => {
    // The shared Modal stops at min(640px, 100vw - 32px), which is narrower than this form's two columns.
    open({ location: location('LIVE_DOC') });
    await loaded();

    const dialog = shadow()!.querySelector<HTMLElement>('.rsp-modal')!;
    expect(dialog.getBoundingClientRect().width).toBeGreaterThan(640);
  });

  it('reads the item out of the page URL when it is not told where it is', async () => {
    window.history.replaceState({}, '', `${window.location.pathname}#/project/elibrary/wiki/Specs/BigDoc`);

    open({ documentType: 'LIVE_DOC' });
    await loaded();

    // The file name request is what says the location was understood: it carries the path it read
    await vi.waitFor(() =>
      expect(shadow()!.querySelector<HTMLInputElement>('#popup-filename')?.value).toBe(
        'E-Library Cross Link Issue.pdf',
      ),
    );
    const fetchMock = globalThis.fetch as unknown as { mock: { calls: [string, RequestInit?][] } };
    const call = fetchMock.mock.calls.find(([url]) => String(url).includes('export-filename'))!;
    expect(JSON.parse(String(call[1]!.body))).toMatchObject({
      documentType: 'LIVE_DOC',
      projectId: 'elibrary',
      locationPath: 'Specs/BigDoc',
    });
  });

  it('resolves no location path for a bulk export, whatever the page URL says', async () => {
    // A bulk export addresses each item as it converts it; a path left over from the page would travel in
    // the request the widget reuses for every one of them.
    window.history.replaceState({}, '', `${window.location.pathname}#/project/elibrary/wiki/Specs/BigDoc`);

    open({ documentType: 'LIVE_DOC', exportType: 'BULK', identifiers: [{ documentName: 'BigDoc' }] });

    await vi.waitFor(() => expect(shadow()?.querySelector('#popup-auto-select-style-package')).not.toBeNull());
    const fetchMock = globalThis.fetch as unknown as { mock: { calls: [string, RequestInit?][] } };
    // A bulk export asks for no file name at all, which is the observable half of the same thing
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('export-filename'))).toBe(false);
  });

  it('removes its host from the page when the dialog is closed', async () => {
    open({ location: location('LIVE_DOC') });
    await loaded();
    const host = document.body.lastElementChild!;

    shadow()!.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--secondary')!.click();

    await vi.waitFor(() => expect(host.isConnected).toBe(false));
    // The root was unmounted with it, so the afterEach unmount is a no-op
    roots.length = 0;
  });
});

/** Where the sample document is, spelled out as the endpoints want it. */
function location(documentType: 'LIVE_DOC') {
  return {
    documentType,
    scope: 'project/elibrary/',
    projectId: 'elibrary',
    locationPath: '_default/Cross Link Issue',
    spaceId: '_default',
    documentName: 'Cross Link Issue',
    urlQueryParameters: {},
  };
}
