import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  convertCollectionDocuments,
  convertPdf,
  downloadTestRunAttachments,
  errorMessageOf,
  warningOf,
} from '../src/services/conversion';
import type { Remote } from '../src/services/conversion';
import { installFetchMock, jsonResponse } from './mockFetch';

// The conversion protocol, which every export surface runs through: submit a job, poll it, read the
// result's headers. The legacy ExportContext.js drove this with XMLHttpRequest and callbacks and had no
// test of its own - these cover the requests, the two warning headers and the two side errands.

const REST_BASE = '/polarion/pdf-exporter/rest/internal';
const JOB_URL = `${REST_BASE}/convert/jobs/job-1`;

/** A Remote shaped exactly as useRemote's: one sender prefixes the REST base, one does not. */
const remote: Remote = {
  sendRequest: ({ method, url, body, contentType }) =>
    fetch(`${REST_BASE}${url}`, {
      method,
      body,
      headers: contentType ? { 'Content-Type': contentType } : undefined,
    }),
  sendAbsoluteRequest: ({ method, url }) => fetch(url, { method }),
};

const pdf = (headers: Record<string, string> = {}) =>
  new Response(new Blob(['%PDF-1.7'], { type: 'application/pdf' }), { status: 200, headers });

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('warningOf', () => {
  it('warns that compliance could not be checked when the header is absent', () => {
    expect(warningOf(new Headers())).toBe(
      "Resulting PDF couldn't be validated if it's compliant with the selected PDF variant.",
    );
  });

  it('says nothing when the result is compliant', () => {
    expect(warningOf(new Headers({ 'PDF-Variant-Compliant': 'true' }))).toBeNull();
  });

  it('warns about a non-compliant result', () => {
    expect(warningOf(new Headers({ 'PDF-Variant-Compliant': 'false' }))).toBe(
      "Be aware that resulting PDF isn't compliant with the selected PDF variant.",
    );
  });

  it('reports missing work item attachments, and joins two warnings with a blank line', () => {
    const warning = warningOf(
      new Headers({
        'Missing-WorkItem-Attachments-Count': '2',
        'WorkItem-IDs-With-Missing-Attachment': 'EL-1, EL-2',
        'PDF-Variant-Compliant': 'false',
      }),
    );
    expect(warning).toContain('2 image(s) in WI(s) EL-1, EL-2 were not exported');
    expect(warning).toContain('This image is not accessible');
    expect(warning).toContain("isn't compliant");
    expect(warning?.split('\n\n')).toHaveLength(2);
  });

  it('ignores a zero or unparseable attachment count', () => {
    expect(
      warningOf(new Headers({ 'Missing-WorkItem-Attachments-Count': '0', 'PDF-Variant-Compliant': 'true' })),
    ).toBeNull();
    expect(
      warningOf(new Headers({ 'Missing-WorkItem-Attachments-Count': 'x', 'PDF-Variant-Compliant': 'true' })),
    ).toBeNull();
  });
});

describe('errorMessageOf', () => {
  it('prefers message, falls back to errorMessage, and tolerates anything else', async () => {
    await expect(errorMessageOf(jsonResponse({ message: 'boom' }, 500))).resolves.toBe('boom');
    await expect(errorMessageOf(jsonResponse({ errorMessage: 'bang' }, 500))).resolves.toBe('bang');
    await expect(errorMessageOf(new Response('not json', { status: 500 }))).resolves.toBe('');
    await expect(errorMessageOf(new Response('', { status: 500 }))).resolves.toBe('');
  });
});

