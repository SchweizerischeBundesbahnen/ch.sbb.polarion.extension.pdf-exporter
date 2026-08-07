import { describe, expect, it } from 'vitest';
import type { DocumentType } from '../src/export/documentType';
import { childValue, resolveLanguage, toExportForm } from '../src/export/exportForm';
import type { ExportTarget } from '../src/export/exportParams';
import { buildExportParams, toRequestBody } from '../src/export/exportParams';
import {
  CHAPTERS_ERROR,
  parseChapters,
  parseMetadataFields,
  validateNumberedListStyles,
} from '../src/export/validation';
import { SAMPLE_DOCUMENT, SAMPLE_STYLE_PACKAGE_FULL } from './sidePanelSamples';

// The rules both export dialogs run on, which the legacy ExportPanel.js and ExportPopup.js carried as DOM
// manipulation: how a style package becomes a form, what the three value-carrying fields accept, and what
// the export request ends up saying. They are asserted here rather than through either dialog because they
// are what the two share.

/** What the side panel exports, and the popup's default. */
const LIVE_DOC: ExportTarget = { documentType: 'LIVE_DOC', exportType: 'SINGLE' };

const params = (form = toExportForm(SAMPLE_STYLE_PACKAGE_FULL), target: ExportTarget = LIVE_DOC) => {
  const built = buildExportParams(form, SAMPLE_DOCUMENT, target);
  if ('error' in built) {
    throw new Error(`unexpected validation error: ${built.error.message}`);
  }
  return built.params;
};

describe('reading a style package into the form', () => {
  it('switches a setting on exactly when the package carries a value for it', () => {
    const form = toExportForm({ coverPage: 'SBB', specificChapters: '1,2' });

    expect(form.coverPageEnabled).toBe(true);
    expect(form.coverPage).toBe('SBB');
    expect(form.specificChaptersEnabled).toBe(true);
    expect(form.specificChapters).toBe('1,2');
    // Nothing said about webhooks means the switch is off, and the field falls back to Default
    expect(form.webhooksEnabled).toBe(false);
    expect(form.webhooks).toBe('Default');
  });

  it('resolves what a package leaves unset the way the renderer resolves it', () => {
    const form = toExportForm({});

    expect(form.paperSize).toBe('A4');
    expect(form.orientation).toBe('PORTRAIT');
    expect(form.pdfVariant).toBe('PDF_A_2B');
    expect(form.imageDensity).toBe('DPI_96');
    expect(form.renderComments).toBe('OPEN');
    expect(form.linkRoleDirection).toBe('BOTH');
    expect(form.headersColor).toBe('#004d73');
  });

  it('lets the document language win over the package, but only where settings are exposed', () => {
    const exposed = toExportForm({ exposeSettings: true, language: 'fr' }, { documentLanguage: 'it' });
    expect(exposed.language).toBe('it');

    // A package that keeps its settings to itself is not redefined by the document it is applied to
    const hidden = toExportForm({ exposeSettings: false, language: 'fr' }, { documentLanguage: 'it' });
    expect(hidden.language).toBe('fr');
  });

  it('ignores a document language that is not one of the offered ones', () => {
    // English is not an option - there is nothing to localize into - and neither is an unknown id
    expect(resolveLanguage('en')).toBeUndefined();
    expect(resolveLanguage('klingon')).toBeUndefined();
    expect(resolveLanguage(null)).toBeUndefined();
    // The field is an enum option, so it may arrive as the display name or in another case
    expect(resolveLanguage('Deutsch')).toBe('de');
    expect(resolveLanguage('FR')).toBe('fr');
  });

  it('reads the language custom field through, no dialog offering a control for it', () => {
    // Which LiveDoc field holds the document's language is an administrator's decision, so it reaches the
    // form only to be put back into the request the server resolves it with.
    expect(toExportForm({ languageCustomField: 'docLanguage' }).languageCustomField).toBe('docLanguage');
    // A package naming none leaves nothing for the request to carry
    expect(toExportForm({}).languageCustomField).toBe('');
  });

  it('lets the URL query win over the package, the document being viewed filtered', () => {
    const form = toExportForm({ workItemsQuery: 'type:task' }, { urlQuery: 'type:requirement' });

    expect(form.workItemsQueryEnabled).toBe(true);
    expect(form.workItemsQuery).toBe('type:requirement');
  });

  it('switches the roles on only when the package names any', () => {
    expect(toExportForm({ linkedWorkitemRoles: [] }).rolesEnabled).toBe(false);
    expect(toExportForm({ linkedWorkitemRoles: ['relates_to'] }).rolesEnabled).toBe(true);
  });

  it('falls a name the scope no longer offers back to Default, but not a pending option list', () => {
    const options = [{ id: 'Default', name: 'Default' }];

    expect(childValue(options, 'Default')).toBe('Default');
    expect(childValue(options, 'Gone')).toBe('Default');
    // Nothing loaded yet: the stored reference is kept rather than quietly rewritten
    expect(childValue([], 'Gone')).toBe('Gone');
  });
});

