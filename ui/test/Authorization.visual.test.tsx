import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { snapshotFeature } from './visualHelpers';

// Docker-only snapshot of the Authorization page: the two role groups, each a multi-select
// SearchableSelect, the Save / Cancel / Default / Revisions toolbar and the Quick Help text. This is
// the page a styling change in the shared component would move without any behavior test noticing.
//
// Captured through snapshotFeature, so the chips and the placeholder are waited for as well as the
// controls: a trigger exists before it is painted (see dropdownsUpgraded).

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

/** Both role groups, which is this page's own content; what they hold is snapshotFeature's half. */
const bothRoleGroups = () => document.querySelectorAll('.roles-group select').length === 2;

describe.skipIf(!__PIXEL_REFERENCES__)('Authorization page visual', () => {
  it('global and project roles, one of them granted', async () => {
    await snapshotFeature(
      'authorization',
      [
        {
          method: 'GET',
          match: /\/roles\?/,
          json: { globalRoles: ['admin', 'user'], projectRoles: ['project_admin', 'project_user'] },
        },
        {
          method: 'GET',
          match: /\/settings\/authorization\/names\/Default\/content/,
          json: { globalRoles: ['admin'], projectRoles: [] },
        },
      ],
      bothRoleGroups,
      'authorization-loaded',
    );
    expect(true).toBe(true);
  });
});
