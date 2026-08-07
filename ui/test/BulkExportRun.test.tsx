import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import type { ConversionResult } from '../src/services/conversion';
import BulkExportWidget from '../src/widget/BulkExportWidget';
import type { WidgetDependencies } from '../src/widget/BulkExportWidget';
import { SAMPLE_ITEMS, SAMPLE_SHIM } from '../src/widget/sampleData';
import type { BulkExportItem, BulkExportItems } from '../src/widget/types';
import { SAMPLE_STYLE_PACKAGE_FULL, popupDependencies } from './exportPopupSamples';

// A bulk export from the click on the button to the last download: the widget opens its export dialog for the
// selection, the dialog hands back the parameters the user chose, and the run converts one item at a time.
//
// Both dialogs are the widget's own React now, so the whole path is exercised here - selecting rows, the real
// export dialog, its Export button, the progress dialog. Only the conversion is replaced: it needs a running
// Polarion. What is asserted is therefore exactly what the widget asks the conversion to do.

interface Conversion {
  request: Record<string, unknown>;
  resolve: (result: ConversionResult) => void;
  reject: (error: Error) => void;
}

const conversions: Conversion[] = [];
const downloads: { fileName: string }[] = [];
const attachmentCalls: Record<string, unknown>[] = [];
const collectionCalls: { collectionId: string; resolve: () => void; reject: (error: Error) => void }[] = [];

const pdf = (fileName: string | null = null): ConversionResult => ({
  blob: new Blob(['pdf']),
  fileName,
  warning: null,
});

/** Dependencies that hold every conversion open, so the run can be observed one item at a time. */
const deps = (items: BulkExportItems): WidgetDependencies => ({
  loadItems: () => Promise.resolve(items),
  popup: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }),
  convert: (_remote, request) =>
    new Promise<ConversionResult>((resolve, reject) => {
      conversions.push({ request: JSON.parse(request) as Record<string, unknown>, resolve, reject });
    }),
  convertCollection: (_remote, options) =>
    new Promise<void>((resolve, reject) => {
      collectionCalls.push({ collectionId: options.collectionId, resolve, reject });
    }),
  download: (_blob, fileName) => downloads.push({ fileName }),
  downloadAttachments: (_remote, options) => {
    attachmentCalls.push(options as unknown as Record<string, unknown>);
    return Promise.resolve();
  },
});

const rows = () => Array.from(document.querySelectorAll('.polarion-rpw-table-content-row'));
const checkboxes = () => Array.from(document.querySelectorAll<HTMLInputElement>('input.export-item'));
const progressDialog = () => document.querySelector('.bulk-export-progress');
const progressRows = () => Array.from(document.querySelectorAll('.bulk-export-progress .export-item'));
const result = () => document.querySelector('.bulk-export-outcome .result')?.textContent;
const primaryButton = () => document.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--primary')!;
const secondaryButton = () => document.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--secondary')!;

/**
 * Opens the widget, selects the given rows and walks through the export dialog to the run.
 *
 * `settings` are applied to the dialog's form before Export is pressed, which is how a test says what the
 * user chose. The dialog's own behavior is covered by ExportPopup.test.tsx.
 */
async function startExport(
  items: BulkExportItems,
  selection: number[],
  settings: () => Promise<void> = async () => {},
) {
  render(<BulkExportWidget shim={SAMPLE_SHIM} deps={deps(items)} />);
  await vi.waitFor(() => expect(rows().length).toBe(items.items.length));
  for (const index of selection) {
    await userEvent.click(checkboxes()[index]);
  }
  await userEvent.click(document.querySelector<HTMLElement>('#bulk-export-pdf')!);

  // The export dialog opens for the selection. Where the automatic style package pick applies - a bulk run
  // over documents or collections - it is on by default and hides the form, so it is switched off: what the
  // run then converts is what the form says. A bulk run over test runs is offered no such pick.
  await vi.waitFor(() =>
    expect(document.querySelector('#popup-auto-select-style-package, #popup-style-package-select')).not.toBeNull(),
  );
  const autoSelect = document.querySelector<HTMLInputElement>('#popup-auto-select-style-package');
  if (autoSelect?.checked) {
    await userEvent.click(autoSelect);
  }
  await vi.waitFor(() => expect(document.querySelector('#popup-style-package-content')).not.toBeNull());
  await settings();

  await userEvent.click(primaryButton());
  await vi.waitFor(() => expect(progressDialog()).not.toBeNull());
}

