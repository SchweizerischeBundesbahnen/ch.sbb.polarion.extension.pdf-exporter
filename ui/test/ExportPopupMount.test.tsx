import type { Root } from 'react-dom/client';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { page } from 'vitest/browser';
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

/** The viewport the rest of the suite runs at, restored after the two tests that change it. */
const DEFAULT_VIEWPORT = { width: 1280, height: 720 } as const;

/**
 * The columns a section of the form is laid out in, as the browser resolved them.
 *
 * How many there are is a container query on the form (see export/export-form.css), so this is what says
 * whether the dialog is wide enough for two - and it is read off the computed style rather than measured,
 * so an empty section cannot pass for a folded one.
 */
const columnsOf = (section: Element): string[] => getComputedStyle(section).gridTemplateColumns.split(' ');

/** Everything inside the dialog that has more to show than it shows, and offers a scrollbar for it. */
const scrollers = (shadow: ShadowRoot): string[] =>
  [...new Set(shadow.querySelectorAll('*'))]
    .filter(
      (element) =>
        element.scrollHeight > element.clientHeight + 1 &&
        ['auto', 'scroll'].includes(getComputedStyle(element).overflowY),
    )
    .map((element) => element.className || element.tagName);

afterEach(async () => {
  await page.viewport(DEFAULT_VIEWPORT.width, DEFAULT_VIEWPORT.height);
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
    // ... the shared export form, whose layout used to be a page stylesheet ...
    expect(styles.some((css) => css.includes('.pdf-section'))).toBe(true);
    // ... and the dialog's own chrome, which is what is left of export-popup.css
    expect(styles.some((css) => css.includes('.in-progress-overlay'))).toBe(true);
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

    // Rules only this extension's stylesheets state, checked as computed style: they prove the sheets are
    // in effect inside the root, not merely present as text.
    expect(getComputedStyle(shadow()!.querySelector('.property-wrapper')!).display).toBe('grid');
    // The dialog is wide enough for the form's two-column layout, which is a container query on the form
    expect(columnsOf(shadow()!.querySelector('.pdf-section')!)).toHaveLength(2);
  });

  it('keeps the two settings columns side by side when a scrollbar takes width off the form', async () => {
    // Short enough that the form goes over its height cap and the content area scrolls. The scrollbar then
    // takes about 15px off the form's width, and the form's own container query is what decides how many
    // columns are left - so this is where a layout that measured the window rather than the form, or a
    // breakpoint set too close to the dialog's width, folds the two columns into one.
    //
    // The scrollbar is a real one, which the suite can only draw because vitest.config.ts passes
    // `ignoreDefaultArgs: ['--hide-scrollbars']`. Playwright hides scrollbars in headless Chromium by
    // default, and that is precisely why this class of defect once reached production with every test green.
    await page.viewport(900, 520);
    open({ location: location('LIVE_DOC') });
    await loaded();

    const content = shadow()!.querySelector<HTMLElement>('.rsp-modal-content')!;
    expect(content.scrollHeight).toBeGreaterThan(content.clientHeight); // it really does scroll
    expect(content.offsetWidth - content.clientWidth).toBeGreaterThan(0); // and the scrollbar takes width

    const section = shadow()!.querySelector<HTMLElement>('.pdf-section')!;
    expect(columnsOf(section)).toHaveLength(2);
    const [first, second] = Array.from(section.children)
      .slice(0, 2)
      .map((row) => row.getBoundingClientRect());
    expect(Math.round(first.top)).toBe(Math.round(second.top)); // the same line, i.e. not folded
    expect(Math.round(first.width)).toBe(Math.round(second.width)); // and still equal

    await page.viewport(DEFAULT_VIEWPORT.width, DEFAULT_VIEWPORT.height);
  });

  it('folds the form into one column where the window cannot hold two', async () => {
    // The dialog gives way at min(732px, 100vw - 32px), so a narrow window leaves the form under the 620px
    // its two columns need - and the rows then stack instead of being squeezed. Nothing about this is the
    // dialog's own: the panel folds at the same width, in the properties pane, from the same rule.
    await page.viewport(520, 900);
    open({ location: location('LIVE_DOC') });
    await loaded();

    expect(columnsOf(shadow()!.querySelector('.pdf-section')!)).toHaveLength(1);

    await page.viewport(DEFAULT_VIEWPORT.width, DEFAULT_VIEWPORT.height);
  });

  it('widens the dialog beyond what the shared modal caps itself at', async () => {
    // The shared Modal stops at min(640px, 100vw - 32px), which is narrower than this form's two columns.
    open({ location: location('LIVE_DOC') });
    await loaded();

    const dialog = shadow()!.querySelector<HTMLElement>('.rsp-modal')!;
    expect(dialog.getBoundingClientRect().width).toBeGreaterThan(640);
  });

  it('may use the whole window height, not the share the shared modal allows itself', async () => {
    // The regression this guards: RSP's Modal caps the dialog at 85vh, which is 135px less than this at a
    // 900px window - enough to put a scrollbar on a form that had none on the page before. The cap here is
    // the window less the 16px margin the shared modal already keeps at its sides.
    await page.viewport(1280, 900);
    open({ location: location('LIVE_DOC') });
    await loaded();

    const dialog = shadow()!.querySelector<HTMLElement>('.rsp-modal')!;
    expect(getComputedStyle(dialog).maxHeight).toBe(`${window.innerHeight - 32}px`);
    // Whatever the form's height, the dialog itself is never the scroller
    expect(scrollers(shadow()!)).not.toContain('rsp-modal');
  });

  it('scrolls its content and nothing else where the form does not fit', async () => {
    await page.viewport(1280, 560);
    open({ location: location('LIVE_DOC') });
    await loaded();

    // Exactly one scrollbar, and on the content: the title and the buttons stay where they are
    expect(scrollers(shadow()!)).toEqual(['rsp-modal-content']);

    const header = shadow()!.querySelector('.rsp-modal-header')!.getBoundingClientRect();
    const footer = shadow()!.querySelector('.rsp-modal-footer')!.getBoundingClientRect();
    const content = shadow()!.querySelector('.rsp-modal-content')!;
    content.scrollTop = content.scrollHeight;

    expect(shadow()!.querySelector('.rsp-modal-header')!.getBoundingClientRect().top).toBe(header.top);
    expect(shadow()!.querySelector('.rsp-modal-footer')!.getBoundingClientRect().bottom).toBe(footer.bottom);
    // Both are on screen, which is the point of the dialog not being the scroller
    expect(header.top).toBeGreaterThanOrEqual(0);
    expect(footer.bottom).toBeLessThanOrEqual(window.innerHeight);
  });

  /** The dropdown option lists, wherever they currently live. */
  const portalsIn = (parent: ShadowRoot | Element): Element[] =>
    [...parent.children].filter((child) => child.classList.contains('sd-portal'));

  it('puts the dropdown option lists inside the dialog, where they are painted above it', async () => {
    // The regression this guards: the shared dropdown appends its `position: fixed` option list to the
    // element's root node - the shadow root, a *sibling* of the dialog. RSP's Modal is a native <dialog>
    // opened with showModal(), so it is in the browser's top layer and paints above anything in the normal
    // layer whatever its z-index. The list opened underneath the dialog, with only the part hanging past the
    // dialog's bottom edge visible.
    open({ location: location('LIVE_DOC') });
    await loaded();

    const root = shadow()!;
    const dialog = root.querySelector<HTMLElement>('dialog.rsp-modal')!;

    expect(portalsIn(dialog).length).toBeGreaterThan(0);
    expect(portalsIn(root)).toEqual([]);
    // And the dialog must not clip them, or a list longer than the room below its trigger is cut off
    expect(getComputedStyle(dialog).overflow).toBe('visible');
  });

  it('adopts the option list of a dropdown that appears later', async () => {
    // The form grows dropdowns as it goes: switching the work item roles on mounts two SearchableSelects,
    // each creating its option list right then - which is why this is an observer and not a one-off pass.
    open({ location: location('LIVE_DOC') });
    await loaded();
    const root = shadow()!;
    const dialog = root.querySelector<HTMLElement>('dialog.rsp-modal')!;
    const before = portalsIn(dialog).length;

    root.querySelector<HTMLInputElement>('#popup-selected-roles')!.click();

    await vi.waitFor(() => expect(root.querySelector('#popup-roles-selector')).not.toBeNull());
    await vi.waitFor(() => expect(portalsIn(dialog).length).toBe(before + 2));
    expect(portalsIn(root)).toEqual([]);
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

  it('closes a previously opened dialog instead of stacking a second one on top of it', async () => {
    // A second click on a toolbar button while the first dialog is still open must not leave two
    // independently submittable dialogs behind - see the greptile review on PR #991.
    const firstRoot = open({ location: location('LIVE_DOC') });
    await loaded();
    const firstHost = document.body.lastElementChild!;

    open({ location: location('LIVE_DOC') });
    await loaded();

    expect(firstHost.isConnected).toBe(false);
    expect([...document.querySelectorAll('body > div')].filter((element) => element.shadowRoot)).toHaveLength(1);
    // The first root was already unmounted by the second open() call
    roots.splice(roots.indexOf(firstRoot), 1);
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
