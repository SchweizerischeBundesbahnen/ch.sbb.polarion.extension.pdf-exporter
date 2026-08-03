import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import BulkExportWidget from '../src/widget/BulkExportWidget';
import type {
  BulkCallback,
  ConversionResult,
  ExportContextLike,
  ExportContextOptions,
  ExportParamsLike,
} from '../src/widget/productModules';
import { SAMPLE_ITEMS, SAMPLE_SHIM } from '../src/widget/sampleData';
import type { BulkExportItem, BulkExportItems } from '../src/widget/types';

// A bulk export from the click on the button to the last download: the widget hands the selection to
// the export dialog, the dialog hands back the parameters the user chose, and the run converts one item
// at a time. The product's export JS is replaced here - a browser can only load it from a running
// Polarion - so what is asserted is exactly what the widget asks it to do.

const conversions: {
  request: Record<string, unknown>;
  resolve: (result: ConversionResult) => void;
  reject: (response: Response) => void;
}[] = [];
const downloads: { fileName: string }[] = [];
const attachmentCalls: unknown[][] = [];
const collectionCalls: { collectionId: string; resolve: () => void; reject: (response: Response) => void }[] = [];
let contextOptions: ExportContextOptions | null = null;

const context: ExportContextLike = {
  asyncConvertPdf: (request, onSuccess, onError) =>
    conversions.push({ request: request as Record<string, unknown>, resolve: onSuccess, reject: onError }),
  convertCollectionDocuments: (_params, collectionId, onComplete, onError) =>
    collectionCalls.push({ collectionId, resolve: onComplete, reject: onError }),
  downloadTestRunAttachments: (...args) => attachmentCalls.push(args),
  downloadBlob: (_blob, fileName) => downloads.push({ fileName }),
};

/** Stands in for the product's ExportParams: a mutable bag the run fills in per item. */
const exportParams = (over: Partial<ExportParamsLike> = {}): ExportParamsLike => {
  const params: Record<string, unknown> = { attachmentsFilter: null, embedAttachments: false, ...over };
  params.toJSON = () => ({ ...params, toJSON: undefined });
  return params as unknown as ExportParamsLike;
};

const failure = (message: string) => new Response(JSON.stringify({ message }), { status: 500 });

let openPopup: ((params: ExportParamsLike) => void) | null = null;
let docIdentifiers: BulkCallback['getDocIdentifiers'] | null = null;

const deps = {
  openExportPopup: (_documentType: string, callback: BulkCallback) => {
    openPopup = callback.openPopup;
    docIdentifiers = callback.getDocIdentifiers;
    return Promise.resolve();
  },
  createExportContext: (options: ExportContextOptions) => {
    contextOptions = options;
    return Promise.resolve(context);
  },
};

const rows = () => Array.from(document.querySelectorAll('.polarion-rpw-table-content-row'));
const checkboxes = () => Array.from(document.querySelectorAll<HTMLInputElement>('input.export-item'));
const progressRows = () => Array.from(document.querySelectorAll('.modal__content .export-item'));
const result = () => document.querySelector('.modal__footer .result')?.textContent;

/** Opens the widget, selects the given rows and walks through the export dialog. */
async function startExport(
  items: BulkExportItems,
  selection: number[],
  params = exportParams(),
  overrides: Partial<typeof deps> = {},
) {
  render(
    <BulkExportWidget
      shim={SAMPLE_SHIM}
      hostSelector="#host"
      deps={{ ...deps, ...overrides, loadItems: () => Promise.resolve(items) }}
    />,
  );
  await vi.waitFor(() => expect(rows().length).toBe(items.items.length));
  for (const index of selection) {
    await userEvent.click(checkboxes()[index]);
  }
  await userEvent.click(document.querySelector<HTMLElement>('#bulk-export-pdf')!);
  await vi.waitFor(() => expect(openPopup).not.toBeNull());
  openPopup!(params);
  await vi.waitFor(() => expect(document.querySelector('#bulk-pdf-export-modal-popup')).not.toBeNull());
}

const finishConversion = async (fileName?: string) => {
  await vi.waitFor(() => expect(conversions.length).toBeGreaterThan(0));
  conversions.shift()!.resolve({ response: new Blob(['pdf']), fileName });
};

const failConversion = async (message: string) => {
  await vi.waitFor(() => expect(conversions.length).toBeGreaterThan(0));
  conversions.shift()!.reject(failure(message));
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
  contextOptions = null;
  openPopup = null;
  docIdentifiers = null;
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

  it('binds the export context to the widget and its export-pages setting', async () => {
    await startExport(SAMPLE_ITEMS, [0]);

    await vi.waitFor(() => expect(contextOptions).not.toBeNull());
    expect(contextOptions).toEqual({ exportPages: false, rootComponentSelector: '#host' });
  });

  it('addresses a test run by id and fetches its attachments alongside', async () => {
    await startExport(SAMPLE_ITEMS, [0], exportParams({ attachmentsFilter: '.*', revision: '42' }));

    await vi.waitFor(() => expect(conversions.length).toBe(1));
    expect(conversions[0].request).toMatchObject({
      projectId: 'elibrary',
      documentType: 'TEST_RUN',
      urlQueryParameters: { id: 'build_quick-20170211-141155' },
    });
    expect(attachmentCalls[0].slice(0, 4)).toEqual(['elibrary', 'build_quick-20170211-141155', '42', '.*']);
  });

  it('leaves the attachments alone when they are embedded into the PDF', async () => {
    await startExport(SAMPLE_ITEMS, [0], exportParams({ attachmentsFilter: '.*', embedAttachments: true }));

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

  it('ends the run when the product export JS cannot be loaded', async () => {
    await startExport(SAMPLE_ITEMS, [0, 1], exportParams(), {
      createExportContext: () => Promise.reject(new Error('Failed to fetch dynamically imported module')),
    });

    await vi.waitFor(() => expect(result()).toBe('Export finished with errors'));
    // Nothing was ever converted, and no row is left waiting for a conversion that cannot come
    expect(conversions.length).toBe(0);
    expect(progressRows().every((row) => row.className.includes('error'))).toBe(true);
    expect(progressRows()[0].querySelector('.error-message')?.textContent).toBe('Could not start the export.');
  });

  it('stops the run when the user asks, leaving what was queued untouched', async () => {
    await startExport(SAMPLE_ITEMS, [0, 1, 2]);
    await vi.waitFor(() => expect(conversions.length).toBe(1));

    await userEvent.click(document.querySelector<HTMLElement>('#bulk-stop-export-pdf')!);

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

    await userEvent.click(document.querySelector<HTMLElement>('.modal__footer .polarion-JSWizardButton')!);

    expect(document.querySelector('#bulk-pdf-export-modal-popup')).toBeNull();
  });

  it('tells the export dialog which items were selected', async () => {
    await startExport(
      withItems([documentItem('Requirements', 'Specification'), documentItem('_default', 'Home')]),
      [1],
    );

    expect(docIdentifiers!()).toEqual([{ projectId: 'elibrary', spaceId: '_default', documentName: 'Home' }]);
  });
});
