import { SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import type { SelectOption } from '@sbb-polarion/react-sbb-polarion';
import {
  COMMENTS_RENDER_TYPES,
  type ChildNames,
  type ChildSetting,
  FULL_FONTS_HELP,
  IMAGE_DENSITIES,
  LANGUAGES,
  LINK_ROLE_DIRECTIONS,
  NATIVE_COMMENTS_HELP,
  ORIENTATIONS,
  PAPER_SIZES,
  PDF_VARIANTS,
  UNREFERENCED_COMMENTS_HELP,
} from '../services/stylePackage';
import type { ExportFieldName } from './documentType';
import type { ExportForm } from './exportForm';
import { childValue } from './exportForm';
import type { ExportField } from './exportParams';
import { FieldCell, FieldRow, SwitchRow } from './formRows';
import { reportStickyNotes } from './reporting';

const WORK_ITEMS_QUERY_HELP =
  "Lucene query applied to filter work items within the document, e.g. 'type:requirement'. Leave empty to " +
  'include all work items.';

export interface StylePackageSettingsViewProps {
  /** What every element id is prefixed with: `popup-` in the dialog, nothing in the panel and on the administration page. */
  ids: string;
  form: ExportForm;
  onPatch: (values: Partial<ExportForm>) => void;
  /** The named configurations the child dropdowns offer, null until they are read. */
  childNames: ChildNames | null;
  childNamesLoading?: boolean;
  /** The link roles of the project. None, and not loading, means the roles row is not offered. */
  roles: SelectOption[];
  rolesLoading?: boolean;
  webhooksEnabled: boolean;
  /** Whether a row is offered: an export asks the document type, the administration page offers every row. */
  shows: (field: ExportFieldName) => boolean;
  busy: boolean;
  /** The field an export was refused on, which is then marked. */
  invalidField?: ExportField | null;
}

/**
 * The settings of a style package, laid out as rows against one set of columns (see export-form-layout.css).
 *
 * The "Export to PDF" dialog and the Document Properties side panel render them inside the export form, where a
 * package exposes its settings, and the Style Packages administration page renders them as the package itself
 * (#1178). One markup is what keeps the three looking the same: the page used to be laid out by hand, apart
 * from the two others, and drifted from them.
 */
export default function StylePackageSettingsView({
  ids,
  form,
  onPatch,
  childNames,
  childNamesLoading,
  roles,
  rolesLoading,
  webhooksEnabled,
  shows,
  busy,
  invalidField,
}: Readonly<StylePackageSettingsViewProps>) {
  const id = (name: string): string => `${ids}${name}`;
  const childOptions = (setting: ChildSetting): SelectOption[] => childNames?.[setting] ?? [];
  const rolesShown = shows('roles') && (roles.length > 0 || !!rolesLoading);
  const valueFieldsShown = shows('specificChapters') || shows('workItemsQuery') || shows('metadataFields');

  return (
    <>
      {/* The named configurations the package points at. */}
      <div className="pdf-section">
        <SwitchRow
          id={id('cover-page-checkbox')}
          label="Cover page:"
          checked={form.coverPageEnabled}
          onChange={(checked) => onPatch({ coverPageEnabled: checked })}
        >
          <FieldCell shown={form.coverPageEnabled}>
            <SearchableSelect
              id={id('cover-page-selector')}
              ariaLabel="Cover page"
              options={childOptions('cover-page')}
              loading={childNamesLoading}
              value={childValue(childOptions('cover-page'), form.coverPage)}
              onChange={(value) => onPatch({ coverPage: value })}
              disabled={busy}
            />
          </FieldCell>
        </SwitchRow>
        <FieldRow label="CSS:" labelFor={id('css-selector')}>
          <FieldCell>
            <SearchableSelect
              id={id('css-selector')}
              options={childOptions('css')}
              loading={childNamesLoading}
              value={childValue(childOptions('css'), form.css)}
              onChange={(value) => onPatch({ css: value })}
              disabled={busy}
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Header/Footer:" labelFor={id('header-footer-selector')}>
          <FieldCell>
            <SearchableSelect
              id={id('header-footer-selector')}
              options={childOptions('header-footer')}
              loading={childNamesLoading}
              value={childValue(childOptions('header-footer'), form.headerFooter)}
              onChange={(value) => onPatch({ headerFooter: value })}
              disabled={busy}
            />
          </FieldCell>
        </FieldRow>
        <SwitchRow
          id={id('first-page-header-footer-checkbox')}
          label="First page header/footer:"
          checked={form.firstPageHeaderFooterEnabled}
          onChange={(checked) => onPatch({ firstPageHeaderFooterEnabled: checked })}
        >
          <FieldCell shown={form.firstPageHeaderFooterEnabled}>
            <SearchableSelect
              id={id('first-page-header-footer-selector')}
              ariaLabel="First page header/footer"
              options={childOptions('header-footer')}
              loading={childNamesLoading}
              value={childValue(childOptions('header-footer'), form.firstPageHeaderFooter)}
              onChange={(value) => onPatch({ firstPageHeaderFooter: value })}
              disabled={busy}
            />
          </FieldCell>
        </SwitchRow>
        <FieldRow label="Localization:" labelFor={id('localization-selector')}>
          <FieldCell>
            <SearchableSelect
              id={id('localization-selector')}
              options={childOptions('localization')}
              loading={childNamesLoading}
              value={childValue(childOptions('localization'), form.localization)}
              onChange={(value) => onPatch({ localization: value })}
              disabled={busy}
            />
          </FieldCell>
        </FieldRow>
        {webhooksEnabled && (
          <SwitchRow
            id={id('webhooks-checkbox')}
            label="Webhooks:"
            checked={form.webhooksEnabled}
            onChange={(checked) => onPatch({ webhooksEnabled: checked })}
          >
            <FieldCell shown={form.webhooksEnabled}>
              <SearchableSelect
                id={id('webhooks-selector')}
                ariaLabel="Webhooks"
                options={childOptions('webhooks')}
                loading={childNamesLoading}
                value={childValue(childOptions('webhooks'), form.webhooks)}
                onChange={(value) => onPatch({ webhooks: value })}
                disabled={busy}
              />
            </FieldCell>
          </SwitchRow>
        )}
      </div>

      {/* The page the PDF is laid out on: the colour picker on a line of its own, then the two pairs. */}
      <div className="pdf-section group-start">
        <FieldRow className="full-row" label="Headings color:" labelFor={id('headers-color')}>
          <FieldCell>
            <input
              id={id('headers-color')}
              type="color"
              value={form.headersColor}
              onChange={(event) => onPatch({ headersColor: event.target.value })}
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Paper size:" labelFor={id('paper-size-selector')}>
          <FieldCell>
            <SearchableSelect
              id={id('paper-size-selector')}
              options={PAPER_SIZES}
              value={form.paperSize}
              onChange={(value) => onPatch({ paperSize: value })}
              disabled={busy}
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Orientation:" labelFor={id('orientation-selector')}>
          <FieldCell>
            <SearchableSelect
              id={id('orientation-selector')}
              options={ORIENTATIONS}
              value={form.orientation}
              onChange={(value) => onPatch({ orientation: value })}
              disabled={busy}
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="PDF variant:" labelFor={id('pdf-variant-selector')}>
          <FieldCell>
            <SearchableSelect
              id={id('pdf-variant-selector')}
              options={PDF_VARIANTS}
              value={form.pdfVariant}
              onChange={(value) => onPatch({ pdfVariant: value })}
              disabled={busy}
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Image density:" labelFor={id('image-density-selector')}>
          <FieldCell>
            <SearchableSelect
              id={id('image-density-selector')}
              options={IMAGE_DENSITIES}
              value={form.imageDensity}
              onChange={(value) => onPatch({ imageDensity: value })}
              disabled={busy}
            />
          </FieldCell>
        </FieldRow>
      </div>

      {/* What the renderer does and does not carry over.

        The order is the one the two-column layout wants, and the rows flow across before they flow
        down: what reads as a list in a properties pane is two columns of five in a dialog, with the
        pairs that belong together ("cut empty chapters" / "cut empty Workitem attributes") side by
        side. Which is also why a row is never given a column: a document type that hides one of them
        (a report hides six) reflows the rest rather than leaving a hole. */}
      <div className="pdf-section group-start">
        <SwitchRow
          id={id('full-fonts')}
          label={
            <>
              Embed full fonts (no subsetting)
              <span className="more-info" title={FULL_FONTS_HELP} />
            </>
          }
          checked={form.fullFonts}
          onChange={(checked) => onPatch({ fullFonts: checked })}
        />
        {shows('fitToPage') && (
          <SwitchRow
            id={id('fit-to-page')}
            label="Fit images and tables to page"
            checked={form.fitToPage}
            onChange={(checked) => onPatch({ fitToPage: checked })}
          />
        )}
        <SwitchRow
          id={id('presentational-hints')}
          label="Follow HTML presentational hints"
          checked={form.followHTMLPresentationalHints}
          onChange={(checked) => onPatch({ followHTMLPresentationalHints: checked })}
        />
        <SwitchRow
          id={id('watermark')}
          label="Watermark"
          checked={form.watermark}
          onChange={(checked) => onPatch({ watermark: checked })}
        />
        {shows('cutEmptyChapters') && (
          <SwitchRow
            id={id('cut-empty-chapters')}
            label="Cut empty chapters (any level)"
            checked={form.cutEmptyChapters}
            onChange={(checked) => onPatch({ cutEmptyChapters: checked })}
          />
        )}
        {shows('cutEmptyWorkitemAttributes') && (
          <SwitchRow
            id={id('cut-empty-wi-attributes')}
            label="Cut empty Workitem attributes"
            checked={form.cutEmptyWorkitemAttributes}
            onChange={(checked) => onPatch({ cutEmptyWorkitemAttributes: checked })}
          />
        )}
        <SwitchRow
          id={id('cut-urls')}
          label="Cut local Polarion URLs"
          checked={form.cutLocalURLs}
          onChange={(checked) => onPatch({ cutLocalURLs: checked })}
        />
        {shows('markReferencedWorkitems') && (
          <SwitchRow
            id={id('mark-referenced-workitems')}
            label="Mark referenced Workitems"
            checked={form.markReferencedWorkitems}
            onChange={(checked) => onPatch({ markReferencedWorkitems: checked })}
          />
        )}
        {shows('customListStyles') && (
          <SwitchRow
            className="tight"
            id={id('custom-list-styles')}
            label="Custom styles of numbered lists"
            checked={form.customListStylesEnabled}
            onChange={(checked) => onPatch({ customListStylesEnabled: checked })}
          >
            <FieldCell grows shown={form.customListStylesEnabled}>
              <input
                id={id('numbered-list-styles')}
                className={invalidField === 'numberedListStyles' ? 'error' : undefined}
                type="text"
                placeholder="eg. 1ai"
                value={form.customNumberedListStyles}
                onChange={(event) => onPatch({ customNumberedListStyles: event.target.value })}
              />
            </FieldCell>
          </SwitchRow>
        )}
        {shows('localizeEnums') && (
          <SwitchRow
            id={id('localization')}
            label="Localize enums"
            checked={form.localizeEnums}
            onChange={(checked) => onPatch({ localizeEnums: checked })}
          >
            <FieldCell shown={form.localizeEnums}>
              <SearchableSelect
                id={id('language')}
                ariaLabel="Language"
                options={LANGUAGES}
                value={form.language}
                onChange={(value) => onPatch({ language: value })}
                disabled={busy}
              />
            </FieldCell>
          </SwitchRow>
        )}
      </div>

      {/* How comments are rendered, and the two options that go with it - a section of its own, since
        those options belong under the row rather than beside it. */}
      {shows('renderComments') && (
        <div className="pdf-section group-start">
          <SwitchRow
            className="full-row"
            id={id('render-comments')}
            label="Comments rendering"
            checked={form.renderCommentsEnabled}
            onChange={(checked) => onPatch({ renderCommentsEnabled: checked })}
          >
            <FieldCell shown={form.renderCommentsEnabled}>
              <SearchableSelect
                id={id('render-comments-selector')}
                ariaLabel="Comments rendering"
                options={COMMENTS_RENDER_TYPES}
                value={form.renderComments}
                onChange={(value) => onPatch({ renderComments: value })}
                disabled={busy}
              />
            </FieldCell>
          </SwitchRow>
          {form.renderCommentsEnabled && (
            <div className="property-wrapper sub-row" id={id('render-comments-options')}>
              <FieldCell wide>
                <div className="option-pair">
                  <label htmlFor={id('include-unreferenced-comments')} title={UNREFERENCED_COMMENTS_HELP}>
                    <input
                      id={id('include-unreferenced-comments')}
                      type="checkbox"
                      checked={form.includeUnreferencedComments}
                      onChange={(event) => onPatch({ includeUnreferencedComments: event.target.checked })}
                    />
                    include unreferenced
                  </label>
                  <label htmlFor={id('render-native-comments')} title={NATIVE_COMMENTS_HELP}>
                    <input
                      id={id('render-native-comments')}
                      type="checkbox"
                      checked={form.renderNativeComments}
                      onChange={(event) => {
                        onPatch({ renderNativeComments: event.target.checked });
                        // Sticky notes are not a PDF/A construct, so the form says so as soon as
                        // they are asked for rather than after a non-compliant file was produced.
                        reportStickyNotes(event.target.checked);
                      }}
                    />
                    as sticky notes
                  </label>
                </div>
              </FieldCell>
            </div>
          )}
        </div>
      )}

      {/* What to export of the document, rather than how to render it: the three switches that carry a
        value the user types. One column whatever the form's width, so each of them has the whole
        width for its field and the three read as a list. */}
      {valueFieldsShown && (
        <div className="pdf-section single-column group-start">
          {shows('specificChapters') && (
            <SwitchRow
              className="tight"
              id={id('specific-chapters')}
              label="Specific higher level chapters"
              checked={form.specificChaptersEnabled}
              onChange={(checked) => onPatch({ specificChaptersEnabled: checked })}
            >
              <FieldCell grows shown={form.specificChaptersEnabled}>
                <input
                  id={id('chapters')}
                  className={invalidField === 'chapters' ? 'error' : undefined}
                  type="text"
                  placeholder="eg. 1,2,4 etc."
                  value={form.specificChapters}
                  onChange={(event) => onPatch({ specificChapters: event.target.value })}
                />
              </FieldCell>
            </SwitchRow>
          )}
          {shows('workItemsQuery') && (
            <SwitchRow
              id={id('work-items-query')}
              label={
                <>
                  Work items query
                  <span className="more-info" title={WORK_ITEMS_QUERY_HELP} />
                </>
              }
              checked={form.workItemsQueryEnabled}
              onChange={(checked) => onPatch({ workItemsQueryEnabled: checked })}
            >
              <FieldCell grows shown={form.workItemsQueryEnabled}>
                <input
                  id={id('work-items-query-input')}
                  type="text"
                  title={WORK_ITEMS_QUERY_HELP}
                  placeholder="e.g. type:requirement"
                  value={form.workItemsQuery}
                  onChange={(event) => onPatch({ workItemsQuery: event.target.value })}
                />
              </FieldCell>
            </SwitchRow>
          )}
          {shows('metadataFields') && (
            <SwitchRow
              id={id('metadata-fields')}
              label="Metadata fields"
              checked={form.metadataFieldsEnabled}
              onChange={(checked) => onPatch({ metadataFieldsEnabled: checked })}
            >
              <FieldCell grows shown={form.metadataFieldsEnabled}>
                <input
                  id={id('metadata-fields-input')}
                  type="text"
                  placeholder="e.g. docOwner, docLanguage, customField*"
                  value={form.metadataFields}
                  onChange={(event) => onPatch({ metadataFields: event.target.value })}
                />
              </FieldCell>
            </SwitchRow>
          )}
        </div>
      )}

      {/* Which work items a document's links pull in, which is a question of its own - and two controls
        wide, so it takes a line to itself between the hairlines rather than a place in a list. */}
      {rolesShown && (
        <div className="pdf-section single-column group-start">
          <SwitchRow
            rowId={id('roles-wrapper')}
            id={id('selected-roles')}
            label="Specific Workitem roles"
            checked={form.rolesEnabled}
            onChange={(checked) => onPatch({ rolesEnabled: checked })}
          >
            {form.rolesEnabled && (
              <FieldCell grows>
                {/* Two controls in one cell, side by side while the form has room for both. */}
                <div className="option-pair">
                  <SearchableSelect
                    id={id('roles-selector')}
                    ariaLabel="Workitem roles"
                    multiple
                    options={roles}
                    loading={rolesLoading}
                    value={form.linkedWorkitemRoles}
                    onChange={(values) => onPatch({ linkedWorkitemRoles: values })}
                    disabled={busy}
                  />
                  <SearchableSelect
                    id={id('roles-direction-selector')}
                    ariaLabel="Link role direction"
                    options={LINK_ROLE_DIRECTIONS}
                    value={form.linkRoleDirection}
                    onChange={(value) => onPatch({ linkRoleDirection: value })}
                    disabled={busy}
                  />
                </div>
              </FieldCell>
            )}
          </SwitchRow>
        </div>
      )}
    </>
  );
}