describe('convertPdf', () => {
  it('submits the job, polls until it is done and reads the result headers', async () => {
    let polls = 0;
    const fetchMock = installFetchMock([
      {
        method: 'POST',
        match: /\/convert\/jobs$/,
        respond: () => new Response(null, { status: 202, headers: { Location: JOB_URL } }),
      },
      {
        method: 'GET',
        match: /\/convert\/jobs\/job-1$/,
        respond: () =>
          ++polls < 2
            ? new Response(null, { status: 202 })
            : pdf({ 'Export-Filename': 'BigDoc.pdf', 'PDF-Variant-Compliant': 'true' }),
      },
    ]);

    const result = await convertPdf(remote, '{"documentType":"LIVE_DOC"}', 0);

    expect(result.fileName).toBe('BigDoc.pdf');
    expect(result.warning).toBeNull();
    expect(await result.blob.text()).toBe('%PDF-1.7');
    expect(polls).toBe(2);
    expect(fetchMock.mock.calls[0][0]).toBe(`${REST_BASE}/convert/jobs`);
    expect(fetchMock.mock.calls[0][1]?.body).toBe('{"documentType":"LIVE_DOC"}');
    // The job URL the server handed out is polled as given, not re-prefixed with the REST base.
    expect(fetchMock.mock.calls[1][0]).toBe(JOB_URL);
  });

  it('rejects with the message the submission failed with', async () => {
    installFetchMock([
      { method: 'POST', match: /\/convert\/jobs$/, respond: () => jsonResponse({ message: 'no permission' }, 403) },
    ]);
    await expect(convertPdf(remote, '{}', 0)).rejects.toThrow('no permission');
  });

  it('rejects with the message the polled job failed with', async () => {
    installFetchMock([
      {
        method: 'POST',
        match: /\/convert\/jobs$/,
        respond: () => new Response(null, { status: 202, headers: { Location: JOB_URL } }),
      },
      { method: 'GET', match: /job-1$/, respond: () => jsonResponse({ errorMessage: 'renderer died' }, 500) },
    ]);
    await expect(convertPdf(remote, '{}', 0)).rejects.toThrow('renderer died');
  });

  it('rejects when the job was accepted without a location to poll', async () => {
    installFetchMock([
      { method: 'POST', match: /\/convert\/jobs$/, respond: () => new Response(null, { status: 202 }) },
    ]);
    await expect(convertPdf(remote, '{}', 0)).rejects.toThrow('without a location');
  });
});

describe('downloadTestRunAttachments', () => {
  it('lists the attachments and downloads each under the name the server gives', async () => {
    const fetchMock = installFetchMock([
      { method: 'GET', match: /attachments\?/, json: [{ id: 'a1' }, { id: 'a2' }] },
      {
        method: 'GET',
        match: /attachments\/a\d\/content/,
        respond: (url) =>
          new Response(new Blob(['x']), {
            status: 200,
            headers: { Filename: `${url.includes('a1') ? 'first' : 'second'}.png` },
          }),
      },
    ]);
    const download = vi.fn();

    await downloadTestRunAttachments(remote, {
      projectId: 'elibrary',
      testRunId: 'run-1',
      revision: '42',
      filter: '*.png',
      testCaseFieldId: 'exportIt',
      download,
    });

    expect(download.mock.calls.map((call) => call[1])).toEqual(['first.png', 'second.png']);
    const listUrl = String(fetchMock.mock.calls[0][0]);
    expect(listUrl).toContain('/projects/elibrary/testruns/run-1/attachments?');
    expect(listUrl).toContain('revision=42');
    expect(listUrl).toContain('filter=*.png');
    expect(listUrl).toContain('testCaseFilterFieldId=exportIt');
  });

  it('logs and gives up when the list cannot be read', async () => {
    installFetchMock([
      { method: 'GET', match: /attachments\?/, respond: () => jsonResponse({ message: 'gone' }, 404) },
    ]);
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const download = vi.fn();

    await downloadTestRunAttachments(remote, { projectId: 'p', testRunId: 'r', download });

    expect(download).not.toHaveBeenCalled();
    expect(error).toHaveBeenCalled();
    error.mockRestore();
  });
});

