import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import type { DocumentType, ExportType } from '../src/export/documentType';
import type { ExportParamsJson } from '../src/export/exportParams';
import ExportPopupModal from '../src/popup/ExportPopupModal';
import type { ExportPopupDependencies } from '../src/popup/ExportPopupModal';
import type { DocumentIdentity } from '../src/services/exportContext';
import {
  SAMPLE_DOCUMENT,
  SAMPLE_POPUP_DATA,
  SAMPLE_STYLE_PACKAGE,
  SAMPLE_STYLE_PACKAGE_FULL,
  SAMPLE_STYLE_PACKAGE_HIDDEN,
  SAMPLE_TEST_RUN,
  pdfResult,
  popupDependencies,
} from './exportPopupSamples';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';
import { clearToasts, toastText, toasted, untoasted } from './toasts';

// The "Export to PDF" dialog the toolbar buttons open: which rows the item being exported puts on screen,
// what the export sends, what the user is told when something is wrong, and how a bulk export hands its
// parameters over instead of converting.
//
// The dialog is rendered directly rather than through `openExportPopup`, so the assertions read the document
// rather than a shadow root; the mounting itself is covered by ExportPopupMount.test.tsx. Its REST data and
// its conversion are replaced (see exportPopupSamples): a browser test has neither a Polarion to read from
// nor a page to be on.

interface OpenOptions {
  document?: DocumentIdentity;
  exportType?: ExportType;
  onBulkExport?: (params: ExportParamsJson) => void;
  onClose?: () => void;
  deps?: ExportPopupDependencies;
}

const open = (options: OpenOptions = {}) =>
  render(
    <ExportPopupModal
      document={options.document ?? SAMPLE_DOCUMENT}
      exportType={options.exportType}
      identifiers={options.exportType === 'BULK' ? [{ projectId: 'elibrary', documentName: 'One' }] : undefined}
      onBulkExport={options.onBulkExport}
      onClose={options.onClose ?? (() => {})}
      deps={options.deps ?? popupDependencies()}
    />,
  );

/** A document of the given type, at the same place as the sample document. */
const documentOfType = (documentType: DocumentType): DocumentIdentity => ({ ...SAMPLE_DOCUMENT, documentType });

const field = <T extends HTMLElement>(selector: string) => document.querySelector<T>(selector);
const checkbox = (id: string) => field<HTMLInputElement>(`#${id}`)!;
const text = (selector: string) => field(selector)?.textContent ?? '';
const selected = (id: string) => field<HTMLSelectElement>(`#${id}`)?.value;

const exportButton = () => field<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--primary')!;
const closeButton = () => field<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--secondary')!;

/** Waits for the dialog to have loaded its data and its style package. */
const settled = () => vi.waitFor(() => expect(field('#popup-style-package-select')).not.toBeNull());

/** Waits for the settings block, which only a package that exposes its settings puts on screen. */
const settledWithSettings = () => vi.waitFor(() => expect(field('#popup-style-package-content')).not.toBeNull());

/** Drives a SearchableSelect by the native select it wraps, which is its source of truth. */
const choose = (id: string, value: string) => {
  const select = field<HTMLSelectElement>(`#${id}`)!;
  select.value = value;
  select.dispatchEvent(new Event('change', { bubbles: true }));
};

beforeEach(() => {
  // The style package the dialog remembers is a cookie; each test starts without one.
  document.cookie = 'selected-style-package=; path=/; max-age=0';
});

afterEach(() => {
  cleanup();
  clearToasts();
  vi.unstubAllGlobals();
  document.cookie = 'selected-style-package=; path=/; max-age=0';
});