const finishConversion = async (fileName?: string) => {
  await vi.waitFor(() => expect(conversions.length).toBeGreaterThan(0));
  conversions.shift()!.resolve(pdf(fileName ?? null));
};

const failConversion = async (message: string) => {
  await vi.waitFor(() => expect(conversions.length).toBeGreaterThan(0));
  conversions.shift()!.reject(new Error(message));
};

const documentItem = (spaceId: string, id: string): BulkExportItem => ({
  readable: true,
  type: 'Module',
  projectId: 'elibrary',
  spaceId,
  id,
  cells: ['<span>doc</span>', '', '', '', ''],
});

const collectionItem = (id: string, name: string): BulkExportItem => ({
  readable: true,
  type: 'BaselineCollection',
  projectId: 'elibrary',
  id,
  name,
  cells: ['<span>collection</span>', '', '', '', ''],
});

const withItems = (items: BulkExportItem[]): BulkExportItems => ({ ...SAMPLE_ITEMS, items, totalCount: items.length });

afterEach(() => {
  cleanup();
  conversions.length = 0;
  downloads.length = 0;
  attachmentCalls.length = 0;
  collectionCalls.length = 0;
  document.cookie = 'selected-style-package=; path=/; max-age=0';
});

describe('Bulk export run', () => {
  it('converts the selected items one after another', async () => {
    await startExport(SAMPLE_ITEMS, [0, 1]);

    expect(progressRows().length).toBe(2);
    await vi.waitFor(() => expect(progressRows()[0].className).toContain('in-progress'));
    // The second item waits: one conversion at a time
    expect(progressRows()[1].className).toContain('paused');
    expect(conversions.length).toBe(1);

    await finishConversion('first.pdf');
    await vi.waitFor(() => expect(progressRows()[0].className).toContain('finished'));
    await vi.waitFor(() => expect(conversions.length).toBe(1));

    await finishConversion('second.pdf');
    await vi.waitFor(() => expect(result()).toBe('Export successfully finished'));
    expect(downloads.map((download) => download.fileName)).toEqual(['first.pdf', 'second.pdf']);
  });

  it('addresses a test run by id and fetches its attachments alongside', async () => {
    // The sample widget lists test runs, and the style package names an attachments filter
    await startExport(SAMPLE_ITEMS, [0]);

    await vi.waitFor(() => expect(conversions.length).toBe(1));
    expect(conversions[0].request).toMatchObject({
      projectId: 'elibrary',
      documentType: 'TEST_RUN',
      urlQueryParameters: { id: 'build_quick-20170211-141155' },
      attachmentsFilter: '*.*',
    });
    expect(attachmentCalls[0]).toMatchObject({
      projectId: 'elibrary',
      testRunId: 'build_quick-20170211-141155',
      filter: '*.*',
    });
  });

  it('leaves the attachments alone when they are embedded into the PDF', async () => {
    await startExport(SAMPLE_ITEMS, [0], async () => {
      await userEvent.click(document.querySelector<HTMLInputElement>('#popup-embed-attachments')!);
    });

    await vi.waitFor(() => expect(conversions.length).toBe(1));
    expect(attachmentCalls).toEqual([]);
  });

  it('addresses a document by its space and name, and falls back to that as file name', async () => {
    await startExport(withItems([documentItem('Requirements', 'Specification')]), [0]);

    await vi.waitFor(() => expect(conversions.length).toBe(1));
    expect(conversions[0].request).toMatchObject({
      documentType: 'LIVE_DOC',
      locationPath: 'Requirements/Specification',
    });

    await finishConversion();
    await vi.waitFor(() => expect(downloads.length).toBe(1));
    // No file name from the server: the item's own coordinates name the file
    expect(downloads[0].fileName).toBe('Requirements_Specification.pdf');
  });

  it('hands a collection to the collection conversion instead', async () => {
    await startExport(withItems([collectionItem('C1', 'Release 1.0')]), [0]);

    await vi.waitFor(() => expect(collectionCalls.length).toBe(1));
    expect(collectionCalls[0].collectionId).toBe('C1');
    expect(conversions.length).toBe(0);

    collectionCalls[0].resolve();
    await vi.waitFor(() => expect(result()).toBe('Export successfully finished'));
  });

  it('marks a failed item, says why, and carries on with the rest', async () => {
    await startExport(SAMPLE_ITEMS, [0, 1]);

    await failConversion('Document has no content');
    await vi.waitFor(() => expect(progressRows()[0].className).toContain('error'));
    expect(progressRows()[0].querySelector('.error-message')?.textContent).toBe('Document has no content');

    await finishConversion('second.pdf');
    await vi.waitFor(() => expect(result()).toBe('Export finished with errors'));
    // The failure did not cost the download of the item after it
    expect(downloads.map((download) => download.fileName)).toEqual(['second.pdf']);
  });

  it('says a failure happened even when the server gave no reason', async () => {
    await startExport(SAMPLE_ITEMS, [0]);

    await failConversion('');
    await vi.waitFor(() => expect(result()).toBe('Export finished with errors'));
    expect(progressRows()[0].querySelector('.error-message')?.textContent).toBe('Export failed');
  });

  it('stops the run when the user asks, leaving what was queued untouched', async () => {
    await startExport(SAMPLE_ITEMS, [0, 1, 2]);
    await vi.waitFor(() => expect(conversions.length).toBe(1));

    // While the run is going the dialog offers Stop, and only Stop
    await userEvent.click(primaryButton());

    expect(result()).toBe('Export interrupted by user');
    expect(progressRows()[1].className).toContain('interrupted');
    expect(progressRows()[2].className).toContain('interrupted');

    // The conversion already running cannot be recalled, but nothing new starts after it
    await finishConversion('first.pdf');
    await vi.waitFor(() => expect(downloads.length).toBe(1));
    expect(conversions.length).toBe(0);
    expect(result()).toBe('Export interrupted by user');
  });

  it('closes the dialog when the run is over', async () => {
    await startExport(SAMPLE_ITEMS, [0]);
    await finishConversion('first.pdf');
    await vi.waitFor(() => expect(result()).toBe('Export successfully finished'));

    // The run is over, so the dialog offers Close
    await userEvent.click(secondaryButton());

    await vi.waitFor(() => expect(progressDialog()).toBeNull());
  });

  it('offers the style packages that suit every selected item', async () => {
    const items = withItems([documentItem('Requirements', 'Specification'), documentItem('_default', 'Home')]);
    const requested: unknown[] = [];
    render(
      <BulkExportWidget
        shim={SAMPLE_SHIM}
        deps={{
          ...deps(items),
          popup: {
            ...popupDependencies(),
            loadData: (_send, request) => {
              requested.push(request.identifiers);
              return Promise.reject(new Error('enough'));
            },
          },
        }}
      />,
    );
    await vi.waitFor(() => expect(rows().length).toBe(2));
    await userEvent.click(checkboxes()[1]);
    await userEvent.click(document.querySelector<HTMLElement>('#bulk-export-pdf')!);

    await vi.waitFor(() => expect(requested.length).toBe(1));
    expect(requested[0]).toEqual([{ projectId: 'elibrary', spaceId: '_default', documentName: 'Home' }]);
  });

  // That an empty selection opens nothing is covered by BulkExportWidget.test.tsx, which reads the disabled
  // state of the button rather than clicking it - a click on an aria-disabled control is not something a
  // browser driver will do.

  it('lets the user close the export dialog without exporting', async () => {
    render(<BulkExportWidget shim={SAMPLE_SHIM} deps={deps(SAMPLE_ITEMS)} />);
    await vi.waitFor(() => expect(rows().length).toBe(SAMPLE_ITEMS.items.length));
    await userEvent.click(checkboxes()[0]);
    await userEvent.click(document.querySelector<HTMLElement>('#bulk-export-pdf')!);
    await vi.waitFor(() => expect(document.querySelector('#popup-style-package-select')).not.toBeNull());

    await userEvent.click(secondaryButton());

    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).toBeNull());
    expect(conversions.length).toBe(0);
  });
});