describe('convertCollectionDocuments', () => {
  const collectionRoutes = (documents: unknown[]) => [
    { method: 'GET', match: /collections\/144\/documents$/, json: documents },
    {
      method: 'POST',
      match: /\/convert\/jobs$/,
      respond: () => new Response(null, { status: 202, headers: { Location: JOB_URL } }),
    },
    { method: 'GET', match: /job-1$/, respond: () => pdf({ 'PDF-Variant-Compliant': 'true' }) },
  ];

  const options = (download: ReturnType<typeof vi.fn>, exportPages: boolean) => ({
    projectId: 'elibrary',
    collectionId: '144',
    exportPages,
    params: { documentType: 'BASELINE_COLLECTION', watermark: true },
    toRequestBody: (params: Record<string, unknown>) => JSON.stringify(params),
    download,
    pollInterval: 0,
  });

  it('exports every document, addressing each one and naming the file after it', async () => {
    const fetchMock = installFetchMock(
      collectionRoutes([
        { projectId: 'elibrary', spaceId: 'Specs', documentName: 'One', documentType: 'LIVE_DOC' },
        {
          projectId: 'other',
          spaceId: '_default',
          documentName: 'Two',
          documentType: 'LIVE_DOC',
          fileName: 'named.pdf',
        },
      ]),
    );
    const download = vi.fn();

    await convertCollectionDocuments(remote, options(download, false));

    expect(download.mock.calls.map((call) => call[1])).toEqual(['elibrary_Specs_One.pdf', 'named.pdf']);
    const submitted = fetchMock.mock.calls
      .filter((call) => String(call[0]).endsWith('/convert/jobs'))
      .map((call) => JSON.parse(String(call[1]?.body)) as Record<string, unknown>);
    expect(submitted[0]).toMatchObject({ projectId: 'elibrary', locationPath: 'Specs/One', watermark: true });
    expect(submitted[1]).toMatchObject({ projectId: 'other', locationPath: '_default/Two' });
  });

  it('skips Live Reports unless the widget exports pages too', async () => {
    const documents = [
      { projectId: 'elibrary', spaceId: 'Specs', documentName: 'Doc', documentType: 'LIVE_DOC' },
      { projectId: 'elibrary', spaceId: 'Specs', documentName: 'Report', documentType: 'LIVE_REPORT' },
    ];

    installFetchMock(collectionRoutes(documents));
    const withoutPages = vi.fn();
    await convertCollectionDocuments(remote, options(withoutPages, false));
    expect(withoutPages).toHaveBeenCalledTimes(1);

    installFetchMock(collectionRoutes(documents));
    const withPages = vi.fn();
    await convertCollectionDocuments(remote, options(withPages, true));
    expect(withPages).toHaveBeenCalledTimes(2);
  });

  it('warns and returns for an empty collection', async () => {
    installFetchMock(collectionRoutes([]));
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const download = vi.fn();

    await convertCollectionDocuments(remote, options(download, true));

    expect(download).not.toHaveBeenCalled();
    expect(warn).toHaveBeenCalledWith('No documents found in the collection.');
    warn.mockRestore();
  });

  it('reports a failure even when a later document succeeds', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /collections\/144\/documents$/,
        json: [
          { projectId: 'elibrary', spaceId: 'S', documentName: 'Bad', documentType: 'LIVE_DOC' },
          { projectId: 'elibrary', spaceId: 'S', documentName: 'Good', documentType: 'LIVE_DOC' },
        ],
      },
      {
        method: 'POST',
        match: /\/convert\/jobs$/,
        respond: (_url, init) =>
          String(init?.body).includes('Bad')
            ? jsonResponse({ message: 'document is empty' }, 400)
            : new Response(null, { status: 202, headers: { Location: JOB_URL } }),
      },
      { method: 'GET', match: /job-1$/, respond: () => pdf({ 'PDF-Variant-Compliant': 'true' }) },
    ]);

    await expect(convertCollectionDocuments(remote, options(vi.fn(), false))).rejects.toThrow('document is empty');
  });

  it('rejects when the collection cannot be listed', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /collections\/144\/documents$/,
        respond: () => jsonResponse({ message: 'no access' }, 403),
      },
    ]);
    await expect(convertCollectionDocuments(remote, options(vi.fn(), false))).rejects.toThrow('no access');
  });
});
