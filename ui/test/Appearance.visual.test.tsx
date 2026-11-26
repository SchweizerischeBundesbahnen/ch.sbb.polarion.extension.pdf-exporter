import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import type { Route } from './mockFetch';
import { found, snapshotFeature } from './visualHelpers';

// Docker-only snapshot of the Appearance page: the three toolbar groups with their sub-options, the
// Save / Cancel / Default / Revisions toolbar and the Quick Help text.
//
// Two captures, because the page has two looks and the difference between them is the whole point of
// the page: a project which inherits the Default Repository configuration shows the values it
// inherits, greyed out behind the inherit checkbox, and a project which has its own shows them
// editable. The layout is also what a change in RSP's own checkbox rules would move without a
// behavior test noticing: these labels only sit one per line because this page outranks that rule.

const origUrl = window.location.pathname + window.location.search;

const CONTENT = {
  liveDocButton: true,
  liveReportButton: false,
  liveReportAutoExpandTools: false,
  classicWikiButton: true,
  classicWikiAutoExpandTools: true,
};

/** @param names what `settings/appearance/names` answers - the scope each `Default` was found in. */
const routes = (names: { name: string; scope: string }[]): Route[] => [
  { method: 'GET', match: /\/settings\/appearance\/names\?/, json: names },
  { method: 'GET', match: /\/settings\/appearance\/names\/Default\/content/, json: CONTENT },
  { method: 'GET', match: /\/settings\/appearance\/names\/Default\/revisions/, json: [] },
];

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe.skipIf(!__PIXEL_REFERENCES__)('Appearance page visual', () => {
  it('a project which inherits the Default Repository configuration', async () => {
    await snapshotFeature(
      'appearance',
      routes([{ name: 'Default', scope: '' }]),
      found('#appearance-inherit'),
      'appearance-inherited',
    );
    expect(true).toBe(true);
  });

  it('a project which configures the buttons itself', async () => {
    await snapshotFeature(
      'appearance',
      routes([{ name: 'Default', scope: 'project/elibrary/' }]),
      found('#live-doc-button'),
      'appearance-configured',
    );
    expect(true).toBe(true);
  });
});