describe('what the style package puts on screen', () => {
  it('offers the settings a package exposes', async () => {
    open();
    await settledWithSettings();

    expect(text('#popup-style-package-content')).toContain('exposes its settings');
  });

  it('offers nothing but the name and the file name for a package that exposes none', async () => {
    open({ deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_HIDDEN }) });
    await settled();

    expect(field('#popup-style-package-content')).toBeNull();
    expect(field('#popup-filename')).not.toBeNull();
    expect(field('#popup-page-width-validation')).toBeNull();
  });

  it('preselects the first suitable style package, which the server ordered by weight', async () => {
    open();
    await settled();

    expect(selected('popup-style-package-select')).toBe(SAMPLE_POPUP_DATA.stylePackages[0].id);
  });

  it('offers the package the user picked last, and remembers a new pick', async () => {
    document.cookie = 'selected-style-package=Specification; path=/';
    open();
    await settled();

    expect(selected('popup-style-package-select')).toBe('Specification');

    choose('popup-style-package-select', 'Default');
    await vi.waitFor(() => expect(document.cookie).toContain('selected-style-package=Default'));
  });

  it('ignores a remembered package the selection no longer allows', async () => {
    document.cookie = 'selected-style-package=Gone; path=/';
    open();
    await settled();

    expect(selected('popup-style-package-select')).toBe('Default');
  });

  it('sets every control from the package it loaded', async () => {
    open({ deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }) });
    await settledWithSettings();

    expect(selected('popup-paper-size-selector')).toBe('A4');
    expect(selected('popup-pdf-variant-selector')).toBe('PDF_A_2B');
    expect(selected('popup-image-density-selector')).toBe('DPI_96');
    expect(checkbox('popup-fit-to-page').checked).toBe(true);
    expect(checkbox('popup-watermark').checked).toBe(true);
    expect(checkbox('popup-cut-empty-chapters').checked).toBe(true);
    expect(field<HTMLInputElement>('#popup-chapters')!.value).toBe('1,2');
    expect(field<HTMLInputElement>('#popup-work-items-query-input')!.value).toBe('type:requirement');
    expect(field<HTMLInputElement>('#popup-headers-color')!.value).toBe('#004d73');
  });

  it('reserves the space of a value field rather than removing it, as the legacy popup did', async () => {
    // `visibility` and not `display`: ticking a checkbox must not reflow the column around it.
    open();
    await settledWithSettings();

    const chapters = field<HTMLInputElement>('#popup-chapters')!;
    expect(getComputedStyle(chapters).visibility).toBe('hidden');

    await userEvent.click(checkbox('popup-specific-chapters'));
    expect(getComputedStyle(field('#popup-chapters')!).visibility).toBe('visible');
  });

  it('shows the comment options only while comments are rendered', async () => {
    open();
    await settledWithSettings();

    expect(field('#popup-render-comments-options')).toBeNull();
    await userEvent.click(checkbox('popup-render-comments'));

    expect(field('#popup-include-unreferenced-comments')).not.toBeNull();
    expect(field('#popup-render-native-comments')).not.toBeNull();
  });

  it('warns that sticky notes break PDF/A compliance as soon as they are asked for', async () => {
    open();
    await settledWithSettings();
    await userEvent.click(checkbox('popup-render-comments'));

    await userEvent.click(checkbox('popup-render-native-comments'));
    expect(await toasted('warning')).toContain('not compliant with any of PDF/A variants');

    // Taken back when the checkbox goes off again: it is the one report that belongs to a control
    await userEvent.click(checkbox('popup-render-native-comments'));
    await untoasted('warning');
  });

  it('hides the webhooks row where the installation has webhooks switched off', async () => {
    open({ deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }) });
    await settledWithSettings();

    expect(field('#popup-webhooks-checkbox')).toBeNull();
  });

  it('offers the webhooks row where the installation has them switched on', async () => {
    open({ deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL, data: { webhooksEnabled: true } }) });
    await settledWithSettings();

    expect(field('#popup-webhooks-checkbox')).not.toBeNull();
    expect(selected('popup-webhooks-selector')).toBe('Default');
  });

  it('hides the roles group where the project defines no link roles', async () => {
    open({ deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL, data: { roles: [] } }) });
    await settledWithSettings();

    expect(field('#popup-selected-roles')).toBeNull();
  });

  it('offers the roles and their direction once the roles are switched on', async () => {
    open({ deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }) });
    await settledWithSettings();

    expect(checkbox('popup-selected-roles').checked).toBe(true);
    expect(field('#popup-roles-selector')).not.toBeNull();
    expect(selected('popup-roles-direction-selector')).toBe('BOTH');
  });

  it('offers page width validation where the package exposes it', async () => {
    open();
    await settled();

    expect(field('#popup-validate-pdf')).not.toBeNull();
  });

  it('reloads every field when another style package is picked', async () => {
    const packages: Record<string, typeof SAMPLE_STYLE_PACKAGE> = {
      Default: SAMPLE_STYLE_PACKAGE,
      Specification: { ...SAMPLE_STYLE_PACKAGE, paperSize: 'A3', watermark: true },
    };
    open({
      deps: {
        ...popupDependencies(),
        loadPackage: (_send, name) => Promise.resolve(packages[name] ?? SAMPLE_STYLE_PACKAGE),
      },
    });
    await settledWithSettings();
    expect(selected('popup-paper-size-selector')).toBe('A4');

    choose('popup-style-package-select', 'Specification');

    await vi.waitFor(() => expect(selected('popup-paper-size-selector')).toBe('A3'));
    expect(checkbox('popup-watermark').checked).toBe(true);
  });
});

