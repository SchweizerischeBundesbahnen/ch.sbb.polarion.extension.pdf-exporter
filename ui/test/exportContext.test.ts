import { describe, expect, it } from 'vitest';
import type { DocumentType } from '../src/export/documentType';
import {
  documentNameOf,
  parseDocumentLocation,
  scopeFor,
  spaceIdOf,
  toDocumentIdentity,
} from '../src/services/exportContext';

// The Polarion location hashes the export surfaces have to make sense of, ported one for one from the
// legacy src/test/js/ExportContextTest.js so that the TypeScript parser is held to what shipped. Every
// case names the URL it stands for, as the mocha suite did.
//
// A test run hash is resolved through an injected href rather than Polarion's own DOM node: the legacy
// suite ran without a `window` and so never reached that branch at all.

interface Expected {
  documentType: DocumentType;
  projectId: string | null;
  locationPath?: string;
  baselineRevision?: string;
  revision?: string;
  urlQueryParameters?: Record<string, string>;
  spaceId?: string;
  documentName?: string;
}

const check = (hash: string, documentType: DocumentType, expected: Expected) => {
  const location = parseDocumentLocation(hash, { documentType, resolveTestRunHref: () => null });
  expect(location.documentType).toBe(expected.documentType);
  expect(location.projectId).toBe(expected.projectId);
  expect(location.locationPath).toBe(expected.locationPath);
  expect(location.baselineRevision).toBe(expected.baselineRevision);
  expect(location.revision).toBe(expected.revision);
  expect(location.urlQueryParameters).toEqual(expected.urlQueryParameters);
  expect(spaceIdOf(location)).toBe(expected.spaceId);
  expect(documentNameOf(location)).toBe(expected.documentName);
};

