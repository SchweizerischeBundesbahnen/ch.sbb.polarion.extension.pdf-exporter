import { useEffect, useState } from 'react';
import { SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import { FEATURES } from '../features';
import { getCookie, setCookie } from '../services/cookies';
import { fetchProjects } from '../services/projects';
import type { PolarionProject } from '../services/projects';
import { getScope } from '../services/scope';

const DEV_SCOPE_COOKIE = 'pdf-exporter-dev-scope';

/** Initial scope: an explicit `scope` query param wins (e.g. arriving via a feature page's Overview
 * link), otherwise the last dev selection from the cookie, otherwise global. */
function initialScope(): string {
  const params = new URLSearchParams(window.location.search);
  if (params.has('scope')) {
    return getScope();
  }
  return getCookie(DEV_SCOPE_COOKIE) ?? '';
}

/**
 * Development landing page. Not shown in Polarion (there each feature is opened directly via its own
 * admin menu entry, already scoped), but during `vite dev` it lets us reach every feature from the app
 * root. It carries a **project scope**: pick a project here and every feature link includes
 * `scope=project/<id>/`, so navigating Overview -> feature no longer loses the scope. The choice is
 * remembered in a cookie and pre-selected from the `scope` query param when returning via a page's
 * "Overview" link.
 */
export default function Landing() {
  const [projects, setProjects] = useState<PolarionProject[]>([]);
  const [scope, setScope] = useState<string>(initialScope);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchProjects()
      .then((list) => {
        if (!cancelled) setProjects(list);
      })
      .catch(() => {
        if (!cancelled) {
          setError(
            'Could not load projects. In dev, set VITE_BEARER_TOKEN in ui/.env.local and restart the dev server.',
          );
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Remember the selection so a later bare "/?" (no scope param) restores it.
  useEffect(() => {
    setCookie(DEV_SCOPE_COOKIE, scope);
  }, [scope]);

  const scopeOptions = [
    { id: '', name: 'Repository (global scope)' },
    ...projects.map((p) => ({ id: `project/${p.id}/`, name: `${p.name} (${p.id})` })),
  ];

  return (
    <div className="page landing">
      <h1>PDF Exporter</h1>
      <p className="landing-intro">
        Experimental React UI. Pick a project scope and a feature below, or open one directly with{' '}
        <code>?feature=&lt;id&gt;&amp;scope=project/&lt;id&gt;/</code>.
      </p>

      <div className="landing-scope">
        <label>Project scope:</label>
        <SearchableSelect value={scope} onChange={setScope} options={scopeOptions} placeholder="" />
      </div>
      {error && <div className="alert alert-error">{error}</div>}

      <ul className="feature-list">
        {FEATURES.map((f) => (
          <li key={f.id}>
            {/* No embedded param: dev navigation is not embedded by default, so the ported pages show
                their "Overview" back link. Polarion (hivemodule.xml) opens them embedded. */}
            <a href={`?feature=${f.id}${scope ? `&scope=${encodeURIComponent(scope)}` : ''}`}>{f.label}</a>
            <span className="feature-desc">{f.description}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