describe('which rows the item being exported puts on screen', () => {
  it('offers the document rows for a Live Document', async () => {
    open();
    await settledWithSettings();

    for (const id of [
      'popup-fit-to-page',
      'popup-render-comments',
      'popup-cut-empty-chapters',
      'popup-cut-empty-wi-attributes',
      'popup-mark-referenced-workitems',
      'popup-custom-list-styles',
      'popup-specific-chapters',
      'popup-metadata-fields',
      'popup-localization',
      'popup-selected-roles',
      'popup-work-items-query',
    ]) {
      expect(field(`#${id}`), id).not.toBeNull();
    }
    expect(field('#popup-download-attachments')).toBeNull();
  });

  it('offers the attachment rows for a test run, and none of the document-only ones', async () => {
    open({ document: SAMPLE_TEST_RUN, deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }) });
    await settledWithSettings();

    expect(field('#popup-download-attachments')).not.toBeNull();
    expect(field('#popup-attachments-filter')).not.toBeNull();
    expect(field('#popup-testcase-field-id')).not.toBeNull();
    // A test run still fits its images to the page
    expect(field('#popup-fit-to-page')).not.toBeNull();
    for (const id of [
      'popup-render-comments',
      'popup-cut-empty-chapters',
      'popup-metadata-fields',
      'popup-localization',
      'popup-selected-roles',
      'popup-work-items-query',
    ]) {
      expect(field(`#${id}`), id).toBeNull();
    }
  });

  it('hides the attachment fields until they are asked for', async () => {
    open({ document: SAMPLE_TEST_RUN });
    await settledWithSettings();

    expect(field('#popup-attachments-filter')).toBeNull();
    await userEvent.click(checkbox('popup-download-attachments'));
    expect(field('#popup-attachments-filter')).not.toBeNull();
    expect(field('#popup-embed-attachments')).not.toBeNull();
  });

  it('offers only the unconditional rows for a report', async () => {
    open({
      document: documentOfType('LIVE_REPORT'),
      deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }),
    });
    await settledWithSettings();

    expect(field('#popup-presentational-hints')).not.toBeNull();
    expect(field('#popup-watermark')).not.toBeNull();
    expect(field('#popup-cut-urls')).not.toBeNull();
    expect(field('#popup-full-fonts')).not.toBeNull();
    expect(field('#popup-fit-to-page')).toBeNull();
    expect(field('#popup-render-comments')).toBeNull();
  });

  it('offers the collection rows for a baseline collection', async () => {
    open({
      document: documentOfType('BASELINE_COLLECTION'),
      deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }),
    });
    await settledWithSettings();

    expect(field('#popup-render-comments')).not.toBeNull();
    expect(field('#popup-selected-roles')).not.toBeNull();
    // Shown for a collection, and then left out of the request - see documentType.test.ts
    expect(field('#popup-fit-to-page')).not.toBeNull();
    expect(field('#popup-metadata-fields')).toBeNull();
  });
});

