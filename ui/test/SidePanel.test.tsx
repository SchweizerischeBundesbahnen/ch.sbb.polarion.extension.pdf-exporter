import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import type { StylePackageSettings } from '../src/services/stylePackage';
import SidePanel from '../src/sidepanel/SidePanel';
import type { SidePanelDependencies } from '../src/sidepanel/SidePanel';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';
import {
  SAMPLE_PANEL_DATA,
  SAMPLE_STYLE_PACKAGE,
  SAMPLE_STYLE_PACKAGE_FULL,
  SAMPLE_STYLE_PACKAGE_HIDDEN,
  pdfResult,
  sampleDependencies,
} from './sidePanelSamples';
import { clearToasts, toastText, toasted, untoasted } from './toasts';

// The export panel of the document editor: what the selected style package puts on screen, what the export
// sends, and what the user is told when something is wrong. The panel is rendered directly rather than
// through `mountSidePanel` so the assertions read the document rather than a shadow root; the mounting
// itself is covered by SidePanelMount.test.tsx.
//
// The document location, the conversion and the REST data are replaced (see sidePanelSamples): a browser
// test has neither a Polarion to read from nor an editor URL to be in.

const open = (deps: SidePanelDependencies = sampleDependencies()) => render(<SidePanel deps={deps} />);

/** Waits for the panel to have loaded its data and its style package. */
const settled = () => vi.waitFor(() => expect(document.querySelector('#filename')).not.toBeNull());

const field = <T extends HTMLElement>(selector: string) => document.querySelector<T>(selector);
const checkbox = (id: string) => field<HTMLInputElement>(`#${id}`)!;
const text = (selector: string) => field(selector)?.textContent ?? '';

/** The panel's dropdowns are SearchableSelects; their value is the native select they wrap. */
const selected = (id: string) => field<HTMLSelectElement>(`#${id}`)?.value;

afterEach(() => {
  cleanup();
  clearToasts();
  vi.unstubAllGlobals();
});

