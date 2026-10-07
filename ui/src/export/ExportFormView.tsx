import { type ReactNode, type RefObject } from 'react';
import { Modal, SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import type { SelectOption } from '@sbb-polarion/react-sbb-polarion';
import validateIcon from '../assets/validate.svg';
import type { ChildNames } from '../services/stylePackage';
import StylePackageSettingsView from './StylePackageSettingsView';
import type { DocumentType, ExportFieldName, ExportType } from './documentType';
import {
  isAutoSelectStylePackageAvailable,
  isFieldVisible,
  isFileNameOffered,
  isPageWidthValidationOffered,
} from './documentType';
import type { ExportForm } from './exportForm';
import type { ExportField } from './exportParams';
import { FieldCell, FieldRow, SwitchRow } from './formRows';
import { MAX_PAGE_PREVIEWS, type WidthValidationResult, invalidPagesSummary } from './reporting';

/** The option lists an export form offers, whoever read them. Both `PopupData` and `PanelData` are one. */
export interface ExportFormData {
  stylePackages: SelectOption[];
  childNames: ChildNames;
  /** Empty where link roles do not apply, in which case the roles row is not offered at all. */
  roles: SelectOption[];
  webhooksEnabled: boolean;
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

/** The bulk-only "merge into a single PDF" option, offered only when the merge service is available. */
export interface MergeOption {
  available: boolean;
  into: boolean;
  onInto: (checked: boolean) => void;
  fileName: string;
  onFileName: (name: string) => void;
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
  /**
   * Why the form cannot be used, where that is the case: the option lists or the style package behind it
   * could not be read.
   *
   * The one message that stays in the form. Everything else an export surface has to say is an event and is
   * reported as a toast (see `reporting.ts`); this is a state, and a toast that came and went would leave a
   * form that quietly does not work.
   */
  loadError: string | null;
  validation: PageWidthValidation;
  /** The bulk-only "merge into a single PDF" option; absent on the side panel, which never merges. */
  merge?: MergeOption;
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
  loadError,
  validation,
  merge,
  actions,
  overlay,
  formRef,
}: Readonly<ExportFormViewProps>) {
  const id = (name: string): string => `${ids}${name}`;
  const shows = (field: ExportFieldName): boolean => isFieldVisible(field, documentType);

  const autoSelectAvailable = isAutoSelectStylePackageAvailable(documentType, exportType);
  const autoSelected = autoSelectAvailable && autoSelect;
  /** The settings block is offered only where a package exposes them and no automatic pick is in effect. */
  const settingsShown = !!form && exposeSettings && !autoSelected;
  const validationShown = isPageWidthValidationOffered(exportType, validation.exposed);

  const result = validation.result;
  const previews = result?.invalidPages.slice(0, MAX_PAGE_PREVIEWS) ?? [];
  /** The preview the user opened, if any: the same page, shown at the size the window allows. */
  const opened = validation.zoomed === null ? undefined : previews[validation.zoomed];
  const closePreview = () => validation.onZoom(null);

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

          <StylePackageSettingsView
            ids={ids}
            form={form}
            onPatch={onPatch}
            childNames={data?.childNames ?? null}
            roles={data?.roles ?? []}
            webhooksEnabled={!!data?.webhooksEnabled}
            shows={shows}
            busy={busy}
            invalidField={invalidField}
          />

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
        <div className="pdf-section group-start">
          <FieldRow className="full-row" rowId={id('filename-wrapper')} label="File name:" labelFor={id('filename')}>
            <FieldCell grows>
              <input
                id={id('filename')}
                type="text"
                value={fileName}
                onChange={(event) => onFileName(event.target.value)}
              />
            </FieldCell>
          </FieldRow>
        </div>
      )}

      {/* Bulk over several documents can be combined into one file, where the merge service is up. A bulk
          export offers no plain file name (each item names its own), so this stands in its place. */}
      {exportType === 'BULK' && merge?.available && (
        <div className="pdf-section group-start">
          <SwitchRow
            id={id('merge-into-single-pdf')}
            label="Merge all documents into a single PDF"
            checked={merge.into}
            onChange={merge.onInto}
          />
          {merge.into && (
            <FieldRow
              className="full-row"
              rowId={id('merge-filename-wrapper')}
              label="Merged file name:"
              labelFor={id('merge-filename')}
            >
              <FieldCell grows>
                <input
                  id={id('merge-filename')}
                  type="text"
                  value={merge.fileName}
                  onChange={(event) => merge.onFileName(event.target.value)}
                />
              </FieldCell>
            </FieldRow>
          )}
        </div>
      )}

      {/* Only where there is something to say: an empty block would keep its padding above the buttons. */}
      {loadError && (
        <div className="notifications">
          <div id={id('load-error')} className="alert alert-error">
            {loadError}
          </div>
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
              <button
                // The previews have no identity of their own beyond their position in the answer.
                key={index}
                type="button"
                className="validate-result-img"
                onClick={() => validation.onZoom(index)}
              >
                <img
                  src={`data:image/png;base64,${page.content}`}
                  alt={`Invalid page ${index + 1}, open it enlarged`}
                />
              </button>
            ))}
          </div>
          {/* A preview opened, in the shared Modal: a page of a document deserves the frame a dialog gives it
              - a title saying which page it is, a close button in the header, Escape, and a backdrop over
              the form it was opened from. Nested inside the export dialog where that is what opened it,
              which a native `<dialog>` allows: the newer one joins the top layer above the older.

              `onOk` is required by the Modal and there is nothing to confirm here, so it closes as well -
              the footer it would sit in is hidden (see export-form.css), which leaves the header's own
              close button as the only one. */}
          {opened && (
            <Modal
              open
              title={`Invalid page ${(validation.zoomed ?? 0) + 1} of ${result.invalidPages.length}`}
              onOk={closePreview}
              onCancel={closePreview}
            >
              {/* A mouse shortcut only: the header's close button and Escape close it from the keyboard. */}
              <div className="preview-zoom" id={id('page-preview-zoom')} role="presentation" onClick={closePreview}>
                <img
                  src={`data:image/png;base64,${opened.content}`}
                  alt={`Invalid page ${(validation.zoomed ?? 0) + 1}`}
                />
              </div>
            </Modal>
          )}

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
