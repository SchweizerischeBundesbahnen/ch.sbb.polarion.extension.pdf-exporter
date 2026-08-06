import { afterEach, describe, expect, it, vi } from 'vitest';
import { MAX_PAGES, documentEditorHash, documentPath, fetchDocuments, nextPagePath } from '../src/services/documents';
import { installFetchMock, jsonResponse } from './mockFetch';

// The document list the side panel's development harness picks from, and the editor hash it writes for the
// document that was picked. The hash is the interesting half: it is what makes the harness drive the real
// panel, so it has to be the URL Polarion would have - which is what the product's export context parses.

const doc = (spaceId: string, moduleName: string) => ({
  attributes: { moduleFolder: spaceId, moduleName },
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('listing a project documents', () => {
  it('reads the space and the name of each document, in path order', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/projects\/elibrary\/documents/,
        json: { data: [doc('Specs', 'Beta'), doc('_default', 'Alpha'), doc('Specs', 'Alpha')] },
      },
    ]);

    const list = await fetchDocuments('elibrary');

    // Locale collation, so `_default` sorts ahead of the named spaces
    expect(list.documents.map(documentPath)).toEqual(['_default/Alpha', 'Specs/Alpha', 'Specs/Beta']);
    expect(list.truncated).toBe(false);
  });

  it('drops a document missing either half of an editor URL', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/documents/,
        json: { data: [doc('Specs', 'Good'), doc('', 'NoSpace'), doc('Specs', ''), {}, null] },
      },
    ]);

    const list = await fetchDocuments('elibrary');

    expect(list.documents.map(documentPath)).toEqual(['Specs/Good']);
  });

  it('asks for the two fields it needs and nothing else', async () => {
    const fetchMock = installFetchMock([{ method: 'GET', match: /\/documents/, json: { data: [] } }]);

    await fetchDocuments('my project');

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/projects/my%20project/documents');
    expect(url).toContain('fields%5Bdocuments%5D=moduleName%2CmoduleFolder');
  });

  it('follows the pages until there is no next one', async () => {
    let page = 0;
    // One route for every page: the first request and each followed `next` all land here.
    const fetchMock = installFetchMock([
      {
        method: 'GET',
        match: /\/documents|\/next/,
        respond: () => {
          page++;
          return jsonResponse({
            data: [doc('Specs', `Doc${page}`)],
            links: page < 3 ? { next: `http://polarion.example/polarion/rest/v1/next?page=${page + 1}` } : {},
          });
        },
      },
    ]);

    const list = await fetchDocuments('elibrary');

    expect(list.documents.map(documentPath)).toEqual(['Specs/Doc1', 'Specs/Doc2', 'Specs/Doc3']);
    expect(list.truncated).toBe(false);
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it('stops at the page cap and says the list is not the whole project', async () => {
    // A next link on every page: without the cap this would follow the server forever.
    const fetchMock = installFetchMock([
      {
        method: 'GET',
        match: /\/documents|\/next/,
        respond: () =>
          jsonResponse({
            data: [doc('Specs', 'Doc')],
            links: { next: 'http://polarion.example/polarion/rest/v1/next' },
          }),
      },
    ]);

    const list = await fetchDocuments('elibrary');

    expect(fetchMock).toHaveBeenCalledTimes(MAX_PAGES);
    expect(list.truncated).toBe(true);
    // Every page returned the same document; the picker is offered one option, not twenty
    expect(list.documents.map(documentPath)).toEqual(['Specs/Doc']);
  });

  it('lists a document once when the pages shift and repeat it', async () => {
    // Separate requests, so a document added or removed in between can move the window and repeat an
    // entry. Two identical options is a duplicate React key and a choice a user cannot make.
    let page = 0;
    installFetchMock([
      {
        method: 'GET',
        match: /\/documents|\/next/,
        respond: () => {
          page++;
          return jsonResponse({
            data: [doc('Specs', 'Repeated'), doc('Specs', `Only${page}`)],
            links: page < 2 ? { next: 'http://polarion.example/polarion/rest/v1/next' } : {},
          });
        },
      },
    ]);

    const list = await fetchDocuments('elibrary');

    expect(list.documents.map(documentPath)).toEqual(['Specs/Only1', 'Specs/Only2', 'Specs/Repeated']);
  });

  it('follows the next page through the dev proxy rather than to the server directly', () => {
    // Polarion answers with an absolute URL; fetched as it is, the browser would leave the dev origin and
    // be refused by CORS. Only the path and query survive.
    expect(nextPagePath('http://polarion.example/polarion/rest/v1/projects/x/documents?page%5Bnumber%5D=2')).toBe(
      '/polarion/rest/v1/projects/x/documents?page%5Bnumber%5D=2',
    );
    expect(nextPagePath('/polarion/rest/v1/relative?a=1')).toBe('/polarion/rest/v1/relative?a=1');
    expect(nextPagePath(undefined)).toBeNull();
    expect(nextPagePath('')).toBeNull();
    expect(nextPagePath(42)).toBeNull();
  });

  it('reports a list that cannot be read', async () => {
    installFetchMock([{ method: 'GET', match: /\/documents/, status: 401 }]);

    await expect(fetchDocuments('elibrary')).rejects.toThrow('HTTP 401');
  });
});

describe('the editor hash of a document', () => {
  it('is the URL Polarion opens the document at', () => {
    expect(documentEditorHash('elibrary', { spaceId: 'Specs', moduleName: 'Requirements' })).toBe(
      '#/project/elibrary/wiki/Specs/Requirements',
    );
  });

  it('escapes what a URL cannot carry literally, the way the parser unescapes it', () => {
    expect(documentEditorHash('elibrary', { spaceId: 'Default Space', moduleName: 'Cross Link Issue' })).toBe(
      '#/project/elibrary/wiki/Default%20Space/Cross%20Link%20Issue',
    );
  });

  it('leaves out the default space, exactly as Polarion leaves it out', () => {
    // The export context puts `_default/` back (addDefaultSpaceIfRequired), so emitting it here would hide
    // that branch from the harness.
    expect(documentEditorHash('elibrary', { spaceId: '_default', moduleName: 'Home' })).toBe(
      '#/project/elibrary/wiki/Home',
    );
  });
});
