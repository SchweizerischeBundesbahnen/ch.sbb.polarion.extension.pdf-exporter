import { afterEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { mountInto, readShim } from '../src/widget/main';
import {
  SAMPLE_ITEMS,
  SAMPLE_ITEMS_EMPTY,
  SAMPLE_ITEMS_TRUNCATED,
  SAMPLE_ITEMS_WITH_UNREADABLE,
  SAMPLE_SHIM,
} from '../src/widget/sampleData';
import type { BulkExportItems } from '../src/widget/types';
import { SAMPLE_STYLE_PACKAGE_FULL, popupDependencies } from './exportPopupSamples';

// Docker-only snapshots of the widget as a report page shows it, mounted the way the renderer mounts
// it: its own shadow root, carrying the tokens and the widget's stylesheet. Polarion's page CSS is not
// part of this app and is not loaded here, so these references show the widget's own styling - which is
// exactly what a change in this repo can move.

const hosts: HTMLElement[] = [];

function mounted(items: BulkExportItems, deps: Partial<Parameters<typeof mountInto>[2]> = {}): HTMLElement {
  const host = document.createElement('div');
  host.id = `widget-visual-${hosts.length}`;
  host.className = 'polarion-PdfExporter-BulkExportWidget sbb-ui';
  host.setAttribute('data-title', 'Test Runs');
  host.setAttribute('data-document-type', 'TEST_RUN');
  host.setAttribute('data-descriptor', SAMPLE_SHIM.descriptor);
  host.setAttribute('data-signature', SAMPLE_SHIM.signature);
  document.body.appendChild(host);
  hosts.push(host);
  mountInto(host, readShim(host), { loadItems: () => Promise.resolve(items), ...deps });
  return host;
}

async function snapshot(host: HTMLElement, name: string): Promise<void> {
  // Park the pointer on the heading first. It carries no hover styling, while wherever the pointer
  // happened to rest after the previous test might - a link picking up its underline is enough to make
  // a reference disagree with itself from one run to the next.
  const heading = host.shadowRoot!.querySelector('h3');
  if (heading) {
    await userEvent.hover(heading);
  }
  await page.viewport(1280, Math.ceil(host.scrollHeight) + 40);
  await expect(page.elementLocator(host)).toMatchScreenshot(name);
}

const settled = (host: HTMLElement, selector = '.polarion-rpw-table-counts') =>
  vi.waitFor(() => expect(host.shadowRoot!.querySelector(selector)).not.toBeNull());

afterEach(() => {
  hosts.splice(0).forEach((host) => host.remove());
});

describe.skipIf(!__PIXEL_REFERENCES__)('Bulk PDF Export widget visual', () => {
  it('the table of a loaded widget, nothing selected', async () => {
    const host = mounted(SAMPLE_ITEMS);
    await settled(host);

    await snapshot(host, 'widget-loaded');
  });

  it('a selection, which enables the export button', async () => {
    const host = mounted(SAMPLE_ITEMS);
    await settled(host);

    host.shadowRoot!.querySelectorAll<HTMLInputElement>('input.export-item')[1].click();
    await vi.waitFor(() =>
      expect(host.shadowRoot!.querySelector('#bulk-export-pdf')!.className).not.toContain('defaultCursor'),
    );

    await snapshot(host, 'widget-selection');
  });

  it('a row the user may not read', async () => {
    const host = mounted(SAMPLE_ITEMS_WITH_UNREADABLE);
    await settled(host);

    await snapshot(host, 'widget-unreadable-row');
  });

  it('more items found than the widget shows', async () => {
    const host = mounted(SAMPLE_ITEMS_TRUNCATED);
    await settled(host);

    await snapshot(host, 'widget-truncated');
  });

  it('the query behind the info icon', async () => {
    const host = mounted(SAMPLE_ITEMS);
    await settled(host);

    host.shadowRoot!.querySelector<HTMLElement>('.polarion-rpw-table-show-query img')!.click();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('.polarion-rpw-table-query')).not.toBeNull());

    await snapshot(host, 'widget-query-shown');
  });

  it('a data set that found nothing', async () => {
    const host = mounted(SAMPLE_ITEMS_EMPTY);
    await settled(host);

    await snapshot(host, 'widget-empty');
  });

  /**
   * Snapshots a dialog of the widget's, which is a native <dialog> in the top layer of its shadow root - so
   * its host's box is empty and the dialog itself is what a reference can show. The viewport is a fixed one
   * taller than any of these dialogs: they cap their own height against it, which makes a viewport derived
   * from their height circular. Same value as the toolbar dialog's references.
   */
  async function dialogSnapshot(host: HTMLElement, name: string): Promise<void> {
    await userEvent.hover(host.shadowRoot!.querySelector('.rsp-modal-title')!);
    await page.viewport(900, 1800);
    await expect(page.elementLocator(host.shadowRoot!.querySelector<HTMLElement>('.rsp-modal')!)).toMatchScreenshot(
      name,
    );
  }

  /** Drives the widget to its export dialog, which is the one the toolbar buttons open as well. */
  async function openExportDialog(host: HTMLElement): Promise<void> {
    host.shadowRoot!.querySelectorAll<HTMLInputElement>('input.export-item')[0].click();
    await vi.waitFor(() =>
      expect(host.shadowRoot!.querySelector('#bulk-export-pdf')!.className).not.toContain('defaultCursor'),
    );
    host.shadowRoot!.querySelector<HTMLElement>('#bulk-export-pdf')!.click();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('.pdf-export-form')).not.toBeNull());
  }

  it('the export dialog, styled by the stylesheets of this shadow root', async () => {
    // The same dialog a toolbar button opens (see ExportPopup.visual.test.tsx); what this reference adds is
    // that it is styled inside the widget's root, where a second stylesheet is present.
    const host = mounted(SAMPLE_ITEMS, { popup: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }) });
    await settled(host);
    await openExportDialog(host);
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('#popup-style-package-content')).not.toBeNull());

    await dialogSnapshot(host, 'widget-export-dialog');
  });

  /**
   * Starts a bulk run over the whole selection and returns the host plus a hold on each conversion, so a
   * reference can be taken with the run going and again once it is over.
   */
  async function startRun(items = SAMPLE_ITEMS): Promise<{ host: HTMLElement; finish: (fail?: boolean) => void }> {
    const pending: { resolve: () => void; reject: (error: Error) => void }[] = [];
    const host = mounted(items, {
      popup: popupDependencies(),
      convert: () =>
        new Promise((resolve, reject) => {
          pending.push({ resolve: () => resolve({ blob: new Blob(['pdf']), fileName: null, warning: null }), reject });
        }),
      download: () => {},
      downloadAttachments: () => Promise.resolve(),
    });
    await settled(host);

    host.shadowRoot!.querySelectorAll<HTMLInputElement>('input.export-item').forEach((box) => box.click());
    await vi.waitFor(() =>
      expect(host.shadowRoot!.querySelector('#bulk-export-pdf')!.className).not.toContain('defaultCursor'),
    );
    host.shadowRoot!.querySelector<HTMLElement>('#bulk-export-pdf')!.click();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('.pdf-export-form')).not.toBeNull());
    host.shadowRoot!.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--primary')!.click();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('.bulk-export-progress')).not.toBeNull());

    const finish = (fail = false) => {
      const next = pending.shift();
      if (fail) next?.reject(new Error('Conversion failed: the document has no content'));
      else next?.resolve();
    };
    return { host, finish };
  }

  it('a bulk export in progress', async () => {
    const { host } = await startRun();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('.export-item.in-progress')).not.toBeNull());

    await dialogSnapshot(host, 'widget-progress-in-progress');
  });

  it('a bulk export over more items than the dialog can show at once', async () => {
    // The bar reporting how far along the run is stays put at the bottom of the list, where the legacy
    // dialog had it in the footer next to Stop. It used to be the last thing in the body, so a run over two
    // dozen items scrolled it out of sight - leaving the user watching a list of clocks with no progress
    // reported anywhere.
    const many = {
      ...SAMPLE_ITEMS,
      items: Array.from({ length: 24 }, (_unused, index) => ({
        ...SAMPLE_ITEMS.items[index % SAMPLE_ITEMS.items.length],
        id: `Report ${index + 1}`,
      })),
      totalCount: 24,
    };
    const { host, finish } = await startRun(many);
    // Nine done, so the bar is past the quarter mark where it starts naming the count
    for (let index = 0; index < 9; index++) {
      finish();
      await vi.waitFor(() => expect(host.shadowRoot!.querySelectorAll('.export-item.finished').length).toBe(index + 1));
    }

    // Narrow the window first: 24 rows fit whole at the viewport the other references are taken at
    await page.viewport(900, 700);
    const root = host.shadowRoot!;
    const content = root.querySelector('.rsp-modal-content')!;
    // The list has more to show than it shows, which is the condition this reference is about
    expect(content.scrollHeight).toBeGreaterThan(content.clientHeight + 1);
    await userEvent.hover(root.querySelector('.rsp-modal-title')!);
    await expect(page.elementLocator(root.querySelector<HTMLElement>('.rsp-modal')!)).toMatchScreenshot(
      'widget-progress-long-run',
    );
  });

  it('a bulk export that finished with a failure among the items', async () => {
    const { host, finish } = await startRun();
    for (let index = 0; index < SAMPLE_ITEMS.items.length; index++) {
      finish(index === 1);
      await vi.waitFor(() =>
        expect(host.shadowRoot!.querySelectorAll('.export-item.finished, .export-item.error').length).toBe(index + 1),
      );
    }
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('.bulk-export-outcome .result')).not.toBeNull());

    await dialogSnapshot(host, 'widget-progress-finished-with-errors');
  });

  it('an endpoint that refused the descriptor', async () => {
    const host = document.createElement('div');
    host.id = 'widget-visual-error';
    host.className = 'polarion-PdfExporter-BulkExportWidget sbb-ui';
    host.setAttribute('data-title', 'Test Runs');
    document.body.appendChild(host);
    hosts.push(host);
    mountInto(host, readShim(host), {
      loadItems: () =>
        Promise.reject(
          new Error('The widget descriptor is missing or was not signed by this server. Reload the page.'),
        ),
    });

    await settled(host, '.widget-error');

    await snapshot(host, 'widget-error');
  });
});