describe('what a bulk export offers', () => {
  const bulk = (options: OpenOptions = {}) =>
    open({ ...options, exportType: 'BULK', document: options.document ?? documentOfType('LIVE_DOC') });

  it('offers the automatic style package and nothing else, by default', async () => {
    bulk();
    await vi.waitFor(() => expect(field('#popup-auto-select-style-package')).not.toBeNull());

    expect(checkbox('popup-auto-select-style-package').checked).toBe(true);
    expect(field('#popup-style-package-select')).toBeNull();
    expect(field('#popup-style-package-content')).toBeNull();
    // Each file is named after its own item, so there is no file name to offer
    expect(field('#popup-filename')).toBeNull();
    // A validation run would mean one per selected item
    expect(field('#popup-page-width-validation')).toBeNull();
  });

  it('offers the style package and its settings once the automatic pick is switched off', async () => {
    bulk();
    await vi.waitFor(() => expect(field('#popup-auto-select-style-package')).not.toBeNull());

    await userEvent.click(checkbox('popup-auto-select-style-package'));

    await vi.waitFor(() => expect(field('#popup-style-package-select')).not.toBeNull());
    await vi.waitFor(() => expect(field('#popup-style-package-content')).not.toBeNull());
  });

  it('does not offer the automatic pick for a type it does not apply to', async () => {
    bulk({ document: { ...SAMPLE_TEST_RUN, documentType: 'TEST_RUN' } });
    await settled();

    expect(field('#popup-auto-select-style-package')).toBeNull();
  });

  it('hands the parameters over instead of converting, and closes', async () => {
    const handed: ExportParamsJson[] = [];
    const closes: number[] = [];
    const convert = vi.fn(() => Promise.resolve(pdfResult()));
    bulk({
      onBulkExport: (params) => handed.push(params),
      onClose: () => closes.push(1),
      deps: popupDependencies({ convert }),
    });
    await vi.waitFor(() => expect(field('#popup-auto-select-style-package')).not.toBeNull());

    await userEvent.click(exportButton());

    expect(handed).toHaveLength(1);
    expect(handed[0].autoSelectStylePackage).toBe(true);
    expect(handed[0].documentType).toBe('LIVE_DOC');
    // A bulk export names each file after its own item
    expect(handed[0].fileName).toBeUndefined();
    expect(closes).toHaveLength(1);
    expect(convert).not.toHaveBeenCalled();
  });
});

