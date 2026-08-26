import type { Root } from 'react-dom/client';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import type { DocumentType, ExportType } from '../src/export/documentType';
import { openExportPopup } from '../src/popup/mount';
import type { DocumentIdentity } from '../src/services/exportContext';
import type { PopupSampleOptions } from './exportPopupSamples';
import {
  SAMPLE_DOCUMENT,
  SAMPLE_STYLE_PACKAGE,
  SAMPLE_STYLE_PACKAGE_FULL,
  SAMPLE_STYLE_PACKAGE_HIDDEN,
  SAMPLE_TEST_RUN,
  popupDependencies,
} from './exportPopupSamples';
import { parkPointer } from './visualHelpers';

// Docker-only snapshots of the "Export to PDF" dialog as a toolbar button opens it, mounted the way
// openExportPopup mounts it: its own shadow root, carrying react-sbb-polarion's stylesheet, the base font
// rule and the dialog's own CSS. Polarion's page CSS is not part of this app and is not loaded here, so
// these references show the dialog's own styling - which is exactly what a change in this repo can move.
//
// The dialog is snapshotted rather than the page: it is a native <dialog> in the top layer, so an element
// screenshot of its host would be empty.

const roots: Root[] = [];

interface MountOptions extends PopupSampleOptions {
  document?: DocumentIdentity;
  exportType?: ExportType;
}

function mounted(options: MountOptions = {}): ShadowRoot {
  const { document: location, exportType, ...sample } = options;
  const root = openExportPopup({
    location: location ?? SAMPLE_DOCUMENT,
    exportType,
    identifiers: exportType === 'BULK' ? [{ projectId: 'elibrary', documentName: 'One' }] : undefined,
    deps: popupDependencies(sample),
  });
  roots.push(root);
  return (document.body.lastElementChild as HTMLElement).shadowRoot!;
}

const settled = (shadow: ShadowRoot, selector = '#popup-style-package-select') =>
  vi.waitFor(() => expect(shadow.querySelector(selector)).not.toBeNull());

/** Every dropdown painted and showing its selection, so a snapshot cannot catch a blank trigger. */
const dropdownsUpgraded = (shadow: ShadowRoot) =>
  vi.waitFor(() => {
    expect(shadow.querySelectorAll('.searchable-dropdown').length).toBe(shadow.querySelectorAll('select').length);
    const triggers = Array.from(shadow.querySelectorAll<HTMLInputElement>('input.sd-trigger'));
    expect(triggers.every((trigger) => trigger.value !== '')).toBe(true);
    const multi = Array.from(shadow.querySelectorAll('.sd-trigger-multi'));
    expect(multi.every((trigger) => trigger.querySelector('.sd-chip, .sd-placeholder') !== null)).toBe(true);
  });

/**
 * One viewport for every reference, rather than one derived from the dialog's height.
 *
 * The dialog caps itself at a share of the viewport (RSP's Modal at 85vh, its content at 80vh here), so a
 * viewport measured from the dialog's own height is circular and clips the tallest form. A viewport taller
 * than any of these forms shows all of them whole, and one shared value keeps the references comparable.
 */
const VIEWPORT = { width: 900, height: 1800 } as const;

async function snapshot(shadow: ShadowRoot, name: string): Promise<void> {
  await dropdownsUpgraded(shadow);
  await snapshotDialog(shadow, name);
}

/** Snapshots the dialog itself: it is a native <dialog> in the top layer, so its host's box is empty. */
async function snapshotDialog(shadow: ShadowRoot, name: string): Promise<void> {
  await page.viewport(VIEWPORT.width, VIEWPORT.height);
  await parkPointer(shadow.querySelector('.rsp-modal-title')!);
  await expect(page.elementLocator(shadow.querySelector<HTMLElement>('.rsp-modal')!)).toMatchScreenshot(name);
}

afterEach(() => {
  roots.splice(0).forEach((root) => root.unmount());
  document.querySelectorAll('body > div').forEach((element) => {
    if (element.shadowRoot) element.remove();
  });
  document.cookie = 'selected-style-package=; path=/; max-age=0';
});

