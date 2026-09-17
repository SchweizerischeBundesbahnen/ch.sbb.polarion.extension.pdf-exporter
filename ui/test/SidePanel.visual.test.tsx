import { afterEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { mountSidePanel } from '../src/sidepanel/mount';
import { installFetchMock } from './mockFetch';
import { pagePreview } from './pagePreviewSample';
import {
  SAMPLE_STYLE_PACKAGE,
  SAMPLE_STYLE_PACKAGE_FULL,
  SAMPLE_STYLE_PACKAGE_HIDDEN,
  sampleDependencies,
} from './sidePanelSamples';
import type { SampleOptions } from './sidePanelSamples';
import { clearToasts } from './toasts';
import { settleBeforeCapture, settleLayout } from './visualHelpers';

// Docker-only snapshots of the export panel as the document editor shows it, mounted the way the
// form-extension fragment mounts it: its own shadow root, carrying react-sbb-polarion's stylesheet, the
// base font rule and the panel's own CSS. Polarion's page CSS is not part of this app and is not loaded
// here, so these references show the panel's own styling - which is exactly what a change in this repo can
// move.

/** The width of Polarion's Document Properties pane, so the rows wrap where they really wrap. */
const PANE_WIDTH = 360;

const hosts: HTMLElement[] = [];

function mounted(options: SampleOptions = {}): HTMLElement {
  const host = document.createElement('div');
  host.id = `side-panel-visual-${hosts.length}`;
  host.className = 'pdf-exporter form-wrapper';
  host.style.width = `${PANE_WIDTH}px`;
  document.body.appendChild(host);
  hosts.push(host);
  mountSidePanel(`#${host.id}`, sampleDependencies(options));
  return host;
}

const settled = (host: HTMLElement, selector = '#filename') =>
  vi.waitFor(() => expect(host.shadowRoot!.querySelector(selector)).not.toBeNull());

/** Every dropdown painted and showing its selection, so a snapshot cannot catch a blank trigger. */
const dropdownsUpgraded = (host: HTMLElement) =>
  vi.waitFor(() => {
    const root = host.shadowRoot!;
    expect(root.querySelectorAll('.searchable-dropdown').length).toBe(root.querySelectorAll('select').length);
    const triggers = Array.from(root.querySelectorAll<HTMLInputElement>('input.sd-trigger'));
    expect(triggers.every((trigger) => trigger.value !== '')).toBe(true);
    const multi = Array.from(root.querySelectorAll('.sd-trigger-multi'));
    expect(multi.every((trigger) => trigger.querySelector('.sd-chip, .sd-placeholder') !== null)).toBe(true);
  });

/**
 * Snapshots the toast the panel reported through, which is the one thing that is NOT in a capture of the
 * panel: a toast is `position: fixed` at the top of the window, and the panel is a 360px pane in it.
 *
 * The `<li>` and not sonner's `<ol>`: the list is a fixed box of no height, its toasts absolutely positioned
 * inside it, so an element capture of the list would be empty.
 */
async function snapshotToast(host: HTMLElement, name: string): Promise<void> {
  const toast = await vi.waitFor(() => {
    const found = host.shadowRoot!.querySelector<HTMLElement>('[data-sonner-toast]');
    expect(found).not.toBeNull();
    return found!;
  });
  await settleBeforeCapture(false);
  await expect(page.elementLocator(toast)).toMatchScreenshot(name);
}

async function snapshot(host: HTMLElement, name: string): Promise<void> {
  await dropdownsUpgraded(host);
  // Park the pointer somewhere without hover styling. Wherever it happened to rest after the previous test
  // might have some, which is enough to make a reference disagree with itself from one run to the next.
  await userEvent.hover(host.shadowRoot!.querySelector('p')!);
  await settleLayout();
  // Wider than the pane on purpose: a toast is 712px in the middle of the window, and in a narrow viewport
  // that lands on top of the 360px pane and into this capture. The pane's own rendering does not depend on
  // the window's width - it is the host's 360px that decides the layout - so widening it only moves the
  // toast out of frame. What a toast looks like is a reference of its own (see snapshotToast).
  await page.viewport(1600, Math.ceil(host.scrollHeight) + 40);
  await settleBeforeCapture(false);
  await expect(page.elementLocator(host)).toMatchScreenshot(name);
}

afterEach(() => {
  // Before the hosts go: a toast outlives its host (sonner keeps the queue), and the next host to mount is
  // handed everything still active - which would report one test's failure into the next one's reference.
  clearToasts();
  hosts.splice(0).forEach((host) => host.remove());
  vi.unstubAllGlobals();
});

/**
 * A panel whose validation came back with two pages that do not fit, which is the state both the gallery
 * and the preview dialog are photographed in.
 */
async function validated(): Promise<HTMLElement> {
  installFetchMock([
    {
      method: 'POST',
      match: /\/validate\?/,
      json: {
        invalidPages: [{ content: pagePreview() }, { content: pagePreview() }],
        suspiciousWorkItems: [],
      },
    },
  ]);
  const host = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE });
  await settled(host);

  host.shadowRoot!.querySelector<HTMLButtonElement>('#validate-pdf')!.click();
  await vi.waitFor(() => expect(host.shadowRoot!.querySelectorAll('.validate-result-img')).toHaveLength(2));
  return host;
}

