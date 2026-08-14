import styleText from '@grigoriev/react-sbb-polarion/style.css?inline';

interface ShadowMountOptions {
  /** Classes for the inner container React mounts into (token scope + any wrapper classes the
   *  extension's own panel CSS expects, e.g. `pdf-exporter form-wrapper sbb-ui`). */
  containerClassName?: string;
  /** Extension panel CSS to inject as `<style>` INSIDE the shadow root (bundled via `?inline`). */
  styleTexts?: string[];
}

/**
 * Attaches an open shadow root to `host` and returns a fresh container element inside it for React to
 * mount into.
 *
 * Why a shadow root: the Polarion Document Properties pane is a single shared page where several
 * extensions render their own panels, each possibly built against a different react-sbb-polarion version.
 * Plain CSS is global by selector, so those panels would clash. A shadow root gives true, two-way
 * encapsulation: react-sbb-polarion's stylesheet (injected as a `<style>` INSIDE the shadow) styles only
 * this panel and cannot leak out, and the page's / other extensions' styles cannot leak in. The
 * SearchableDropdown popup portal is shadow-aware (it appends into this root via `getRootNode()`), so the
 * dropdown is styled and isolated too.
 *
 * `?inline` gives the built stylesheet as a string; extension-specific panel CSS is bundled the same way
 * and injected as additional `<style>` elements, so no runtime `<link>` to a Polarion-served file is
 * needed.
 */
export function mountInShadow(host: HTMLElement, options: ShadowMountOptions = {}): HTMLElement {
  const shadow = host.shadowRoot ?? host.attachShadow({ mode: 'open' });
  shadow.replaceChildren();

  const style = document.createElement('style');
  style.textContent = styleText;
  shadow.appendChild(style);

  // Apply the canonical SBB control font + base size (control-tokens.css `--sbb-control-font-family` /
  // `--sbb-control-font-size`, bundled in the stylesheet above and declared on `.sbb-ui`). Needed
  // because inside a shadow root both font-family and font-size inherit from the host - a bare <div>
  // in the editor's properties pane - so without this the text falls back to the browser defaults
  // (serif, 16px) instead of Polarion's `body { font-size: 13px }` + Segoe UI. Targeting `.sbb-ui`
  // covers both the container and the SearchableDropdown portal (both carry the class); tokens read from
  // the bundled CSS with literal fallbacks matching their values.
  const baseStyle = document.createElement('style');
  baseStyle.textContent =
    '.sbb-ui { font-family: var(--sbb-control-font-family, "Segoe UI", "Selawik", "Open Sans", Arial, sans-serif); font-size: var(--sbb-control-font-size, 13px); }';
  shadow.appendChild(baseStyle);

  for (const css of options.styleTexts ?? []) {
    const panel = document.createElement('style');
    panel.textContent = css;
    shadow.appendChild(panel);
  }

  const container = document.createElement('div');
  if (options.containerClassName) {
    container.className = options.containerClassName;
  }
  shadow.appendChild(container);
  return container;
}
