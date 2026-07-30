import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { filled, snapshotFeature } from './visualHelpers';

// Docker-only snapshot of the filename templates page: three editors across, no configuration
// selector (this feature has a single setting), and the placeholders reference table below.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe.skipIf(!__PIXEL_REFERENCES__)('Filename template page visual', () => {
  it('custom templates and the supported variables', async () => {
    await snapshotFeature(
      'filename',
      [
        {
          method: 'GET',
          match: /\/settings\/filename-template\/names\/Default\/content/,
          json: {
            useCustomValues: true,
            documentNameTemplate: '{{ PROJECT_NAME }}-{{ DOCUMENT_ID }}',
            reportNameTemplate: '{{ PROJECT_NAME }}-report',
            testRunNameTemplate: '{{ PROJECT_NAME }}-run',
          },
        },
        {
          method: 'GET',
          match: /\/settings\/filename-template\/default-content/,
          json: { useCustomValues: false, documentNameTemplate: '', reportNameTemplate: '', testRunNameTemplate: '' },
        },
        { method: 'GET', match: /\/settings\/filename-template\/names\/[^/]+\/revisions/, json: [] },
      ],
      filled('#custom-documentNameTemplate'),
      'filename-template-loaded',
    );
    expect(true).toBe(true);
  });
});