describe('what the style package puts on screen', () => {
  it('offers the settings a package exposes', async () => {
    open();
    await settled();

    expect(field('#style-package-content')).not.toBeNull();
    expect(text('#style-package-content')).toContain('exposes its settings');
  });

  it('offers nothing but the name and the button for a package that exposes none', async () => {
    open(sampleDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_HIDDEN }));
    await settled();

    expect(field('#style-package-content')).toBeNull();
    expect(field('#filename')).not.toBeNull();
    expect(field('#export-pdf')).not.toBeNull();
  });

  it('preselects the first suitable style package, which the server ordered by weight', async () => {
    open();
    await settled();

    expect(selected('style-package-select')).toBe(SAMPLE_PANEL_DATA.stylePackages[0].id);
  });

  it('sets every control from the package it loaded', async () => {
    open(sampleDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }));
    await settled();

    expect(selected('paper-size-selector')).toBe('A4');
    expect(selected('pdf-variant-selector')).toBe('PDF_A_2B');
    expect(selected('image-density-selector')).toBe('DPI_96');
    expect(checkbox('fit-to-page').checked).toBe(true);
    expect(checkbox('watermark').checked).toBe(true);
    expect(checkbox('cut-empty-chapters').checked).toBe(true);
    expect(field<HTMLInputElement>('#chapters')!.value).toBe('1,2');
    expect(field<HTMLInputElement>('#work-items-query-input')!.value).toBe('type:requirement');
    expect(field<HTMLInputElement>('#headers-color')!.value).toBe('#004d73');
  });

  it('reserves the space of a value field rather than removing it, as the export dialog does', async () => {
    // `visibility` and not `display`: ticking a checkbox must not reflow the rows around it.
    open();
    await settled();

    const chapters = field<HTMLInputElement>('#chapters')!;
    expect(getComputedStyle(chapters).visibility).toBe('hidden');

    await userEvent.click(checkbox('specific-chapters'));
    expect(getComputedStyle(field('#chapters')!).visibility).toBe('visible');

    await userEvent.click(checkbox('specific-chapters'));
    expect(getComputedStyle(field('#chapters')!).visibility).toBe('hidden');
  });

  it('shows the comment options only while comments are rendered', async () => {
    open();
    await settled();

    expect(field('#render-comments-options')).toBeNull();
    await userEvent.click(checkbox('render-comments'));

    expect(field('#render-comments-selector')).not.toBeNull();
    expect(field('#include-unreferenced-comments')).not.toBeNull();
    expect(field('#render-native-comments')).not.toBeNull();
  });

  it('warns that sticky notes break PDF/A compliance as soon as they are asked for', async () => {
    open();
    await settled();
    await userEvent.click(checkbox('render-comments'));

    await userEvent.click(checkbox('render-native-comments'));
    expect(await toasted('warning')).toContain('not compliant with any of PDF/A variants');

    // Taken back when the checkbox goes off again: it is the one report that belongs to a control
    await userEvent.click(checkbox('render-native-comments'));
    await untoasted('warning');
  });

  it('hides the webhooks row where the installation has webhooks switched off', async () => {
    open(sampleDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }));
    await settled();

    expect(field('#webhooks-checkbox')).toBeNull();
  });

  it('offers the webhooks row where the installation has them switched on', async () => {
    open(sampleDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL, data: { webhooksEnabled: true } }));
    await settled();

    expect(field('#webhooks-checkbox')).not.toBeNull();
    expect(selected('webhooks-selector')).toBe('Default');
  });

  it('hides the roles group where the project defines no link roles', async () => {
    open(sampleDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL, data: { roles: [] } }));
    await settled();

    expect(field('#selected-roles')).toBeNull();
    expect(field('#roles-wrapper')).toBeNull();
  });

  it('offers the roles and their direction once the roles are switched on', async () => {
    open(sampleDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }));
    await settled();

    expect(checkbox('selected-roles').checked).toBe(true);
    expect(field('#roles-selector')).not.toBeNull();
    expect(selected('roles-direction-selector')).toBe('BOTH');
  });

  it('offers page width validation where the package exposes it', async () => {
    open();
    await settled();

    expect(field('#validate-pdf')).not.toBeNull();
  });

  it('hides page width validation where the package does not expose it', async () => {
    open(sampleDependencies({ stylePackage: { ...SAMPLE_STYLE_PACKAGE, exposePageWidthValidation: false } }));
    await settled();

    expect(field('#page-width-validation')).toBeNull();
  });

  it('carries every switch and every typed value into the export', async () => {
    // One pass over the whole form: each control is driven the way a user drives it, and what the export
    // then sends is what says the control is wired to the request rather than only to the screen.
    const requests: string[] = [];
    open(
      sampleDependencies({
        convert: (request) => {
          requests.push(request);
          return Promise.resolve(pdfResult());
        },
      }),
    );
    await settled();

    for (const id of [
      'cover-page-checkbox',
      'full-fonts',
      'fit-to-page',
      'presentational-hints',
      'watermark',
      'cut-empty-chapters',
      'cut-empty-wi-attributes',
      'cut-urls',
      'mark-referenced-workitems',
      'specific-chapters',
      'metadata-fields',
      'work-items-query',
      'custom-list-styles',
      'localization',
      'selected-roles',
    ]) {
      await userEvent.click(checkbox(id));
    }
    await userEvent.fill(field<HTMLInputElement>('#chapters')!, '3');
    await userEvent.fill(field<HTMLInputElement>('#metadata-fields-input')!, 'docOwner');
    await userEvent.fill(field<HTMLInputElement>('#work-items-query-input')!, 'type:task');
    await userEvent.fill(field<HTMLInputElement>('#numbered-list-styles')!, 'aI');

    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    const sent = JSON.parse(requests[0]) as Record<string, unknown>;
    expect(sent.coverPage).toBe('Default');
    expect(sent.fullFonts).toBe(true);
    // The package had these two on, so a click turns them off
    expect(sent.fitToPage).toBe(false);
    expect(sent.followHTMLPresentationalHints).toBe(false);
    expect(sent.watermark).toBe(true);
    expect(sent.cutEmptyChapters).toBe(true);
    expect(sent.cutEmptyWIAttributes).toBe(false);
    expect(sent.cutLocalUrls).toBe(true);
    expect(sent.markReferencedWorkitems).toBe(true);
    expect(sent.chapters).toEqual(['3']);
    expect(sent.metadataFields).toEqual(['docOwner']);
    expect(sent.numberedListStyles).toBe('aI');
    expect(sent.language).toBe('de');
    expect(sent.urlQueryParameters).toEqual({ query: 'type:task' });
    // Switched on with nothing picked yet: the roles group is offered, the request carries no role
    expect(sent.linkedWorkitemRoles).toEqual([]);
  });

  it('drives the dropdowns of the page setup into the export', async () => {
    const requests: string[] = [];
    open(
      sampleDependencies({
        convert: (request) => {
          requests.push(request);
          return Promise.resolve(pdfResult());
        },
      }),
    );
    await settled();

    // The SearchableSelect mirrors the native select it wraps, so driving the select is driving the control
    for (const [id, value] of [
      ['paper-size-selector', 'A3'],
      ['orientation-selector', 'LANDSCAPE'],
      ['pdf-variant-selector', 'PDF_UA_2'],
      ['image-density-selector', 'DPI_300'],
      ['css-selector', 'SBB'],
      ['header-footer-selector', 'SBB'],
      ['localization-selector', 'SBB'],
    ]) {
      const select = field<HTMLSelectElement>(`#${id}`)!;
      select.value = value;
      select.dispatchEvent(new Event('change', { bubbles: true }));
    }
    await userEvent.fill(field<HTMLInputElement>('#headers-color')!, '#ff0000');

    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    const sent = JSON.parse(requests[0]) as Record<string, unknown>;
    expect(sent.paperSize).toBe('A3');
    expect(sent.orientation).toBe('LANDSCAPE');
    expect(sent.pdfVariant).toBe('PDF_UA_2');
    expect(sent.imageDensity).toBe('DPI_300');
    expect(sent.css).toBe('SBB');
    expect(sent.headerFooter).toBe('SBB');
    expect(sent.localization).toBe('SBB');
    expect(sent.headersColor).toBe('#ff0000');
  });

  it('carries the comment options and the roles into the export', async () => {
    const requests: string[] = [];
    open(
      sampleDependencies({
        stylePackage: SAMPLE_STYLE_PACKAGE_FULL,
        convert: (request) => {
          requests.push(request);
          return Promise.resolve(pdfResult());
        },
      }),
    );
    await settled();

    const comments = field<HTMLSelectElement>('#render-comments-selector')!;
    comments.value = 'ALL';
    comments.dispatchEvent(new Event('change', { bubbles: true }));
    await userEvent.click(checkbox('render-native-comments'));
    await userEvent.click(checkbox('include-unreferenced-comments'));

    const roles = field<HTMLSelectElement>('#roles-selector')!;
    Array.from(roles.options).forEach((option) => (option.selected = option.value !== 'relates_to'));
    roles.dispatchEvent(new Event('change', { bubbles: true }));
    const direction = field<HTMLSelectElement>('#roles-direction-selector')!;
    direction.value = 'REVERSE';
    direction.dispatchEvent(new Event('change', { bubbles: true }));

    const language = field<HTMLSelectElement>('#language')!;
    language.value = 'it';
    language.dispatchEvent(new Event('change', { bubbles: true }));

    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    const sent = JSON.parse(requests[0]) as Record<string, unknown>;
    expect(sent.renderComments).toBe('ALL');
    expect(sent.renderNativeComments).toBe(true);
    expect(sent.includeUnreferencedComments).toBe(false);
    expect(sent.linkedWorkitemRoles).toEqual(['depends_on', 'verifies']);
    expect(sent.linkRoleDirection).toBe('REVERSE');
    expect(sent.language).toBe('it');
  });

  it('reloads every field when another style package is picked', async () => {
    const packages: Record<string, StylePackageSettings> = {
      Default: SAMPLE_STYLE_PACKAGE,
      Specification: { ...SAMPLE_STYLE_PACKAGE, paperSize: 'A3', watermark: true },
    };
    open({
      ...sampleDependencies(),
      loadPackage: (_send, name) => Promise.resolve(packages[name] ?? SAMPLE_STYLE_PACKAGE),
    });
    await settled();
    expect(selected('paper-size-selector')).toBe('A4');

    // The dropdown mirrors the native select it wraps, so driving the select is driving the control
    const select = field<HTMLSelectElement>('#style-package-select')!;
    select.value = 'Specification';
    select.dispatchEvent(new Event('change', { bubbles: true }));

    await vi.waitFor(() => expect(selected('paper-size-selector')).toBe('A3'));
    expect(checkbox('watermark').checked).toBe(true);
  });
});