describe.skipIf(!__PIXEL_REFERENCES__)('export dialog visual', () => {
  it('a Live Document with a style package that exposes its settings', async () => {
    const shadow = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE });
    await settled(shadow);

    await snapshot(shadow, 'popup-live-doc');
  });

  it('every optional setting switched on, which is the dialog at its tallest', async () => {
    const shadow = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL, data: { webhooksEnabled: true } });
    await settled(shadow);

    await snapshot(shadow, 'popup-everything-on');
  });

  it('a style package that keeps its settings to itself', async () => {
    const shadow = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_HIDDEN });
    await settled(shadow);

    await snapshot(shadow, 'popup-settings-hidden');
  });

  it('a test run, which offers the attachment fields instead of the document ones', async () => {
    const shadow = mounted({ document: SAMPLE_TEST_RUN, stylePackage: SAMPLE_STYLE_PACKAGE_FULL });
    await settled(shadow);

    await snapshot(shadow, 'popup-test-run');
  });

  it('a report, which offers only the unconditional settings', async () => {
    const shadow = mounted({
      document: { ...SAMPLE_DOCUMENT, documentType: 'LIVE_REPORT' as DocumentType },
      stylePackage: SAMPLE_STYLE_PACKAGE_FULL,
    });
    await settled(shadow);

    await snapshot(shadow, 'popup-live-report');
  });

  it('a bulk export, which picks a style package per item', async () => {
    const shadow = mounted({ exportType: 'BULK' });
    await vi.waitFor(() => expect(shadow.querySelector('#popup-auto-select-style-package')).not.toBeNull());

    await snapshotDialog(shadow, 'popup-bulk');
  });

  it('an export in progress, with the form out of reach', async () => {
    // The sample conversion never completes, which is the in-progress state
    const shadow = mounted();
    await settled(shadow);
    await dropdownsUpgraded(shadow);

    shadow.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--primary')!.click();
    await vi.waitFor(() => expect(shadow.querySelector('.in-progress-overlay.show')).not.toBeNull());

    await snapshot(shadow, 'popup-exporting');
  });

  it('a field the export was refused on', async () => {
    const shadow = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL });
    await settled(shadow);
    await dropdownsUpgraded(shadow);

    await userEvent.fill(shadow.querySelector<HTMLInputElement>('#popup-chapters')!, 'one, two');
    shadow.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--primary')!.click();
    await vi.waitFor(() => expect(shadow.querySelector('.notifications .alert-error')).not.toBeNull());

    await snapshot(shadow, 'popup-invalid-field');
  });

  it('an open dropdown, which has to paint above the dialog', async () => {
    // The option list is a `position: fixed` portal the shared dropdown creates outside the React tree, and
    // the dialog is in the browser's top layer - so the list is moved into the dialog to be painted above it
    // at all (see popup/dialogPortals.ts). It is a child of the dialog, so this element screenshot shows it.
    const shadow = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE });
    await settled(shadow);
    await dropdownsUpgraded(shadow);

    const trigger = shadow
      .querySelector('#popup-pdf-variant-selector')!
      .closest('.property-wrapper')!
      .querySelector<HTMLInputElement>('input.sd-trigger')!;
    // The shared dropdown opens on mousedown, not on click
    trigger.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    await vi.waitFor(() => {
      const list = shadow.querySelector<HTMLElement>('.sd-portal[style*="block"] .options');
      expect(list).not.toBeNull();
      expect(list!.querySelectorAll('.option').length).toBeGreaterThan(0);
    });

    await snapshotDialog(shadow, 'popup-dropdown-open');
  });

  it('a short window, where the scrollbar must not push the second column away', async () => {
    // The one reference not taken at VIEWPORT, and the only one that reproduces the defect this dialog
    // actually shipped with: in a real Polarion the two columns wrapped into a single tall column.
    //
    // The window is a normal width and merely short. That is enough: the form goes over its height cap, the
    // content area scrolls, and the scrollbar takes about 15px off it - leaving 685px where two fixed 340px
    // columns and their 20px gap need 700. The columns are sized to shrink rather than wrap (see
    // .flex-column in export-popup.css), so they stay level here. Verified against the old rule: it wraps
    // at this viewport, so this reference catches it.
    //
    // The scrollbar is real, not simulated. It needs `ignoreDefaultArgs: ['--hide-scrollbars']` in
    // vitest.config.ts: Playwright passes that flag to headless Chromium by default, which is why this
    // whole class of defect was invisible to the suite while the dialog was broken in production.
    //
    // A dropdown is left open as well, so the reference also shows that the option list still paints above
    // the dialog and lands in the right place when the form behind it is scrollable.
    const shadow = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL, data: { webhooksEnabled: true } });
    await settled(shadow);
    await dropdownsUpgraded(shadow);
    await page.viewport(900, 520);

    const trigger = shadow
      .querySelector('#popup-orientation-selector')!
      .closest('.property-wrapper')!
      .querySelector<HTMLInputElement>('input.sd-trigger')!;
    trigger.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    await vi.waitFor(() => {
      const list = shadow.querySelector<HTMLElement>('.sd-portal[style*="block"] .options');
      expect(list).not.toBeNull();
      expect(list!.querySelectorAll('.option').length).toBeGreaterThan(0);
    });

    await parkPointer(shadow.querySelector('.rsp-modal-title')!);
    await expect(page.elementLocator(shadow.querySelector<HTMLElement>('.rsp-modal')!)).toMatchScreenshot(
      'popup-small-window',
    );
  });

  it('the data it could not read', async () => {
    const shadow = mounted({ loadError: new Error("No 'css' configurations in scope 'project/elibrary/'") });
    await vi.waitFor(() => expect(shadow.querySelector('.notifications .alert-error')).not.toBeNull());

    await snapshotDialog(shadow, 'popup-load-failed');
  });

  // An open dropdown is deliberately not snapshotted here: its popup is a portal appended to the shadow
  // root and positioned outside the dialog's box, which an element screenshot clips. react-sbb-polarion has
  // its own visual references for the control.
});
