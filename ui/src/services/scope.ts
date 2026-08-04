/**
 * Reads the `scope` query parameter that Polarion substitutes into the page URL (`&scope=$scope$`).
 * For project scope it looks like `project/<projectId>/`; repository/global scope is empty.
 *
 * Polarion always includes the trailing slash; we normalize it here so a manually typed dev URL like
 * `?scope=project/elibrary` still matches the canonical scope the settings framework expects.
 */
export function getScope(): string {
  const raw = new URLSearchParams(window.location.search).get('scope') || '';
  return raw && !raw.endsWith('/') ? `${raw}/` : raw;
}

/** Extracts the project id from a scope string. Handles scope with or without a trailing slash. */
export function getProjectIdFromScope(scope: string): string {
  const match = scope.match(/project\/([^/]+)/);
  return match ? match[1] : '';
}
