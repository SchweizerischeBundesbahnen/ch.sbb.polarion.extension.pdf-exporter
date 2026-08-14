import { createRoot } from 'react-dom/client';
import type { Root } from 'react-dom/client';
import { mountInShadow } from '../services/shadowMount';
import SidePanel from './SidePanel';
import type { SidePanelDependencies } from './SidePanel';
import panelStyle from './side-panel.css?inline';

/**
 * Entry point for the Document Properties side panel, built by Vite into a fixed-name module
 * (`assets/side-panel.js`; the Vite input key `side-panel` sets the output name). The server-rendered
 * form-extension fragment (webapp/pdf-exporter/html/sidePanelContent.html) dynamically imports this module
 * and calls `mountSidePanel("#pdf-exporter-panel")`.
 *
 * The panel is mounted inside a **shadow root** on that fragment div, so its styles are fully encapsulated
 * on the shared editor page (see services/shadowMount.ts). The wrapper classes the panel CSS expects
 * (`pdf-exporter form-wrapper`) plus the token scope (`sbb-ui`) are reproduced on the inner container, and
 * `side-panel.css` is bundled (via `?inline`) and injected into the shadow alongside
 * react-sbb-polarion's bundled stylesheet.
 */
export function mountSidePanel(selector: string, deps?: SidePanelDependencies): Root | undefined {
  const host = document.querySelector<HTMLElement>(selector);
  if (!host) {
    console.error(`pdf-exporter: side panel mount target "${selector}" not found.`);
    return undefined;
  }
  const container = mountInShadow(host, {
    containerClassName: 'pdf-exporter form-wrapper sbb-ui',
    styleTexts: [panelStyle],
  });
  const root = createRoot(container);
  root.render(<SidePanel deps={deps} />);
  // Returned so the dev harness and the tests can unmount; the Polarion fragment ignores it.
  return root;
}
