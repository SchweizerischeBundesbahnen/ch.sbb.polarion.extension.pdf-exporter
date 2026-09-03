import { afterEach, describe, expect, it, vi } from 'vitest';
import { mountSidePanel } from '../src/sidepanel/mount';
import { installFetchMock } from './mockFetch';
import { sampleDependencies } from './sidePanelSamples';
import { clearToasts } from './toasts';

// How the panel gets into the document editor: PdfExporterFormExtension emits a fragment whose <link>
// onload imports this module and calls mountSidePanel on the host div. The pane is shared with every other
// extension's panel, so nothing may leak either way - which is what the shadow root is for.

const hosts: HTMLElement[] = [];

function host(): HTMLElement {
  const element = document.createElement('div');
  element.id = `pdf-exporter-panel-${hosts.length}`;
  element.className = 'pdf-exporter form-wrapper';
  document.body.appendChild(element);
  hosts.push(element);
  return element;
}

const mounted = (element: HTMLElement) => mountSidePanel(`#${element.id}`, sampleDependencies());

/** A 1x1 PNG, for the preview that has to have a size to be measured. */
const PNG = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==';

const loaded = (element: HTMLElement) =>
  vi.waitFor(() => expect(element.shadowRoot!.querySelector('#filename')).not.toBeNull());

afterEach(() => {
  // Before the hosts go: a toast outlives its host, and the next host to mount is handed everything of it
  // that is still active.
  clearToasts();
  hosts.splice(0).forEach((element) => element.remove());
  vi.unstubAllGlobals();
});

