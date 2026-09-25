import {
  DocLinkInterceptor,
  type DocSearchRecord,
  DocsProvider,
  FeatureRouter,
  buildDocsConfig,
} from '@sbb-polarion/react-sbb-polarion';
import ToastHost from './components/ToastHost';
import { DOC_ORDER } from './docs/manifest';
import searchIndex from './docs/search-index.json';
import { FEATURES } from './features';
import Landing from './pages/Landing';
import { switchToFeatureNode } from './services/adminNav';

/** The extension's sources on GitHub; used for the "article not generated" fallback link. */
const SOURCE_BASE_URL =
  'https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/blob/main';

/**
 * Documentation-site configuration for the shared components (@sbb-polarion/react-sbb-polarion): the article
 * manifest, the build-generated search index, and the admin-shell sync a cross-document link triggers. The
 * help articles cross-link with plain relative markdown links so they work as-is on GitHub; the build leaves
 * them relative, and DocLinkInterceptor resolves a click at runtime - a `.md` source through `mdLinkMap` to a
 * `?feature=` switch, and any other relative link (docs/openapi.json) to `sourceBaseUrl` on GitHub.
 * `onDocLinkNavigate` prefers switching the admin shell's node (so Polarion's breadcrumb and left menu
 * follow); it returns false when it cannot, and the interceptor then falls back to a plain in-frame navigation.
 */
const docsConfig = buildDocsConfig({
  docs: DOC_ORDER,
  searchIndex: searchIndex as DocSearchRecord[],
  sourceBaseUrl: SOURCE_BASE_URL,
  // The non-article markdown sources that still resolve to a feature: README (About) and the Disclaimer are
  // not doc-site articles. buildDocsConfig folds these together with each article's own source (from the
  // manifest) into the mdLinkMap the interceptor uses to turn relative `.md` cross-links into feature switches.
  extraMdLinks: {
    'README.md': 'about',
    'DISCLAIMER.md': 'disclaimer',
  },
  // The breadcrumb keeps its defaults: "Documentation", linking to the first article of the manifest (Quick
  // Start), which is also the page the documentation admin node opens.
  onDocLinkNavigate: switchToFeatureNode,
});

/**
 * Top-level feature router. There is a single index.html / bundle; the page to show is chosen from the
 * `feature` query parameter, e.g. `?feature=about`. No matching feature (including the bare root `/`) renders
 * the Landing stub, which lists links to every feature so the whole app can be exercised in `vite dev` without
 * a running Polarion.
 *
 * In Polarion, hivemodule.xml points the About admin extender at
 * `/polarion/pdf-exporter-app/ui/app/index.html?feature=about&embedded=true&scope=$scope$`.
 */
export default function App() {
  return (
    <DocsProvider config={docsConfig}>
      {/* Delegates cross-document help-article links to in-app feature navigation (see docsConfig). */}
      <DocLinkInterceptor>
        <div className="app standard-admin-page">
          {/* App-wide toast host: the shared RSP Toaster, which every page reports its outcomes through.
              `ToastHost` rather than the Toaster itself because the export dialog brings a host of its own. */}
          <ToastHost />
          <FeatureRouter features={FEATURES} fallback={Landing} />
        </div>
      </DocLinkInterceptor>
    </DocsProvider>
  );
}