describe('parseDocumentLocation', () => {
  it('#/project/elibrary/wiki/BigDoc', () => {
    check('#/project/elibrary/wiki/BigDoc', 'LIVE_DOC', {
      documentType: 'LIVE_DOC',
      projectId: 'elibrary',
      locationPath: '_default/BigDoc',
      spaceId: '_default',
      documentName: 'BigDoc',
    });
  });

  it('#/project/elibrary/wiki/Specification/Administration%20Specification', () => {
    check('#/project/elibrary/wiki/Specification/Administration%20Specification', 'LIVE_DOC', {
      documentType: 'LIVE_DOC',
      projectId: 'elibrary',
      locationPath: 'Specification/Administration Specification',
      spaceId: 'Specification',
      documentName: 'Administration Specification',
    });
  });

  it('#/project/mega_project/wiki/Specs/test', () => {
    check('#/project/mega_project/wiki/Specs/test', 'LIVE_DOC', {
      documentType: 'LIVE_DOC',
      projectId: 'mega_project',
      locationPath: 'Specs/test',
      spaceId: 'Specs',
      documentName: 'test',
    });
  });

  it('#/wiki/classic%20wiki%20page (global scope)', () => {
    check('#/wiki/classic%20wiki%20page', 'LIVE_DOC', {
      documentType: 'LIVE_DOC',
      projectId: null,
      locationPath: '_default/classic wiki page',
      spaceId: '_default',
      documentName: 'classic wiki page',
    });
  });

  it('#/wiki/TestLiveReport', () => {
    check('#/wiki/TestLiveReport', 'LIVE_REPORT', {
      documentType: 'LIVE_REPORT',
      projectId: null,
      locationPath: '_default/TestLiveReport',
      spaceId: '_default',
      documentName: 'TestLiveReport',
    });
  });

  it('#/wiki/space/LiveReport', () => {
    check('#/wiki/space/LiveReport', 'LIVE_REPORT', {
      documentType: 'LIVE_REPORT',
      projectId: null,
      locationPath: 'space/LiveReport',
      spaceId: 'space',
      documentName: 'LiveReport',
    });
  });

  it('#/project/elibrary/home (the project home rich page)', () => {
    check('#/project/elibrary/home', 'LIVE_REPORT', {
      documentType: 'LIVE_REPORT',
      projectId: 'elibrary',
      locationPath: '_default/Home',
      spaceId: '_default',
      documentName: 'Home',
    });
  });

  it('#/home (the repository home rich page)', () => {
    check('#/home', 'LIVE_REPORT', {
      documentType: 'LIVE_REPORT',
      projectId: null,
      locationPath: '_default/Home',
      spaceId: '_default',
      documentName: 'Home',
    });
  });

  it('#/project/elibrary/testrun?id=... becomes a TEST_RUN without a location path', () => {
    check('#/project/elibrary/testrun?id=elibrary_20231026-163136654', 'LIVE_REPORT', {
      documentType: 'TEST_RUN',
      projectId: 'elibrary',
      urlQueryParameters: { id: 'elibrary_20231026-163136654' },
    });
  });

  it('#/project/elibrary/testruns becomes a TEST_RUN when the run href cannot be read', () => {
    check('#/project/elibrary/testruns', 'LIVE_REPORT', {
      documentType: 'TEST_RUN',
      projectId: 'elibrary',
    });
  });

  it('#/project/elibrary/wiki/Reports/LiveReport%20with%20params?... keeps every query parameter', () => {
    check(
      '#/project/elibrary/wiki/Reports/LiveReport%20with%20params' +
        '?stringParameter=asd&workItemType=changerequest&yesnoParameter=yes',
      'LIVE_DOC',
      {
        documentType: 'LIVE_DOC',
        projectId: 'elibrary',
        locationPath: 'Reports/LiveReport with params',
        urlQueryParameters: {
          stringParameter: 'asd',
          workItemType: 'changerequest',
          yesnoParameter: 'yes',
        },
        spaceId: 'Reports',
        documentName: 'LiveReport with params',
      },
    );
  });

  it('#/project/elibrary/collection?id=144 has no location path of its own', () => {
    check('#/project/elibrary/collection?id=144', 'BASELINE_COLLECTION', {
      documentType: 'BASELINE_COLLECTION',
      projectId: 'elibrary',
      urlQueryParameters: { id: '144' },
    });
  });

  it('#/project/elibrary/collection/145/wiki/live_doc', () => {
    check('#/project/elibrary/collection/145/wiki/live_doc', 'LIVE_DOC', {
      documentType: 'LIVE_DOC',
      projectId: 'elibrary',
      locationPath: '_default/live_doc',
      spaceId: '_default',
      documentName: 'live_doc',
    });
  });

  it('#/project/drivepilot/collection/elibrary//144/wiki/Requirements/live%20doc', () => {
    check('#/project/drivepilot/collection/elibrary//144/wiki/Requirements/live%20doc', 'LIVE_DOC', {
      documentType: 'LIVE_DOC',
      projectId: 'drivepilot',
      locationPath: 'Requirements/live doc',
      spaceId: 'Requirements',
      documentName: 'live doc',
    });
  });

  it('#/project/drivepilot/collection/1/wiki/Requirements/...?revision=112', () => {
    check(
      '#/project/drivepilot/collection/1/wiki/Requirements/System%20Requirement%20Specification?revision=112',
      'LIVE_DOC',
      {
        documentType: 'LIVE_DOC',
        projectId: 'drivepilot',
        locationPath: 'Requirements/System Requirement Specification',
        revision: '112',
        urlQueryParameters: { revision: '112' },
        spaceId: 'Requirements',
        documentName: 'System Requirement Specification',
      },
    );
  });

  it('#/baseline/6749/project/elibrary/wiki/BigDoc2', () => {
    check('#/baseline/6749/project/elibrary/wiki/BigDoc2', 'LIVE_DOC', {
      documentType: 'LIVE_DOC',
      projectId: 'elibrary',
      locationPath: '_default/BigDoc2',
      baselineRevision: '6749',
      spaceId: '_default',
      documentName: 'BigDoc2',
    });
  });

  it('#/baseline/6711/project/elibrary/wiki/Specification/Epic%20Statistics?parameter=value', () => {
    check('#/baseline/6711/project/elibrary/wiki/Specification/Epic%20Statistics?parameter=value', 'LIVE_REPORT', {
      documentType: 'LIVE_REPORT',
      projectId: 'elibrary',
      locationPath: 'Specification/Epic Statistics',
      baselineRevision: '6711',
      urlQueryParameters: { parameter: 'value' },
      spaceId: 'Specification',
      documentName: 'Epic Statistics',
    });
  });

  it('resolves a test run opened from the list through its own href', () => {
    const location = parseDocumentLocation('#/project/elibrary/testruns', {
      documentType: 'LIVE_REPORT',
      resolveTestRunHref: () => '#/project/elibrary/testrun?id=elibrary_20231026-163136654',
    });
    expect(location.documentType).toBe('TEST_RUN');
    expect(location.projectId).toBe('elibrary');
    expect(location.urlQueryParameters).toEqual({ id: 'elibrary_20231026-163136654' });
  });

  it('reads the test run href off Polarion’s label widget by default', () => {
    const container = document.createElement('div');
    container.className = 'polarion-TestRunLabelWidget-container';
    const link = document.createElement('a');
    link.setAttribute('href', '#/project/elibrary/testrun?id=run-42');
    container.appendChild(link);
    document.body.appendChild(container);
    try {
      const location = parseDocumentLocation('#/project/elibrary/testruns', { documentType: 'LIVE_REPORT' });
      expect(location.urlQueryParameters).toEqual({ id: 'run-42' });
    } finally {
      container.remove();
    }
  });

  it('skips the location entirely for a bulk export', () => {
    const location = parseDocumentLocation('#/project/elibrary/wiki/Specs/BigDoc', {
      documentType: 'LIVE_DOC',
      exportType: 'BULK',
    });
    expect(location.projectId).toBe('elibrary');
    expect(location.locationPath).toBeUndefined();
    expect(location.documentType).toBe('LIVE_DOC');
  });

  it('defaults to a single Live Document export', () => {
    const location = parseDocumentLocation('#/project/elibrary/wiki/BigDoc', { resolveTestRunHref: () => null });
    expect(location.documentType).toBe('LIVE_DOC');
    expect(location.locationPath).toBe('_default/BigDoc');
  });
});

describe('toDocumentIdentity', () => {
  it('spells out the scope, the space and the document name', () => {
    const location = parseDocumentLocation('#/project/elibrary/wiki/Specs/BigDoc', { resolveTestRunHref: () => null });
    expect(toDocumentIdentity(location)).toEqual({
      ...location,
      scope: 'project/elibrary/',
      spaceId: 'Specs',
      documentName: 'BigDoc',
    });
  });

  it('has an empty scope outside any project', () => {
    const location = parseDocumentLocation('#/wiki/space/LiveReport', { resolveTestRunHref: () => null });
    expect(scopeFor(location)).toBe('');
    expect(toDocumentIdentity(location).scope).toBe('');
  });
});
