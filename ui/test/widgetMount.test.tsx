import { afterEach, describe, expect, it, vi } from 'vitest';
import mount, { adoptPageStyles, mountInto, readShim } from '../src/widget/main';
import { SAMPLE_ITEMS, SAMPLE_SHIM } from '../src/widget/sampleData';

// How the widget gets onto a report page: BulkPdfExportWidgetRenderer emits a shim element carrying the
// signed descriptor and imports this module, which turns that element into a shadow root of its own.
// The page around it belongs to Polarion, so nothing may leak either way.

const hosts: HTMLElement[] = [];

function shim(attributes: Record<string, string> = {}): HTMLElement {
  const host = document.createElement('div');
  host.id = `widget-${hosts.length}`;
  host.className = 'polarion-PdfExporter-BulkExportWidget sbb-ui';
  Object.entries({
    'data-descriptor': SAMPLE_SHIM.descriptor,
    'data-signature': SAMPLE_SHIM.signature,
    'data-title': 'Test Runs',
    'data-document-type': 'TEST_RUN',
    'data-export-pages': 'false',
    ...attributes,
  }).forEach(([name, value]) => host.setAttribute(name, value));
  document.body.appendChild(host);
  hosts.push(host);
  return host;
}

const loaded = () => Promise.resolve(SAMPLE_ITEMS);

afterEach(() => {
  hosts.splice(0).forEach((host) => host.remove());
  document.head.querySelectorAll('link[data-test-stylesheet]').forEach((link) => link.remove());
});

describe('Bulk PDF Export widget mounting', () => {
  it('reads what the renderer put on the shim', () => {
    expect(readShim(shim())).toEqual({ ...SAMPLE_SHIM, exportPages: false });
    expect(readShim(shim({ 'data-export-pages': 'true' })).exportPages).toBe(true);
  });

  it('falls back to safe values on a shim without attributes', () => {
    const bare = document.createElement('div');

    expect(readShim(bare)).toEqual({
      descriptor: '',
      signature: '',
      title: '',
      documentType: 'LIVE_DOC',
      exportPages: false,
    });
  });

  it('renders the widget into a shadow root of the shim', async () => {
    const host = shim();

    mountInto(host, readShim(host), `#${host.id}`, { loadItems: loaded });

    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('h3')?.textContent).toBe('Test Runs'));
    // The page sees none of it: the widget's markup is behind the shadow boundary
    expect(document.querySelector('.polarion-rpw-table-content')).toBeNull();
    expect(host.shadowRoot!.querySelectorAll('.polarion-rpw-table-content-row').length).toBe(4);
  });

  it('gives the shadow root the styles the widget needs', async () => {
    const host = shim();

    mountInto(host, readShim(host), `#${host.id}`, { loadItems: loaded });

    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('h3')).not.toBeNull());
    const styles = Array.from(host.shadowRoot!.querySelectorAll('style')).map((style) => style.textContent ?? '');
    // The design tokens, which the widget's checkboxes are drawn from ...
    expect(styles.some((css) => css.includes('--sbb-checkbox-checked'))).toBe(true);
    // ... and the widget's own layout
    expect(styles.some((css) => css.includes('.export-items'))).toBe(true);
    // The container the tokens are declared on
    expect(host.shadowRoot!.querySelector('.sbb-ui')).not.toBeNull();
  });

  it('brings the page stylesheets into the shadow root, so the widget looks like the page it is on', () => {
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = '/polarion/ria/some-polarion-stylesheet.css';
    link.setAttribute('data-test-stylesheet', '');
    document.head.appendChild(link);
    const host = shim();
    const root = host.attachShadow({ mode: 'open' });

    adoptPageStyles(root);

    // The clone carries the resolved URL, which is what makes the stylesheet's own relative urls resolve
    const adopted = Array.from(root.querySelectorAll<HTMLLinkElement>('link[rel="stylesheet"]'));
    expect(adopted.map((clone) => clone.href)).toContain(
      `${window.location.origin}/polarion/ria/some-polarion-stylesheet.css`,
    );
  });

  it('outranks the page stylesheet where the two disagree', async () => {
    // Polarion's own rule for the query block is `display: none`, its widget toggling the element's
    // inline style. This widget renders the block only while it is shown, so the cloned page stylesheet
    // would keep it invisible on a report page - and nowhere else, the dev harness having no Polarion
    // stylesheet to clone. The widget's own sheet is injected after the clones for exactly this reason.
    const pageCss = document.createElement('link');
    pageCss.rel = 'stylesheet';
    pageCss.setAttribute('data-test-stylesheet', '');
    pageCss.href = `data:text/css,${encodeURIComponent('.polarion-rpw-table-query{display:none}')}`;
    document.head.appendChild(pageCss);

    const host = shim();
    mountInto(host, readShim(host), `#${host.id}`, { loadItems: loaded });
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('.polarion-rpw-table-show-query')).not.toBeNull());

    // The clone has to be in effect, or this would pass for the wrong reason
    const clone = Array.from(host.shadowRoot!.querySelectorAll('link')).find((link) =>
      link.href.startsWith('data:text/css'),
    )!;
    await vi.waitFor(() => expect(clone.sheet).not.toBeNull());

    host.shadowRoot!.querySelector<HTMLElement>('.polarion-rpw-table-show-query img')!.click();

    const query = await vi.waitFor(() => {
      const shown = host.shadowRoot!.querySelector('.polarion-rpw-table-query');
      expect(shown).not.toBeNull();
      return shown!;
    });
    expect(getComputedStyle(query).display).toBe('block');
  });

  it('mounts the shim the selector points at', async () => {
    const host = shim();

    mount(`#${host.id}`);

    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('h3')?.textContent).toBe('Test Runs'));
  });

  it('mounts a shim only once, however often the module is imported', async () => {
    const host = shim();
    mountInto(host, readShim(host), `#${host.id}`, { loadItems: loaded });
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('h3')).not.toBeNull());

    mountInto(host, readShim(host), `#${host.id}`, { loadItems: loaded });

    expect(host.shadowRoot!.querySelectorAll('h3').length).toBe(1);
  });

  it('says so instead of throwing when the shim is gone', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => mount('#no-such-widget')).not.toThrow();
    expect(error).toHaveBeenCalledWith(expect.stringContaining('#no-such-widget'));

    error.mockRestore();
  });
});
