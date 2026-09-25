import { DocPage, type Feature as RoutedFeature, findFeature as findIn } from '@sbb-polarion/react-sbb-polarion';
import { DOC_ORDER } from './docs/manifest';
import About from './pages/About';
import Authorization from './pages/Authorization';
import CoverPage from './pages/CoverPage';
import Css from './pages/Css';
import Disclaimer from './pages/Disclaimer';
import ExportPopupPreview from './pages/ExportPopupPreview';
import FilenameTemplate from './pages/FilenameTemplate';
import HeaderFooter from './pages/HeaderFooter';
import Localization from './pages/Localization';
import SidePanelPreview from './pages/SidePanelPreview';
import StylePackageWeights from './pages/StylePackageWeights';
import StylePackages from './pages/StylePackages';
import Webhooks from './pages/Webhooks';
import WidgetPreview from './pages/WidgetPreview';

/**
 * A single navigable page of the app. The `id` is what appears in the URL as `?feature=<id>` and is
 * also what `hivemodule.xml` points its admin extenders at, so the ids here and the extender ids must
 * stay identical - a typo is a blank page in Polarion and no test catches it.
 *
 * Every administration entry of the extension is served from here; the legacy `pdf-exporter-admin`
 * webapp no longer exists.
 *
 * A label ending in `(dev)` marks a development harness - a page reachable only from the dev landing page,
 * which nothing in Polarion points at. That is the marker json-editor and strictdoc-exporter use for the
 * same thing, so it means the same across the extensions.
 *
 * Extends RSP's routed `Feature` (`id` + `component`, all FeatureRouter needs) with what the Landing page lists.
 */
export interface Feature extends RoutedFeature {
  label: string;
  description: string;
}

// The documentation-site articles, derived from the shared manifest (docs.config.json) rather than listed by
// hand: each renders through the single DocPage bound to its manifest entry, in the manifest's reading order.
// Adding an article is a docs.config.json item (plus its markdown and the pom render) - nothing here changes.
// They carry no hivemodule.xml menu entry of their own: they are reached from the single `documentation` node
// and the articles' cross-document links (rewritten to ?feature=<id> by the interceptor in App.tsx); the ids
// equal the generated html basenames those links point at.
const DOC_FEATURES: Feature[] = DOC_ORDER.map((doc) => {
  const Component = () => <DocPage doc={doc} />;
  Component.displayName = `DocPage(${doc.id})`;
  return {
    id: doc.id,
    label: doc.title,
    description: `${doc.title}, generated from ${doc.source}.`,
    component: Component,
  };
});

export const FEATURES: Feature[] = [
  {
    id: 'about',
    label: 'About',
    description: 'Extension version and general information.',
    component: About,
  },
  {
    id: 'disclaimer',
    label: 'Usage Disclaimer',
    description: 'The terms this extension is provided under.',
    component: Disclaimer,
  },
  // The documentation site, in manifest reading order (Quick Start, User Guide, Configuration, ...).
  ...DOC_FEATURES,
  {
    id: 'css',
    label: 'CSS',
    description: 'The custom stylesheet appended to the generated PDFs.',
    component: Css,
  },
  {
    id: 'cover-page',
    label: 'Cover Page',
    description: 'The HTML and CSS of the page printed before the document.',
    component: CoverPage,
  },
  {
    id: 'header-footer',
    label: 'Header and Footer',
    description: 'The six cells printed on every page of the exported PDF.',
    component: HeaderFooter,
  },
  {
    id: 'filename',
    label: 'Filename template',
    description: 'How exported documents, reports and test runs are named.',
    component: FilenameTemplate,
  },
  {
    id: 'localization',
    label: 'Localization',
    description: 'German, French and Italian translations of the exported work item fields.',
    component: Localization,
  },
  {
    id: 'webhooks',
    label: 'Webhooks',
    description: 'REST endpoints the generated HTML is passed through before it is rendered.',
    component: Webhooks,
  },
  {
    id: 'style-package',
    label: 'Style Package',
    description: 'The named bundles of export settings offered on the export dialog.',
    component: StylePackages,
  },
  {
    id: 'style-package-weights',
    label: 'Style Package Weights',
    description: 'Order the style packages; the top one is preselected on the export panel.',
    component: StylePackageWeights,
  },
  {
    id: 'authorization',
    label: 'Authorization',
    description: 'Configure which global and project roles are allowed to export.',
    component: Authorization,
  },
  {
    id: 'bulk-widget',
    label: 'Bulk PDF Export widget (dev)',
    description: 'Development harness for the report page widget. No administration page points here.',
    component: WidgetPreview,
  },
  {
    id: 'side-panel',
    label: 'Document Properties side panel (dev)',
    description:
      'Development harness for the export panel in the document editor: runs the real panel against a real ' +
      'document of the selected project. No administration page points here.',
    component: SidePanelPreview,
  },
  {
    id: 'export-popup',
    label: 'Export to PDF dialog (dev)',
    description:
      'Development harness for the export dialog the toolbar buttons open: runs the real dialog against a ' +
      'real document, for any document type and either export type. No administration page points here.',
    component: ExportPopupPreview,
  },
];

export function findFeature(id: string | null): Feature | undefined {
  return findIn(FEATURES, id);
}