/** Snapshots a dialog the panel opened: it is a native <dialog> in the top layer, outside the host's box. */
async function snapshotDialog(host: HTMLElement, name: string): Promise<void> {
  const dialog = await vi.waitFor(() => {
    const found = host.shadowRoot!.querySelector<HTMLElement>('.rsp-modal');
    expect(found).not.toBeNull();
    return found!;
  });
  await settleLayout();
  await page.viewport(1280, 900);
  await settleBeforeCapture(false);
  await expect(page.elementLocator(dialog)).toMatchScreenshot(name);
}

describe.skipIf(!__PIXEL_REFERENCES__)('side panel visual', () => {
  it('a style package that exposes its settings', async () => {
    const host = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE });
    await settled(host);

    await snapshot(host, 'panel-settings-exposed');
  });

  it('every optional setting switched on, which is the panel at its tallest', async () => {
    const host = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL, data: { webhooksEnabled: true } });
    await settled(host);

    await snapshot(host, 'panel-everything-on');
  });

  it('a style package that keeps its settings to itself', async () => {
    const host = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_HIDDEN });
    await settled(host);

    await snapshot(host, 'panel-settings-hidden');
  });

  it('a user who may not export', async () => {
    const host = mounted({ data: { exportPermission: 'denied' } });
    await settled(host);

    await snapshot(host, 'panel-export-not-allowed');
  });

  it('an export in progress, with the panel out of reach', async () => {
    // The sample conversion never completes, which is the in-progress state
    const host = mounted();
    await settled(host);
    await dropdownsUpgraded(host);

    host.shadowRoot!.querySelector<HTMLButtonElement>('#export-pdf')!.click();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('#filename')!.matches(':disabled')).toBe(true));

    await snapshot(host, 'panel-exporting');
  });

  it('a field the export was refused on', async () => {
    const host = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL });
    await settled(host);
    await dropdownsUpgraded(host);

    const chapters = host.shadowRoot!.querySelector<HTMLInputElement>('#chapters')!;
    await userEvent.fill(chapters, 'one, two');
    host.shadowRoot!.querySelector<HTMLButtonElement>('#export-pdf')!.click();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('[data-sonner-toast]')).not.toBeNull());

    await snapshot(host, 'panel-invalid-field');
    // The reason is a toast, which is at the top of the window rather than inside the pane - so the
    // reference above shows the marked field and this one shows what was said about it, in the same shape
    // the dialog reports it in (test/expected/ExportPopup/popup-export-refused.png).
    await snapshotToast(host, 'panel-export-refused');
  });

  it('a page width preview opened, which is a dialog of its own', async () => {
    // The one state of the panel that is not in the pane: a preview of a page that came out too wide, in
    // the shared Modal - a title saying which page it is, the header's close button, and no footer, since
    // there is nothing to confirm about a preview.
    const host = await validated();

    const preview = host.shadowRoot!.querySelectorAll<HTMLElement>('.validate-result-img')[1];
    preview.click();

    await snapshotDialog(host, 'panel-page-preview');
  });

  // Last on purpose, and a test of its own rather than a second capture inside the one above. `snapshot`
  // sizes the viewport from the host's height, so taking one before `panel-page-preview` resizes the
  // window and re-rasterizes that dialog's title - a reference that changes for no reason anyone reviewing
  // it could explain. Nothing runs after this, so its own resize reaches no other reference.
  it('the thumbnails a failed validation leaves in the pane, the first of them focused', async () => {
    // The gallery had no reference at all: the test above renders it, then photographs the dialog it opens.
    // Its thumbnails are flex items sized against the pane (`flex: 0 1 calc(25% - 8px)` with a min and a
    // max, see export-form.css) - which is why the keyboard fix kept them <img role="button"> instead of
    // wrapping each in a <button>, a wrapper becoming the flex item in its place. Nothing pinned that, so
    // the re-layout would not have shown up anywhere.
    //
    // Captured with the first one focused, so the ring a keyboard user navigates by is pinned too. It is
    // drawn by the user agent: nothing in this app styles an outline, and the one RSP rule that clears one
    // off a [role="button"] is scoped to `.markdown-body`, which this form is not.
    const host = await validated();

    // Tabbed into rather than focus()ed. The ring comes from `:focus-visible`, which follows the modality
    // the focus arrived by, and a programmatic focus does not count as the keyboard. The validate button is
    // the previous tab stop - the spinner and the alert between it and the gallery take no focus.
    host.shadowRoot!.querySelector<HTMLButtonElement>('#validate-pdf')!.focus();
    await userEvent.keyboard('{Tab}');
    // Asserted, so a DOM change that moves the tab stop fails here rather than quietly photographing an
    // unfocused gallery. Inside a shadow root it is the root's activeElement, not the document's.
    expect(host.shadowRoot!.activeElement).toBe(host.shadowRoot!.querySelectorAll('.validate-result-img')[0]);

    await snapshot(host, 'panel-validation-results');
  });

  // An open dropdown is deliberately not snapshotted here: its popup is a portal appended to the shadow
  // root and positioned outside the host's box, which an element screenshot clips. react-sbb-polarion has
  // its own visual references for the control.
});