describe('exporting', () => {
  it('sends what the form says and downloads the result under the file name shown', async () => {
    const requests: string[] = [];
    const downloads: string[] = [];
    open({
      deps: popupDependencies({
        stylePackage: SAMPLE_STYLE_PACKAGE_FULL,
        convert: (request) => {
          requests.push(request);
          return Promise.resolve(pdfResult());
        },
        download: (_blob, name) => downloads.push(name),
      }),
    });
    await settledWithSettings();

    await userEvent.click(exportButton());

    await vi.waitFor(() => expect(downloads).toEqual(['E-Library Cross Link Issue.pdf']));
    const sent = JSON.parse(requests[0]) as Record<string, unknown>;
    expect(sent.documentType).toBe('LIVE_DOC');
    expect(sent.paperSize).toBe('A4');
    expect(sent.chapters).toEqual(['1', '2']);
    expect(sent.fileName).toBe('E-Library Cross Link Issue.pdf');
    expect(await toasted('success')).toBe('PDF was successfully generated');
    // Every kind of message can be sent away by hand, a success included
    expect(field('[data-sonner-toast][data-type="success"] [data-close-button]')).not.toBeNull();
  });

  it('carries every switch and every typed value into the export', async () => {
    // One pass over the whole form: each control is driven the way a user drives it, and what the export
    // then sends is what says the control is wired to the request rather than only to the screen.
    const requests: string[] = [];
    open({
      deps: popupDependencies({
        convert: (request) => {
          requests.push(request);
          return Promise.resolve(pdfResult());
        },
      }),
    });
    await settledWithSettings();

    for (const id of [
      'popup-cover-page-checkbox',
      'popup-full-fonts',
      'popup-fit-to-page',
      'popup-presentational-hints',
      'popup-render-comments',
      'popup-watermark',
      'popup-cut-empty-chapters',
      'popup-cut-empty-wi-attributes',
      'popup-cut-urls',
      'popup-mark-referenced-workitems',
      'popup-custom-list-styles',
      'popup-specific-chapters',
      'popup-metadata-fields',
      'popup-work-items-query',
      'popup-localization',
      'popup-selected-roles',
    ]) {
      await userEvent.click(checkbox(id));
    }
    // The two comment options only exist once comments are rendered, which the sweep above switched on
    await userEvent.click(checkbox('popup-include-unreferenced-comments'));
    await userEvent.click(checkbox('popup-render-native-comments'));

    await userEvent.fill(field<HTMLInputElement>('#popup-chapters')!, '3');
    await userEvent.fill(field<HTMLInputElement>('#popup-metadata-fields-input')!, 'docOwner');
    await userEvent.fill(field<HTMLInputElement>('#popup-work-items-query-input')!, 'type:task');
    await userEvent.fill(field<HTMLInputElement>('#popup-numbered-list-styles')!, 'aI');
    await userEvent.fill(field<HTMLInputElement>('#popup-headers-color')!, '#ff0000');
    for (const [id, value] of [
      ['popup-paper-size-selector', 'A3'],
      ['popup-orientation-selector', 'LANDSCAPE'],
      ['popup-pdf-variant-selector', 'PDF_UA_2'],
      ['popup-image-density-selector', 'DPI_300'],
      ['popup-css-selector', 'SBB'],
      ['popup-header-footer-selector', 'SBB'],
      ['popup-localization-selector', 'SBB'],
      ['popup-cover-page-selector', 'SBB'],
      ['popup-render-comments-selector', 'ALL'],
      ['popup-language', 'it'],
      ['popup-roles-direction-selector', 'REVERSE'],
    ]) {
      choose(id, value);
    }
    const roles = field<HTMLSelectElement>('#popup-roles-selector')!;
    Array.from(roles.options).forEach((option) => (option.selected = option.value !== 'depends_on'));
    roles.dispatchEvent(new Event('change', { bubbles: true }));

    await userEvent.click(exportButton());

    await vi.waitFor(() => expect(requests).toHaveLength(1));
    const sent = JSON.parse(requests[0]) as Record<string, unknown>;
    expect(sent.coverPage).toBe('SBB');
    expect(sent.css).toBe('SBB');
    expect(sent.headerFooter).toBe('SBB');
    expect(sent.localization).toBe('SBB');
    expect(sent.headersColor).toBe('#ff0000');
    expect(sent.paperSize).toBe('A3');
    expect(sent.orientation).toBe('LANDSCAPE');
    expect(sent.pdfVariant).toBe('PDF_UA_2');
    expect(sent.imageDensity).toBe('DPI_300');
    expect(sent.fullFonts).toBe(true);
    // The package had these two on, so a click turns them off
    expect(sent.fitToPage).toBe(false);
    expect(sent.followHTMLPresentationalHints).toBe(false);
    expect(sent.renderComments).toBe('ALL');
    expect(sent.includeUnreferencedComments).toBe(true);
    expect(sent.renderNativeComments).toBe(true);
    expect(sent.watermark).toBe(true);
    expect(sent.cutEmptyChapters).toBe(true);
    expect(sent.cutEmptyWIAttributes).toBe(false);
    expect(sent.cutLocalUrls).toBe(true);
    expect(sent.markReferencedWorkitems).toBe(true);
    expect(sent.chapters).toEqual(['3']);
    expect(sent.metadataFields).toEqual(['docOwner']);
    expect(sent.numberedListStyles).toBe('aI');
    expect(sent.language).toBe('it');
    expect(sent.linkedWorkitemRoles).toEqual(['relates_to', 'verifies']);
    expect(sent.linkRoleDirection).toBe('REVERSE');
    expect(sent.urlQueryParameters).toEqual({ query: 'type:task' });
  });

  it("carries the package's language custom field, which the dialog shows nowhere", async () => {
    // The one field of a style package the export sends without ever putting it on screen: an administrator
    // names the LiveDoc field holding the document's language, and the server reads it during the export. So
    // the only thing that can say it survived the dialog is what the dialog sends.
    const requests: string[] = [];
    open({
      deps: popupDependencies({
        stylePackage: { ...SAMPLE_STYLE_PACKAGE_FULL, languageCustomField: 'docLanguage' },
        convert: (request) => {
          requests.push(request);
          return Promise.resolve(pdfResult());
        },
      }),
    });
    await settledWithSettings();

    await userEvent.click(exportButton());

    await vi.waitFor(() => expect(requests).toHaveLength(1));
    const sent = JSON.parse(requests[0]) as Record<string, unknown>;
    expect(sent.languageCustomField).toBe('docLanguage');
  });

  it('appends .pdf to a name the user typed without it', async () => {
    const downloads: string[] = [];
    open({
      deps: popupDependencies({
        convert: () => Promise.resolve(pdfResult()),
        download: (_blob, name) => downloads.push(name),
      }),
    });
    await settled();

    await userEvent.fill(field<HTMLInputElement>('#popup-filename')!, 'My Export');
    await userEvent.click(exportButton());

    await vi.waitFor(() => expect(downloads).toEqual(['My Export.pdf']));
  });

  it('falls back to the default name when the user cleared the field', async () => {
    const downloads: string[] = [];
    open({
      deps: popupDependencies({
        convert: () => Promise.resolve(pdfResult()),
        download: (_blob, name) => downloads.push(name),
      }),
    });
    await settled();

    await userEvent.clear(field<HTMLInputElement>('#popup-filename')!);
    await userEvent.click(exportButton());

    await vi.waitFor(() => expect(downloads).toEqual(['E-Library Cross Link Issue.pdf']));
  });

  it('shows the warning a conversion came back with, as text rather than markup', async () => {
    open({ deps: popupDependencies({ convert: () => Promise.resolve(pdfResult('One image\n\nwas not exported')) }) });
    await settled();

    await userEvent.click(exportButton());

    expect(await toasted('warning')).toBe('One image\n\nwas not exported');
  });

  it('shows the warning and the success of one export next to each other, not stacked', async () => {
    // Sonner stacks its toasts by default: the newest in front, the ones behind it scaled down with their
    // text hidden until the pointer is over them. A conversion that produced a file and had something to say
    // about it raises both, and the success would cover the warning - see components/ToastHost.tsx.
    open({ deps: popupDependencies({ convert: () => Promise.resolve(pdfResult('One image was not exported')) }) });
    await settled();

    await userEvent.click(exportButton());

    await toasted('warning');
    await toasted('success');
    const [warning, success] = (['warning', 'success'] as const).map((kind) =>
      field(`[data-sonner-toast][data-type="${kind}"]`)!.getBoundingClientRect(),
    );
    // One under the other, whichever way round: no overlap at all
    expect(Math.min(warning.bottom, success.bottom)).toBeLessThanOrEqual(Math.max(warning.top, success.top) + 1);
    expect(warning.height).toBeGreaterThan(0);
  });

  it('shows why a conversion failed', async () => {
    open({ deps: popupDependencies({ convert: () => Promise.reject(new Error('The document has no content')) }) });
    await settled();

    await userEvent.click(exportButton());

    expect(await toasted('error')).toBe('Error occurred during PDF generation: The document has no content');
  });

  it('says only that it failed when the server gave no reason', async () => {
    open({ deps: popupDependencies({ convert: () => Promise.reject(new Error('')) }) });
    await settled();

    await userEvent.click(exportButton());

    expect(await toasted('error')).toBe('Error occurred during PDF generation');
  });

  it('covers the form and disables the actions while an export runs', async () => {
    // The sample conversion never completes, which is the in-progress state
    open();
    await settled();

    await userEvent.click(exportButton());

    await vi.waitFor(() => expect(field('.in-progress-overlay.show')).not.toBeNull());
    expect(text('#in-progress-message')).toBe('Generating PDF');
    expect(exportButton().disabled).toBe(true);
    expect(field<HTMLButtonElement>('#popup-validate-pdf')!.disabled).toBe(true);
  });

  /** Records what would have been downloaded next to the PDF, instead of downloading it. */
  const withAttachmentSpy = (calls: unknown[]) =>
    popupDependencies({
      stylePackage: SAMPLE_STYLE_PACKAGE_FULL,
      convert: () => Promise.resolve(pdfResult()),
      downloadAttachments: (_remote, options) => {
        calls.push(options);
        return Promise.resolve();
      },
    });

  it("downloads a test run's attachments alongside the PDF", async () => {
    const attachmentCalls: unknown[] = [];
    open({ document: SAMPLE_TEST_RUN, deps: withAttachmentSpy(attachmentCalls) });
    await settledWithSettings();

    await userEvent.click(exportButton());

    await vi.waitFor(() => expect(attachmentCalls).toHaveLength(1));
    expect(attachmentCalls[0]).toMatchObject({
      projectId: 'elibrary',
      testRunId: 'build_quick-20170211-141155',
      filter: '*.*',
      testCaseFieldId: 'exportIt',
    });
  });

  it('leaves the attachments alone when they are embedded into the PDF', async () => {
    const attachmentCalls: unknown[] = [];
    open({ document: SAMPLE_TEST_RUN, deps: withAttachmentSpy(attachmentCalls) });
    await settledWithSettings();

    await userEvent.click(checkbox('popup-embed-attachments'));
    await userEvent.click(exportButton());

    await toasted('success');
    expect(attachmentCalls).toEqual([]);
  });

  it('refuses to export on a bad chapters entry, and marks the field', async () => {
    open({ deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }) });
    await settledWithSettings();

    await userEvent.fill(field<HTMLInputElement>('#popup-chapters')!, 'one, two');
    await userEvent.click(exportButton());

    expect(await toasted('error')).toContain('comma separated list of integer values');
    expect(field('#popup-chapters')!.className).toContain('error');
    // Nothing was started, so the dialog is still usable
    expect(exportButton().disabled).toBe(false);
  });

  it('refuses to export on a bad numbered list styles entry', async () => {
    open({ deps: popupDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }) });
    await settledWithSettings();

    await userEvent.fill(field<HTMLInputElement>('#popup-numbered-list-styles')!, 'xyz');
    await userEvent.click(exportButton());

    expect(await toasted('error')).toContain("combination of characters '1aAiI'");
    expect(field('#popup-numbered-list-styles')!.className).toContain('error');
  });

  it('closes when the user asks', async () => {
    const closes: number[] = [];
    open({ onClose: () => closes.push(1) });
    await settled();

    await userEvent.click(closeButton());

    expect(closes).toHaveLength(1);
  });
});

