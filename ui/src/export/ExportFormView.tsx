import { type ReactNode, type RefObject, useEffect, useRef } from 'react';
import { SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import type { SelectOption } from '@sbb-polarion/react-sbb-polarion';
import validateIcon from '../assets/validate.svg';
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
import type { DocumentType, ExportFieldName, ExportType } from './documentType';
import {
  isAutoSelectStylePackageAvailable,
  isFieldVisible,
  isFileNameOffered,
  isPageWidthValidationOffered,
} from './documentType';
import type { ExportForm } from './exportForm';
import { childValue } from './exportForm';
import type { ExportField } from './exportParams';
import { FieldCell, FieldRow, SwitchRow, TextFieldRow } from './formRows';
import { MAX_PAGE_PREVIEWS, STICKY_NOTES_WARNING, type WidthValidationResult, invalidPagesSummary } from './reporting';

/** The option lists an export form offers, whoever read them. Both `PopupData` and `PanelData` are one. */
export interface ExportFormData {
  stylePackages: SelectOption[];
  childNames: ChildNames;
  /** Empty where link roles do not apply, in which case the roles row is not offered at all. */
  roles: SelectOption[];
  webhooksEnabled: boolean;
}

/** What the form says about what just happened. One block, directly above the button that started it. */
export interface ExportFormAlerts {
  warning: string | null;
  error: string | null;
  success: string | null;
}

/** The page width validation: what it is offered for, what it answered, and how to run it again. */
export interface PageWidthValidation {
  /** The style package's `exposePageWidthValidation`; a bulk export never offers it whatever it says. */
  exposed: boolean;
  onRun: () => void;
  disabled: boolean;
  /** A validation is running. The panel says so beside the button; the dialog covers the form instead. */
  running?: boolean;
  /** Why the button is off, where there is a reason worth naming (the panel's export permission). */
  title?: string;
  /** Said where every page fits. */
  ok: string | null;
  /** The pages that did not, and the work items likely behind them. */
  result: WidthValidationResult | null;
  /** Which preview is open, if any. */
  zoomed: number | null;
  onZoom: (index: number | null) => void;
}

export interface ExportFormViewProps {
  /**
   * What every element id in the form is prefixed with (`popup-` in the dialog, nothing in the panel).
   *
   * The two surfaces have always had ids of their own, and the injectors, the visual references and the
   * suites address them by those. Nothing depends on them being different - each form is alone in its
   * shadow root - so this is the one thing the shared markup is parameterized by rather than unified.
   */
  ids: string;
  documentType: DocumentType;
  exportType: ExportType;
  /** Null until the option lists have been read; the form then shows what it can and no dropdown options. */
  data: ExportFormData | null;
  stylePackage: string;
  onStylePackage: (name: string) => void;
  /** Bulk over documents or collections: the server picks the best package per item, so none is chosen here. */
  autoSelect: boolean;
  onAutoSelect: (autoSelect: boolean) => void;
  /** Null until a style package has been read into it. */
  form: ExportForm | null;
  onPatch: (values: Partial<ExportForm>) => void;
  /** Whether the selected style package invites the user to redefine its settings. */
  exposeSettings: boolean;
  fileName: string;
  onFileName: (fileName: string) => void;
  /** The field an export was refused on, which is then marked. */
  invalidField: ExportField | null;
  /** Something is running: every control is out of reach until it is done. */
  busy: boolean;
  alerts: ExportFormAlerts;
  /** Raised by "as sticky notes", which is not a PDF/A construct - the form says so as it is asked for. */
  onWarning: (warning: string | null) => void;
  validation: PageWidthValidation;
  /** The surface's own action area: the panel's "Export to PDF" button. The dialog's are its footer. */
  actions?: ReactNode;
  /** Covers the form while the surface is busy: the dialog's in-progress overlay. */
  overlay?: ReactNode;
  /** The form element, which is what locates the dialog around it - see popup/dialogPortals.ts. */
  formRef?: RefObject<HTMLDivElement | null>;
}

/**
 * The export form: everything the "Export to PDF" dialog and the Document Properties side panel have in
 * common, which is all of it but the chrome.
 *
 * The two used to be two copies of the same form - the same rows, the same style package, the same
 * validation, the same request - laid out differently by hand: the dialog in two fixed flex columns, the
 * panel in one, each with its own label widths, its own way of hiding an optional field and its own way of
 * reporting an error. They render this now, and differ only in what surrounds it: RSP's `Modal` with its
 * footer, or a `<fieldset>` in the properties pane with an "Export to PDF" button of its own (see
 * {@link ExportFormViewProps.actions}).
 *
 * The layout is one row model against one set of columns, and it follows the room the form has rather than
 * which surface it is: a section is one column in a 360px pane and two in a 700px dialog, decided by a
 * container query on this element. See `export-form.css`.
 *
 * Which rows a document type shows is `documentType.ts` rather than the `visible-for-*` classes the legacy
 * markup switched by hand, and it is asked here for both surfaces - the panel exports a Live Document, for
 * which the answer is "all of them", so it needs no branch of its own.
 */
export default function ExportFormView({
  ids,
  documentType,
  exportType,
  data,
  stylePackage,
  onStylePackage,
  autoSelect,
  onAutoSelect,
  form,
  onPatch,
  exposeSettings,
  fileName,
  onFileName,
  invalidField,
  busy,
  alerts,
  onWarning,
  validation,
  actions,
  overlay,
  formRef,
}: Readonly<ExportFormViewProps>) {
  const id = (name: string): string => `${ids}${name}`;
  const shows = (field: ExportFieldName): boolean => isFieldVisible(field, documentType);
  const childOptions = (setting: ChildSetting): SelectOption[] => data?.childNames[setting] ?? [];

  const autoSelectAvailable = isAutoSelectStylePackageAvailable(documentType, exportType);
  const autoSelected = autoSelectAvailable && autoSelect;
  /** The settings block is offered only where a package exposes them and no automatic pick is in effect. */
  const settingsShown = !!form && exposeSettings && !autoSelected;
  const validationShown = isPageWidthValidationOffered(exportType, validation.exposed);
  const rolesShown = shows('roles') && !!data && data.roles.length > 0;
  const textFieldsShown =
    shows('specificChapters') || shows('metadataFields') || shows('workItemsQuery') || shows('customListStyles');

  const result = validation.result;
  const previews = result?.invalidPages.slice(0, MAX_PAGE_PREVIEWS) ?? [];

  // The alerts sit next to the button that produced them, which on both surfaces is the bottom of the
  // form. That is only where the eye is if the form is short enough to be on screen whole, so a form that
  // scrolls (a tall dialog, a properties pane) brings them into view rather than reporting into nothing.
  const alertsRef = useRef<HTMLDivElement>(null);
  const announced = alerts.error || alerts.warning || alerts.success;
  useEffect(() => {
    if (announced) {
      alertsRef.current?.scrollIntoView({ block: 'nearest' });
    }
  }, [announced]);

  return (
    <div className="pdf-export-form" ref={formRef}>
      {overlay}

      {autoSelectAvailable && (
        <SwitchRow
          rowId={id('auto-select-style-package-container')}
          id={id('auto-select-style-package')}
          label="Automatically select a style package most suitable for each of the documents to be exported"
          checked={autoSelect}
          onChange={onAutoSelect}
        />
      )}

      {!autoSelected && (
        <>
          <p>Select one of style packages in dropdown below which you wish to use during export.</p>
          <FieldRow label="Style package:" labelFor={id('style-package-select')}>
            <FieldCell>
              <SearchableSelect
                id={id('style-package-select')}
                options={data?.stylePackages ?? []}
                value={stylePackage}
                onChange={onStylePackage}
                disabled={busy}
              />
            </FieldCell>
          </FieldRow>
        </>
      )}

      {settingsShown && form && (
        <div id={id('style-package-content')} className="settings-block group-start">
          <p>Selected style package exposes its settings, so you can redefine them.</p>

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
                  options={childOptions('cover-page')}
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
                  value={childValue(childOptions('header-footer'), form.headerFooter)}
                  onChange={(value) => onPatch({ headerFooter: value })}
                  disabled={busy}
                />
              </FieldCell>
            </FieldRow>
            <FieldRow label="Localization:" labelFor={id('localization-selector')}>
              <FieldCell>
                <SearchableSelect
                  id={id('localization-selector')}
                  options={childOptions('localization')}
                  value={childValue(childOptions('localization'), form.localization)}
                  onChange={(value) => onPatch({ localization: value })}
                  disabled={busy}
                />
              </FieldCell>
            </FieldRow>
            {data?.webhooksEnabled && (
              <SwitchRow
                id={id('webhooks-checkbox')}
                label="Webhooks:"
                checked={form.webhooksEnabled}
                onChange={(checked) => onPatch({ webhooksEnabled: checked })}
              >
                <FieldCell shown={form.webhooksEnabled}>
                  <SearchableSelect
                    id={id('webhooks-selector')}
                    options={childOptions('webhooks')}
                    value={childValue(childOptions('webhooks'), form.webhooks)}
                    onChange={(value) => onPatch({ webhooks: value })}
                    disabled={busy}
                  />
                </FieldCell>
              </SwitchRow>
            )}
          </div>

          {/* The page the PDF is laid out on. */}
          <div className="pdf-section group-start">
            <FieldRow label="Headings color:" labelFor={id('headers-color')}>
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

          {/* What the renderer does and does not carry over. Switches only, so two of them fit a line. */}
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
                    options={LANGUAGES}
                    value={form.language}
                    onChange={(value) => onPatch({ language: value })}
                    disabled={busy}
                  />
                </FieldCell>
              </SwitchRow>
            )}
          </div>

          {/* The two switches whose choices need a line of their own, so they take a full row each. */}
          {(shows('renderComments') || rolesShown) && (
            <div className="pdf-section group-start">
              {shows('renderComments') && (
                <>
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
                                onWarning(event.target.checked ? STICKY_NOTES_WARNING : null);
                              }}
                            />
                            as sticky notes
                          </label>
                        </div>
                      </FieldCell>
                    </div>
                  )}
                </>
              )}

              {rolesShown && data && (
                <>
                  <SwitchRow
                    className="full-row"
                    id={id('selected-roles')}
                    label="Specific Workitem roles"
                    checked={form.rolesEnabled}
                    onChange={(checked) => onPatch({ rolesEnabled: checked })}
                  />
                  {form.rolesEnabled && (
                    <div className="property-wrapper sub-row" id={id('roles-wrapper')}>
                      <FieldCell wide>
                        <div className="option-pair">
                          <SearchableSelect
                            id={id('roles-selector')}
                            multiple
                            options={data.roles}
                            value={form.linkedWorkitemRoles}
                            onChange={(values) => onPatch({ linkedWorkitemRoles: values })}
                            disabled={busy}
                          />
                          <SearchableSelect
                            id={id('roles-direction-selector')}
                            options={LINK_ROLE_DIRECTIONS}
                            value={form.linkRoleDirection}
                            onChange={(value) => onPatch({ linkRoleDirection: value })}
                            disabled={busy}
                          />
                        </div>
                      </FieldCell>
                    </div>
                  )}
                </>
              )}
            </div>
          )}

          {/* The switches whose text is too long for the label column - see `.pdf-fields`. */}
          {textFieldsShown && (
            <div className="pdf-fields group-start">
              {shows('specificChapters') && (
                <TextFieldRow
                  id={id('specific-chapters')}
                  label="Specific higher level chapters"
                  checked={form.specificChaptersEnabled}
                  onChange={(checked) => onPatch({ specificChaptersEnabled: checked })}
                >
                  <input
                    id={id('chapters')}
                    className={invalidField === 'chapters' ? 'error' : undefined}
                    type="text"
                    placeholder="eg. 1,2,4 etc."
                    value={form.specificChapters}
                    onChange={(event) => onPatch({ specificChapters: event.target.value })}
                  />
                </TextFieldRow>
              )}
              {shows('metadataFields') && (
                <TextFieldRow
                  id={id('metadata-fields')}
                  label="Metadata fields"
                  checked={form.metadataFieldsEnabled}
                  onChange={(checked) => onPatch({ metadataFieldsEnabled: checked })}
                >
                  <input
                    id={id('metadata-fields-input')}
                    type="text"
                    placeholder="e.g. docOwner, docLanguage, customField*"
                    value={form.metadataFields}
                    onChange={(event) => onPatch({ metadataFields: event.target.value })}
                  />
                </TextFieldRow>
              )}
              {shows('workItemsQuery') && (
                <TextFieldRow
                  id={id('work-items-query')}
                  label="Work items query"
                  checked={form.workItemsQueryEnabled}
                  onChange={(checked) => onPatch({ workItemsQueryEnabled: checked })}
                >
                  <input
                    id={id('work-items-query-input')}
                    type="text"
                    title="Lucene query applied to filter work items within the document, e.g. 'type:requirement'."
                    placeholder="e.g. type:requirement"
                    value={form.workItemsQuery}
                    onChange={(event) => onPatch({ workItemsQuery: event.target.value })}
                  />
                </TextFieldRow>
              )}
              {shows('customListStyles') && (
                <TextFieldRow
                  id={id('custom-list-styles')}
                  label="Custom styles of numbered lists"
                  checked={form.customListStylesEnabled}
                  onChange={(checked) => onPatch({ customListStylesEnabled: checked })}
                >
                  <input
                    id={id('numbered-list-styles')}
                    className={invalidField === 'numberedListStyles' ? 'error' : undefined}
                    type="text"
                    placeholder="eg. 1ai"
                    value={form.customNumberedListStyles}
                    onChange={(event) => onPatch({ customNumberedListStyles: event.target.value })}
                  />
                </TextFieldRow>
              )}
            </div>
          )}

          {/* A test run's attachments, which no other document type has. */}
          {shows('testRunAttachments') && (
            <div className="pdf-section group-start">
              <SwitchRow
                id={id('download-attachments')}
                label="Download attachments"
                checked={form.downloadAttachments}
                onChange={(checked) => onPatch({ downloadAttachments: checked })}
              />
              {form.downloadAttachments && (
                <>
                  <SwitchRow
                    rowId={id('embed-attachments-container')}
                    id={id('embed-attachments')}
                    label="Embed attachments into resulted PDF"
                    checked={form.embedAttachments}
                    onChange={(checked) => onPatch({ embedAttachments: checked })}
                  />
                  <FieldRow
                    rowId={id('attachments-filter-container')}
                    label="Attachments filter"
                    labelFor={id('attachments-filter')}
                  >
                    <FieldCell grows>
                      <input
                        id={id('attachments-filter')}
                        type="text"
                        title="Filter for attachments to be downloaded, example: '*.pdf'"
                        placeholder="*.*"
                        value={form.attachmentsFilter}
                        onChange={(event) => onPatch({ attachmentsFilter: event.target.value })}
                      />
                    </FieldCell>
                  </FieldRow>
                  <FieldRow
                    rowId={id('testcase-field-id-container')}
                    label="Custom field ID"
                    labelFor={id('testcase-field-id')}
                    title="A boolean testcase field ID. Attachments will be downloaded only from the testcases which have True value in the provided field. Leaving field empty will process all testcases."
                  >
                    <FieldCell grows>
                      <input
                        id={id('testcase-field-id')}
                        type="text"
                        value={form.testcaseFieldId}
                        onChange={(event) => onPatch({ testcaseFieldId: event.target.value })}
                      />
                    </FieldCell>
                  </FieldRow>
                </>
              )}
            </div>
          )}
        </div>
      )}

      {isFileNameOffered(exportType) && (
        <FieldRow rowId={id('filename-wrapper')} label="File name:" labelFor={id('filename')}>
          <FieldCell grows>
            <input
              id={id('filename')}
              type="text"
              value={fileName}
              onChange={(event) => onFileName(event.target.value)}
            />
          </FieldCell>
        </FieldRow>
      )}

      {/* Only where there is something to say: an empty block would keep its padding above the buttons. */}
      {announced && (
        <div className="notifications" ref={alertsRef}>
          {alerts.warning && (
            <div id={id('export-warning')} className="alert alert-warning">
              {alerts.warning}
            </div>
          )}
          {alerts.error && (
            <div id={id('export-error')} className="alert alert-error">
              {alerts.error}
            </div>
          )}
          {alerts.success && (
            <div id={id('export-success')} className="alert alert-success">
              {alerts.success}
            </div>
          )}
        </div>
      )}

      {actions}

      {validationShown && (
        <div className="buttons-wrapper" id={id('page-width-validation')}>
          <button
            type="button"
            id={id('validate-pdf')}
            disabled={validation.disabled}
            title={validation.title}
            onClick={validation.onRun}
          >
            <img src={validateIcon} alt="" />
            Validate pages width
          </button>
          <span
            id={id('validate-pdf-progress')}
            className="sbb-spinner"
            role="img"
            aria-label="Loading"
            style={validation.running ? { display: 'inline-block' } : undefined}
          />
          {/* The result of a validation run. A validation that could not be run at all is reported in the
              notifications above instead, next to a refused export. */}
          <div className="validation-alerts">
            {validation.ok && (
              <div id={id('validate-ok')} className="alert alert-success">
                {validation.ok}
              </div>
            )}
            {result && (
              <div id={id('validate-error')} className="alert alert-error">
                {invalidPagesSummary(result.invalidPages.length)}
              </div>
            )}
          </div>
        </div>
      )}

      {result && (
        <>
          <div id={id('page-previews')} className="preview">
            {previews.map((page, index) => (
              <img
                // The previews have no identity of their own beyond their position in the answer.
                key={index}
                className={validation.zoomed === index ? 'validate-result-img img-zoomed' : 'validate-result-img'}
                src={`data:image/png;base64,${page.content}`}
                alt=""
                onClick={() => validation.onZoom(validation.zoomed === index ? null : index)}
              />
            ))}
          </div>
          {result.suspiciousWorkItems.length > 0 && (
            <div id={id('suspicious-wi')} className="suspicious-wi">
              Suspicious work items:
              <ul className="suspicious-list">
                {result.suspiciousWorkItems.map((item) => (
                  <li key={item.id}>
                    <a href={item.link} target="_blank" rel="noreferrer">
                      {item.id}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </div>
  );
}
