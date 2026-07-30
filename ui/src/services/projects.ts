/**
 * Fetches the list of Polarion projects from the standard platform REST API
 * (`GET /polarion/rest/v1/projects`, JSON:API shape). Used only by the dev Landing page to build a
 * project (scope) picker, so navigation between features keeps a scope. In `vite dev` the request is
 * proxied to the configured Polarion and authenticated with `VITE_BEARER_TOKEN`.
 */

export interface PolarionProject {
  id: string;
  name: string;
}

const PROJECTS_URL = '/polarion/rest/v1/projects?page%5Bsize%5D=100&fields%5Bprojects%5D=name';

export async function fetchProjects(): Promise<PolarionProject[]> {
  const token = import.meta.env.VITE_BEARER_TOKEN;
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const response = await fetch(PROJECTS_URL, { headers, cache: 'no-cache' });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  const body = await response.json();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const data: any[] = body?.data ?? [];
  return data
    .map((item) => ({
      id: item?.id ?? item?.attributes?.id ?? '',
      name: item?.attributes?.name ?? item?.id ?? '',
    }))
    .filter((p: PolarionProject) => p.id)
    .sort((a: PolarionProject, b: PolarionProject) => a.name.localeCompare(b.name));
}