describe('what the dialog says when it cannot load', () => {
  it('reports the data it could not read, and refuses to export', async () => {
    open({ deps: popupDependencies({ loadError: new Error("No 'css' configurations in scope 'project/elibrary/'") }) });

    // A form that could not be loaded is a state, not an event, so it stays in the form rather than
    // becoming a toast that comes and goes.
    await vi.waitFor(() => expect(text('#popup-load-error')).toContain('Error occurred loading form data'));
    expect(text('#popup-load-error')).toContain("No 'css' configurations");
    expect(toastText('error')).toBe('');
    expect(exportButton().disabled).toBe(true);
    expect(field('.in-progress-overlay.show')).toBeNull();
  });

  it('reports a style package that cannot be read', async () => {
    open({ deps: { ...popupDependencies(), loadPackage: () => Promise.reject(new Error('HTTP 500')) } });

    await vi.waitFor(() =>
      expect(text('#popup-load-error')).toBe('Error occurred loading style package data: HTTP 500'),
    );
  });
});

describe('validating the page width', () => {
  /** A 1x1 PNG, for the one assertion that needs a preview with a size. */
  const PNG = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==';

  const validation = (routes: Route[]) => {
    installFetchMock(routes);
    open();
  };

  it('says so when every page fits', async () => {
    validation([{ method: 'POST', match: /\/validate\?/, json: { invalidPages: [], suspiciousWorkItems: [] } }]);
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#popup-validate-pdf')!);

    await vi.waitFor(() => expect(text('.validation-alerts .alert-success')).toBe('All pages are valid'));
  });

  it('previews the pages that do not fit, and links the work items behind them', async () => {
    validation([
      {
        method: 'POST',
        match: /\/validate\?/,
        json: {
          invalidPages: [{ content: PNG }, { content: PNG }],
          suspiciousWorkItems: [{ id: 'EL-1', link: '/polarion/#/project/elibrary/workitem?id=EL-1' }],
        },
      },
    ]);
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#popup-validate-pdf')!);

    await vi.waitFor(() => expect(text('.validation-alerts .alert-error')).toBe('2 invalid pages found:'));
    expect(document.querySelectorAll('#popup-page-previews img')).toHaveLength(2);
    const link = field<HTMLAnchorElement>('.suspicious-list a')!;
    expect(link.textContent).toBe('EL-1');
    expect(link.target).toBe('_blank');
  });

  it('shows only the first previews and says there are more', async () => {
    validation([
      {
        method: 'POST',
        match: /\/validate\?/,
        json: { invalidPages: Array.from({ length: 5 }, () => ({ content: PNG })), suspiciousWorkItems: [] },
      },
    ]);
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#popup-validate-pdf')!);

    await vi.waitFor(() =>
      expect(text('.validation-alerts .alert-error')).toBe('Invalid pages found. First 4 of them:'),
    );
    expect(document.querySelectorAll('#popup-page-previews img')).toHaveLength(4);
  });

  it('opens a preview in a dialog of its own, and closes it again', async () => {
    validation([
      {
        method: 'POST',
        match: /\/validate\?/,
        json: { invalidPages: [{ content: PNG }, { content: PNG }], suspiciousWorkItems: [] },
      },
    ]);
    await settled();
    await userEvent.click(field<HTMLButtonElement>('#popup-validate-pdf')!);
    await vi.waitFor(() => expect(document.querySelectorAll('#popup-page-previews img')).toHaveLength(2));

    // The second thumbnail, so the title has to say which page it is rather than always the first
    await userEvent.click(document.querySelectorAll<HTMLImageElement>('#popup-page-previews img')[1]);

    const opened = await vi.waitFor(() => {
      const found = field('#popup-page-preview-zoom');
      expect(found).not.toBeNull();
      return found!;
    });
    // Nested in the export dialog it was opened from, which is what puts it above that dialog
    expect(field('.pdf-export-form')!.contains(opened)).toBe(true);
    // `closest` and not `:has()`: the export dialog contains this one, so `:has()` matches them both
    const dialog = opened.closest('.rsp-modal')!;
    expect(dialog.querySelector('.rsp-modal-title')!.textContent).toBe('Invalid page 2 of 2');
    // A modal dialog nested in a modal dialog, which is what puts it in the top layer ABOVE the export
    // dialog rather than behind its backdrop
    expect(dialog.matches(':modal')).toBe(true);
    expect(field('.rsp-modal')!.matches(':modal')).toBe(true);
    // Nothing to confirm about a preview: the header's close button is the only one it offers
    expect(dialog.querySelector('.rsp-modal-close')).not.toBeNull();

    await userEvent.click(opened.querySelector<HTMLImageElement>('img')!);
    await vi.waitFor(() => expect(field('#popup-page-preview-zoom')).toBeNull());
    // The thumbnails are still there to open again
    expect(document.querySelectorAll('#popup-page-previews img')).toHaveLength(2);
  });

  it('reports a validation that could not be run at all', async () => {
    validation([{ method: 'POST', match: /\/validate\?/, json: { message: 'renderer unavailable' }, status: 503 }]);
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#popup-validate-pdf')!);

    expect(await toasted('error')).toBe('Error occurred validating pages width: renderer unavailable');
  });
});
