import { Toaster } from '@grigoriev/react-sbb-polarion';
import { findFeature } from './features';
import Landing from './pages/Landing';

/**
 * Top-level feature router. There is a single index.html / bundle; the page to show is chosen from
 * the `feature` query parameter, e.g. `?feature=about`. No matching feature (including the bare root
 * `/`) renders the Landing stub, which lists links to every feature so the whole app can be exercised
 * in `vite dev` without a running Polarion.
 *
 * In Polarion, hivemodule.xml points the About admin extender at
 * `/polarion/pdf-exporter-app/ui/app/index.html?feature=about&embedded=true&scope=$scope$`.
 */
export default function App() {
  const feature = new URLSearchParams(window.location.search).get('feature');
  const match = findFeature(feature);
  const Page = match ? match.component : Landing;

  return (
    <div className="app standard-admin-page">
      {/* App-wide toast host: the shared RSP Toaster (top-center + richColors). The Authorization
          page reports its save outcomes through it. */}
      <Toaster />
      <Page />
    </div>
  );
}