describe('what the panel says when it cannot load', () => {
  // A form that could not be loaded is a state, not an event, so it stays in the panel rather than becoming
  // a toast that comes and goes - which is what the export dialog does with the same failure.
  it('reports a style package that cannot be read', async () => {
    open({ ...sampleDependencies(), loadPackage: () => Promise.reject(new Error('HTTP 500')) });

    await vi.waitFor(() => expect(text('#load-error')).toContain('error loading style package settings'));
    expect(toastText('error')).toBe('');
  });

  it('reports data that cannot be read', async () => {
    open({ ...sampleDependencies(), loadData: () => Promise.reject(new Error('HTTP 500')) });

    await vi.waitFor(() => expect(text('#load-error')).toContain('error loading style package settings'));
  });

  // There used to be a third failure here: the product's export JS, loaded at runtime from the other
  // webapp, being unavailable - which is what read the document out of the editor URL. That parsing is
  // `services/exportContext.ts` now, bundled with the panel, so the failure mode is gone rather than
  // untested; the parsing itself is covered by exportContext.test.ts.
});

describe('exporting', () => {
  it('sends what the form says and downloads the result under the file name shown', async () => {
    const requests: string[] = [];
    const downloads: string[] = [];
    open(
      sampleDependencies({
        stylePackage: SAMPLE_STYLE_PACKAGE_FULL,
        convert: (request) => {
          requests.push(request);
          return Promise.resolve(pdfResult());
        },
        download: (_blob, name) => downloads.push(name),
      }),
    );
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    expect(downloads).toEqual(['E-Library Cross Link Issue.pdf']);
    const sent = JSON.parse(requests[0]) as Record<string, unknown>;
    expect(sent.documentType).toBe('LIVE_DOC');
    expect(sent.paperSize).toBe('A4');
    expect(sent.chapters).toEqual(['1', '2']);
    expect(sent.fileName).toBe('E-Library Cross Link Issue.pdf');
  });

  it('appends .pdf to a name the user typed without it', async () => {
    const downloads: string[] = [];
    open(
      sampleDependencies({
        convert: () => Promise.resolve(pdfResult()),
        download: (_blob, name) => downloads.push(name),
      }),
    );
    await settled();

    const filename = field<HTMLInputElement>('#filename')!;
    await userEvent.fill(filename, 'My Export');
    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    expect(downloads).toEqual(['My Export.pdf']);
  });

  it('falls back to the default name when the user cleared the field', async () => {
    const downloads: string[] = [];
    open(
      sampleDependencies({
        convert: () => Promise.resolve(pdfResult()),
        download: (_blob, name) => downloads.push(name),
      }),
    );
    await settled();

    await userEvent.clear(field<HTMLInputElement>('#filename')!);
    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    expect(downloads).toEqual(['E-Library Cross Link Issue.pdf']);
  });

  it('shows the warning a conversion came back with, as text rather than markup', async () => {
    // The conversion builds a multi-part warning with blank lines between the parts (see
    // services/conversion.ts); the panel renders it as text, never as markup.
    open(sampleDependencies({ convert: () => Promise.resolve(pdfResult('One image\n\nwas not exported')) }));
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    expect(await toasted('warning')).toBe('One image\n\nwas not exported');
  });

  it('shows why a conversion failed', async () => {
    open(
      sampleDependencies({
        convert: () => Promise.reject(new Error('The document has no content')),
      }),
    );
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    expect(await toasted('error')).toBe('Error occurred during PDF generation: The document has no content');
  });

  it('says only that it failed when the server gave no reason', async () => {
    open(
      sampleDependencies({
        convert: () => Promise.reject(new Error('')),
      }),
    );
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    expect(await toasted('error')).toBe('Error occurred during PDF generation');
  });

  it('disables the panel and shows the spinner while an export runs', async () => {
    // The sample conversion never completes, which is the in-progress state
    open(sampleDependencies());
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    // The panel is one fieldset, so `:disabled` is what says a control is out of reach - an inherited
    // disabled state does not set the control's own `disabled` property.
    expect(field('#export-pdf')!.matches(':disabled')).toBe(true);
    expect(field('#filename')!.matches(':disabled')).toBe(true);
    expect(field('#specific-chapters')!.matches(':disabled')).toBe(true);
    expect(getComputedStyle(field('#export-pdf-progress')!).display).toBe('inline-block');
  });

  it('refuses to export on a bad chapters entry, and marks the field', async () => {
    open(sampleDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }));
    await settled();

    await userEvent.fill(field<HTMLInputElement>('#chapters')!, 'one, two');
    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    expect(await toasted('error')).toContain('comma separated list of integer values');
    expect(field('#chapters')!.className).toContain('error');
    // Nothing was started, so the panel is still usable
    expect(field<HTMLButtonElement>('#export-pdf')!.disabled).toBe(false);
  });

  it('refuses to export on a bad numbered list styles entry', async () => {
    open(sampleDependencies({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL }));
    await settled();

    await userEvent.fill(field<HTMLInputElement>('#numbered-list-styles')!, 'xyz');
    await userEvent.click(field<HTMLButtonElement>('#export-pdf')!);

    expect(await toasted('error')).toContain("combination of characters '1aAiI'");
    expect(field('#numbered-list-styles')!.className).toContain('error');
  });

  it('disables both actions, with the reason, for a user who may not export', async () => {
    open(sampleDependencies({ data: { exportPermission: 'denied' } }));
    await settled();

    const exportButton = field<HTMLButtonElement>('#export-pdf')!;
    expect(exportButton.disabled).toBe(true);
    expect(exportButton.title).toBe('You are not allowed to export PDF for this project');
    expect(field<HTMLButtonElement>('#validate-pdf')!.disabled).toBe(true);
  });

  it('disables both actions when the permission could not be read, without claiming a refusal', async () => {
    // Fail closed, the way the DLE toolbar's button does - but the panel does not know the user is
    // unauthorized, so it must not say so.
    open(sampleDependencies({ data: { exportPermission: 'unknown' } }));
    await settled();

    const exportButton = field<HTMLButtonElement>('#export-pdf')!;
    expect(exportButton.disabled).toBe(true);
    expect(field<HTMLButtonElement>('#validate-pdf')!.disabled).toBe(true);
    expect(exportButton.title).toBe('Could not check whether you are allowed to export. Please, reload the page.');
    expect(exportButton.title).not.toContain('not allowed');
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

    await userEvent.click(field<HTMLButtonElement>('#validate-pdf')!);

    await vi.waitFor(() => expect(text('#validate-ok')).toBe('All pages are valid'));
  });

  it('previews the pages that do not fit, and links the work items behind them', async () => {
    validation([
      {
        method: 'POST',
        match: /\/validate\?/,
        json: {
          invalidPages: [{ content: 'aaa' }, { content: 'bbb' }],
          suspiciousWorkItems: [{ id: 'EL-214', link: '/polarion/#/project/elibrary/workitem?id=EL-214' }],
        },
      },
    ]);
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#validate-pdf')!);

    await vi.waitFor(() => expect(text('#validate-error')).toContain('2 invalid pages found:'));
    expect(document.querySelectorAll('.validate-result-img').length).toBe(2);
    expect(text('.suspicious-list')).toContain('EL-214');
  });

  it('says "page" for a single one, as the legacy panel did', async () => {
    validation([
      {
        method: 'POST',
        match: /\/validate\?/,
        json: { invalidPages: [{ content: 'aaa' }], suspiciousWorkItems: [] },
      },
    ]);
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#validate-pdf')!);

    await vi.waitFor(() => expect(text('#validate-error')).toContain('1 invalid page found:'));
  });

  it('shows only the first four previews when there are more, and says so', async () => {
    validation([
      {
        method: 'POST',
        match: /\/validate\?/,
        json: {
          invalidPages: Array.from({ length: 5 }, (_unused, index) => ({ content: `page${index}` })),
          suspiciousWorkItems: [],
        },
      },
    ]);
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#validate-pdf')!);

    await vi.waitFor(() => expect(text('#validate-error')).toContain('Invalid pages found. First 4 of them:'));
    expect(document.querySelectorAll('.validate-result-img').length).toBe(4);
  });

  it('zooms a preview on click and back on a second click', async () => {
    validation([
      {
        method: 'POST',
        match: /\/validate\?/,
        // A real image, unlike elsewhere in this suite: a preview that does not decode has no size, and
        // an element of no size cannot be clicked.
        json: { invalidPages: [{ content: PNG }], suspiciousWorkItems: [] },
      },
    ]);
    await settled();
    await userEvent.click(field<HTMLButtonElement>('#validate-pdf')!);
    const preview = await vi.waitFor(() => {
      const found = document.querySelector<HTMLElement>('.validate-result-img');
      expect(found).not.toBeNull();
      return found!;
    });

    await userEvent.click(preview);
    expect(document.querySelector('.validate-result-img')!.className).toContain('img-zoomed');

    await userEvent.click(document.querySelector<HTMLElement>('.validate-result-img')!);
    expect(document.querySelector('.validate-result-img')!.className).not.toContain('img-zoomed');
  });

  it('shows why a validation failed, where a refused export is reported too', async () => {
    validation([{ method: 'POST', match: /\/validate\?/, status: 500, json: { message: 'Rendering failed' } }]);
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#validate-pdf')!);

    expect(await toasted('error')).toBe('Error occurred validating pages width: Rendering failed');
    // `#validate-error` says what a validation that *ran* found, so it stays empty here
    expect(text('#validate-error')).toBe('');
  });

  it('asks for one preview more than it shows, so it knows there are more', async () => {
    const fetchMock = installFetchMock([
      { method: 'POST', match: /\/validate\?/, json: { invalidPages: [], suspiciousWorkItems: [] } },
    ]);
    open();
    await settled();

    await userEvent.click(field<HTMLButtonElement>('#validate-pdf')!);

    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(String(fetchMock.mock.calls[0][0])).toContain('max-results=5');
  });
});