describe('the fields a user can get wrong', () => {
  it('accepts a comma separated list of positive integers as chapters, and nothing else', () => {
    expect(parseChapters('1,2,4')).toEqual(['1', '2', '4']);
    expect(parseChapters('1, 2')).toEqual(['1', '2']);
    expect(parseChapters('')).toBeUndefined();
    expect(parseChapters('0')).toBeUndefined();
    expect(parseChapters('-1')).toBeUndefined();
    expect(parseChapters('01')).toBeUndefined();
    expect(parseChapters('1,x')).toBeUndefined();
    expect(parseChapters('1.5')).toBeUndefined();
  });

  it('trims the metadata fields and drops the empty ones', () => {
    expect(parseMetadataFields(' docOwner , docLanguage ,')).toEqual(['docOwner', 'docLanguage']);
    expect(parseMetadataFields('')).toEqual([]);
  });

  it('accepts only combinations of 1aAiI as numbered list styles', () => {
    expect(validateNumberedListStyles('1aAiI')).toBeUndefined();
    expect(validateNumberedListStyles('')).toBe('Please, provide some value');
    expect(validateNumberedListStyles('  ')).toBe('Please, provide some value');
    expect(validateNumberedListStyles('1b')).toBe("Please, provide any combination of characters '1aAiI'");
  });
});