describe('mounting the side panel', () => {
  it('renders the panel into a shadow root of the fragment div', async () => {
    const element = host();

    mounted(element);
    await loaded(element);

    // The editor page sees none of it: the panel's markup is behind the shadow boundary
    expect(document.querySelector('#style-package-select')).toBeNull();
    expect(element.shadowRoot!.querySelector('#style-package-select')).not.toBeNull();
  });

  it('gives the shadow root the styles the panel needs', async () => {
    const element = host();

    mounted(element);
    await loaded(element);

    const styles = Array.from(element.shadowRoot!.querySelectorAll('style')).map((style) => style.textContent ?? '');
    // The design tokens and controls, which react-sbb-polarion's stylesheet brings ...
    expect(styles.some((css) => css.includes('--sbb-checkbox-checked'))).toBe(true);
    // ... the base font, which nothing inside a shadow root inherits from the page ...
    expect(styles.some((css) => css.includes('--sbb-control-font-family'))).toBe(true);
    // ... and the panel's own layout, which used to be a page stylesheet
    expect(styles.some((css) => css.includes('.property-wrapper'))).toBe(true);
  });

  it('carries the classes the panel CSS and the tokens are scoped to', async () => {
    const element = host();

    mounted(element);
    await loaded(element);

    const container = element.shadowRoot!.querySelector('div')!;
    expect(container.className).toBe('pdf-exporter form-wrapper sbb-ui');
  });

  it('styles the panel from inside the shadow root, the page having no rules for it', async () => {
    const element = host();
    // The width of Polarion's Document Properties pane, which is what the form lays itself out against.
    element.style.width = '360px';

    mounted(element);
    await loaded(element);

    // A rule that only this extension's stylesheets state, checked as computed style: it proves they are in
    // effect inside the root, not merely present as text.
    const row = element.shadowRoot!.querySelector('.property-wrapper')!;
    expect(getComputedStyle(row).display).toBe('grid');
    // The pane is 360px wide, which is one column of the shared form - a container query on the form, so
    // the panel gets there without a rule of its own.
    const section = element.shadowRoot!.querySelector('.pdf-section')!;
    expect(getComputedStyle(section).gridTemplateColumns.split(' ')).toHaveLength(1);
  });

  it('says it is loading with the same indicator the export popup uses', async () => {
    // The panel arrives empty and fills itself over REST, where the server-rendered one arrived populated.
    // A button-sized spinner alone reads as a stuck glyph in an empty pane, so this matches the popup's
    // in-progress overlay: 48px, centred in a column, with the message under it.
    const element = host();
    // A load that never resolves is the loading state.
    mountSidePanel(`#${element.id}`, { ...sampleDependencies(), loadData: () => new Promise(() => {}) });

    const message = await vi.waitFor(() => {
      const found = element.shadowRoot!.querySelector('.panel-loading-message');
      expect(found).not.toBeNull();
      return found!;
    });

    expect(message.textContent).toBe('Loading...');
    const spinner = element.shadowRoot!.querySelector('.panel-loading .sbb-spinner')!;
    const style = getComputedStyle(spinner);
    expect(style.width).toBe('48px');
    expect(style.height).toBe('48px');
    // Centred in a column, the message under the spinner rather than beside it
    const block = getComputedStyle(element.shadowRoot!.querySelector('.panel-loading')!);
    expect(block.display).toBe('flex');
    expect(block.flexDirection).toBe('column');
    expect(block.alignItems).toBe('center');
  });

  it('reports through a toast whose stylesheet is inside the root', async () => {
    // sonner injects its stylesheet into `document.head` when its module loads, and a shadow root sees
    // nothing of the document's rules - so the form's own stylesheet imports it (export/export-form.css).
    // Checked as computed style, because an unstyled toast host is not a broken layout, it is an invisible
    // message: the rules are all it has.
    const element = host();
    // A conversion that fails, which is the shortest way to a report
    mountSidePanel(`#${element.id}`, sampleDependencies({ convert: () => Promise.reject(new Error('no renderer')) }));
    await loaded(element);

    element.shadowRoot!.querySelector<HTMLButtonElement>('#export-pdf')!.click();

    const toaster = await vi.waitFor(() => {
      const found = element.shadowRoot!.querySelector<HTMLElement>('[data-sonner-toaster]');
      expect(found).not.toBeNull();
      return found!;
    });
    expect(getComputedStyle(toaster).position).toBe('fixed');
  });

  it('opens a page width preview against the window, not against the form it came from', async () => {
    // A zoomed preview asks for 90% of the height, centred, and it is rendered inside the form - which is a
    // query container (`container-type: inline-size`) in a 360px pane. A query container is NOT a
    // fixed-positioning containing block, so those percentages are the viewport's; but that reads as though
    // it should be the other way round, and a `transform`, a `filter` or a `contain: layout` on any ancestor
    // WOULD make it so. Hence measured rather than assumed.
    installFetchMock([
      {
        method: 'POST',
        match: /\/validate\?/,
        // A real image: one that does not decode has no size, and `width: auto` would have nothing to work
        // from.
        json: { invalidPages: [{ content: PNG }], suspiciousWorkItems: [] },
      },
    ]);
    const element = host();
    // The width of Polarion's Document Properties pane, which is the box this must NOT be measured against.
    element.style.width = '360px';
    mounted(element);
    await loaded(element);

    element.shadowRoot!.querySelector<HTMLButtonElement>('#validate-pdf')!.click();
    const preview = await vi.waitFor(() => {
      const found = element.shadowRoot!.querySelector<HTMLElement>('.validate-result-img');
      expect(found).not.toBeNull();
      return found!;
    });
    preview.click();

    const open = await vi.waitFor(() => {
      const found = element.shadowRoot!.querySelector<HTMLElement>('.img-zoomed');
      expect(found).not.toBeNull();
      return found!;
    });

    // The viewport a `position: fixed` box is measured against, which is the window less its scrollbars
    const viewport = document.documentElement;
    // 90% of its height, read off the computed style: the rectangle would carry the 3px border too
    expect(parseFloat(getComputedStyle(open).height)).toBeCloseTo(viewport.clientHeight * 0.9, 0);
    const box = open.getBoundingClientRect();
    expect(Math.round(box.top)).toBe(Math.round(viewport.clientHeight * 0.05));
    // And centred on the viewport, which a box measured against the pane could not be
    expect(Math.round(box.left + box.width / 2)).toBe(Math.round(viewport.clientWidth / 2));
  });

  it('marks a field the export was refused on, outranking the shared control styling', async () => {
    // The shared control system styles text inputs at a higher specificity than the legacy `.error` rule
    // had, which is why that rule never took effect. Asserted as computed style rather than as a class,
    // because the class is exactly what was there before and did nothing.
    const element = host();
    mounted(element);
    await loaded(element);
    const chapters = await vi.waitFor(() => {
      element.shadowRoot!.querySelector<HTMLInputElement>('#specific-chapters')!.click();
      const found = element.shadowRoot!.querySelector<HTMLInputElement>('#chapters');
      expect(found).not.toBeNull();
      return found!;
    });

    chapters.classList.add('error');

    expect(getComputedStyle(chapters).borderColor).toBe('rgb(255, 0, 0)');
    expect(getComputedStyle(chapters).color).toBe('rgb(255, 0, 0)');
  });

  it('re-mounts into the same host rather than stacking a second panel in it', async () => {
    const element = host();
    const first = mounted(element);
    await loaded(element);

    first?.unmount();
    mounted(element);
    await loaded(element);

    expect(element.shadowRoot!.querySelectorAll('#filename').length).toBe(1);
  });

  it('says so instead of throwing when the fragment div is gone', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(mountSidePanel('#no-such-panel')).toBeUndefined();
    expect(error).toHaveBeenCalledWith(expect.stringContaining('#no-such-panel'));

    error.mockRestore();
  });
});