describe('building the export request', () => {
  it('sends what is switched on and leaves out what is not', () => {
    const built = params();

    expect(built.documentType).toBe('LIVE_DOC');
    expect(built.projectId).toBe('elibrary');
    expect(built.locationPath).toBe('Default Space/Cross Link Issue');
    expect(built.coverPage).toBe('Default');
    expect(built.chapters).toEqual(['1', '2']);
    expect(built.metadataFields).toEqual(['docOwner']);
    expect(built.numberedListStyles).toBe('1ai');
    expect(built.language).toBe('de');
    expect(built.linkedWorkitemRoles).toEqual(['relates_to']);
    expect(built.linkRoleDirection).toBe('BOTH');
  });

  it('uses the product ExportParams field names, which the endpoints read', () => {
    const built = params();

    expect(built.cutEmptyWIAttributes).toBe(true);
    expect(built.cutLocalUrls).toBe(true);
  });

  it('nulls what a switched-off setting would otherwise carry', () => {
    const form = toExportForm(SAMPLE_STYLE_PACKAGE_FULL);
    const built = params({
      ...form,
      coverPageEnabled: false,
      webhooksEnabled: false,
      renderCommentsEnabled: false,
      specificChaptersEnabled: false,
      metadataFieldsEnabled: false,
      customListStylesEnabled: false,
      localizeEnums: false,
      rolesEnabled: false,
    });

    expect(built.coverPage).toBeNull();
    expect(built.webhooks).toBeNull();
    expect(built.renderComments).toBeNull();
    expect(built.chapters).toBeNull();
    expect(built.metadataFields).toBeNull();
    expect(built.numberedListStyles).toBeNull();
    expect(built.language).toBeNull();
    expect(built.linkedWorkitemRoles).toEqual([]);
    // No roles selected means no direction to apply either
    expect(built.linkRoleDirection).toBeNull();
  });

  it('sends the language custom field whatever is exported, and whether or not enums are localized', () => {
    // The two language fields answer different questions, so neither guard on `language` applies here: the
    // server reads the document's own field with this one, rather than localizing enums into a chosen
    // language. So unlike "Localize enums" - which a collection shows and then does not send - nothing
    // narrows it, and the legacy dialogs set it unconditionally for the same reason.
    const form = {
      ...toExportForm(SAMPLE_STYLE_PACKAGE_FULL),
      languageCustomField: 'docLanguage',
      localizeEnums: false,
    };
    const types: DocumentType[] = ['LIVE_DOC', 'LIVE_REPORT', 'TEST_RUN', 'BASELINE_COLLECTION', 'WIKI_PAGE'];

    for (const documentType of types) {
      const built = params(form, { documentType, exportType: 'SINGLE' });
      expect(built.languageCustomField).toBe('docLanguage');
      expect(built.language).toBeNull();
    }

    // A package naming none is left out of the body rather than sent as null, as every unset field is
    expect(JSON.parse(toRequestBody(params()))).not.toHaveProperty('languageCustomField');
  });

  it('reflects the work items query in the URL parameters the renderer reads the document with', () => {
    const form = toExportForm(SAMPLE_STYLE_PACKAGE_FULL);
    const context = { ...SAMPLE_DOCUMENT, urlQueryParameters: { revision: '42', query: 'type:task' } };

    const on = buildExportParams(form, context, LIVE_DOC);
    expect('params' in on && on.params.urlQueryParameters).toEqual({ revision: '42', query: 'type:requirement' });

    const off = buildExportParams({ ...form, workItemsQueryEnabled: false }, context, LIVE_DOC);
    // Dropped, not emptied: an empty query is a filter that matches nothing
    expect('params' in off && off.params.urlQueryParameters).toEqual({ revision: '42' });
  });

  it('refuses to build on a bad chapters entry, and says which field it is', () => {
    const form = { ...toExportForm(SAMPLE_STYLE_PACKAGE_FULL), specificChapters: 'x' };

    const built = buildExportParams(form, SAMPLE_DOCUMENT, LIVE_DOC);

    expect('error' in built && built.error).toEqual({ field: 'chapters', message: CHAPTERS_ERROR });
  });

  it('refuses to build on a bad numbered list styles entry', () => {
    const form = { ...toExportForm(SAMPLE_STYLE_PACKAGE_FULL), customNumberedListStyles: 'zz' };

    const built = buildExportParams(form, SAMPLE_DOCUMENT, LIVE_DOC);

    expect('error' in built && built.error.field).toBe('numberedListStyles');
  });

  it('exports an empty metadata fields entry rather than refusing it, as the legacy panel did', () => {
    // The legacy check was against an array that is never falsy, so it never fired. Reproducing the panel
    // means reproducing that: an empty entry exports as `metadataFields: []`.
    const built = params({ ...toExportForm(SAMPLE_STYLE_PACKAGE_FULL), metadataFields: '' });

    expect(built.metadataFields).toEqual([]);
  });

  it('leaves out of the body what is not set, which is not the same as sending null', () => {
    const body = JSON.parse(toRequestBody(params(undefined, { ...LIVE_DOC, fileName: 'Doc.pdf' }))) as Record<
      string,
      unknown
    >;

    expect(body.fileName).toBe('Doc.pdf');
    expect(body).not.toHaveProperty('baselineRevision');

    const withoutCoverPage = JSON.parse(
      toRequestBody(params({ ...toExportForm(SAMPLE_STYLE_PACKAGE_FULL), coverPageEnabled: false })),
    ) as Record<string, unknown>;
    expect(withoutCoverPage).not.toHaveProperty('coverPage');
  });
});
