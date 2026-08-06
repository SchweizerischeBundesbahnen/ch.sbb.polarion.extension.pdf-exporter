import { type CSSProperties, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Modal, SearchableSelect } from '@grigoriev/react-sbb-polarion';
import type { SelectOption } from '@grigoriev/react-sbb-polarion';
import validateIcon from '../assets/validate.svg';
import type { DocumentType, ExportFieldName, ExportType } from '../export/documentType';
import {
  isAutoSelectStylePackageAvailable,
  isFieldVisible,
  isFileNameOffered,
  isPageWidthValidationOffered,
} from '../export/documentType';
import type { DocIdentifier, PopupData } from '../export/exportData';
import { loadPopupData, loadStylePackage } from '../export/exportData';
import type { ExportForm } from '../export/exportForm';
import { childValue, toExportForm } from '../export/exportForm';
import type { ExportField, ExportParamsJson } from '../export/exportParams';
import { buildExportParams, toRequestBody } from '../export/exportParams';
import { convertPdf, downloadBlob, downloadTestRunAttachments, errorMessageOf } from '../services/conversion';
import { getCookie, setCookie } from '../services/cookies';
import type { DocumentIdentity } from '../services/exportContext';
import {
  COMMENTS_RENDER_TYPES,
  FULL_FONTS_HELP,
  IMAGE_DENSITIES,
  LANGUAGES,
  LINK_ROLE_DIRECTIONS,
  NATIVE_COMMENTS_HELP,
  ORIENTATIONS,
  PAPER_SIZES,
  PDF_VARIANTS,
  type StylePackageSettings,
  UNREFERENCED_COMMENTS_HELP,
} from '../services/stylePackage';
import useRemote from '../services/useRemote';
import useDropdownPopupsInDialog from './dialogPortals';

/** How many invalid page previews the popup shows; the endpoint is asked for one more to detect "more". */
const MAX_PAGE_PREVIEWS = 4;

/** The style package the user picked last, offered again next time. The legacy popup's own cookie name. */
const SELECTED_STYLE_PACKAGE_COOKIE = 'selected-style-package';

const STICKY_NOTES_WARNING =
  'Be aware that comments rendered in PDF as sticky notes are not compliant with any of PDF/A variants';

const LOAD_ERROR = 'Error occurred loading form data';
const PACKAGE_LOAD_ERROR = 'Error occurred loading style package data';
const EXPORT_ERROR = 'Error occurred during PDF generation';
const VALIDATION_ERROR = 'Error occurred validating pages width';

interface WidthValidationResult {
  invalidPages: { content: string }[];
  suspiciousWorkItems: { id: string; link: string }[];
}

/**
 * What the popup hands back for a bulk export instead of converting anything itself.
 *
 * A bulk export is a run of per-item conversions driven by the Bulk PDF Export widget, which owns the
 * progress dialog; the popup only collects the parameters they all share. This is the React equivalent of
 * the legacy `bulkCallback.openPopup(exportParams)`.
 */
export type BulkExportStarter = (params: ExportParamsJson) => void;

/** What the popup reaches outside itself for, so the dev harness and the tests can replace it. */
export interface ExportPopupDependencies {
  loadData?: typeof loadPopupData;
  loadPackage?: typeof loadStylePackage;
  convert?: typeof convertPdf;
  download?: typeof downloadBlob;
  downloadAttachments?: typeof downloadTestRunAttachments;
}

export interface ExportPopupModalProps {
  /**
   * Where the item being exported lives, as the page URL says. Must be stable across renders - it is what
   * the dialog reads its data for, so a fresh object each render would restart that read.
   */
  document: DocumentIdentity;
  exportType?: ExportType;
  /** The items a bulk export was started for; a single export derives its one identifier from `document`. */
  identifiers?: DocIdentifier[];
  /** Where a bulk export goes once the parameters are in. Required for `exportType: 'BULK'`. */
  onBulkExport?: BulkExportStarter;
  onClose: () => void;
  deps?: ExportPopupDependencies;
}

/** `<prefix>` on its own line, then the detail - the legacy `prefix + ": " + message`. */
const withDetail = (prefix: string, detail: string): string => (detail ? `${prefix}: ${detail}` : prefix);

/** What a rejected read or conversion says, which is the server's message or nothing. */
const messageOf = (error: unknown): string => (error instanceof Error ? error.message : '');

/**
 * Reserves a control's space while hiding it, which is how every optional value field of this form behaved:
 * `visibility` rather than `display`, so ticking a checkbox does not reflow the column around it.
 */
const reserved = (visible: boolean): CSSProperties | undefined => (visible ? undefined : { visibility: 'hidden' });

/**
 * The "Export to PDF" dialog: the React port of `ExportPopup.js` + `popupForm.html`.
 *
 * Opened from four places - the document editor toolbar, the Live Report toolbar, the "Export to PDF Button"
 * report widget and the Bulk PDF Export widget - which is why it takes what it is exporting as props rather
 * than reading it itself, and why the bulk case hands its parameters back instead of converting.
 *
 * The chrome is RSP's shared `Modal` (a native `<dialog>`: the top layer, the backdrop and Escape for free),
 * so the micromodal library and stylesheet the legacy popup needed on the page are gone. What is this
 * extension's own is the form body, whose two-column layout comes from `export-popup.css` - injected into
 * the same shadow root the dialog is mounted in, see `mount.tsx`.
 *
 * Which rows a document type shows, and which of them the request then carries, is `export/documentType.ts`
 * rather than the `visible-for-*` classes this markup used to switch by hand.
 */
export default function ExportPopupModal({
  document: document_,
  exportType = 'SINGLE',
  identifiers,
  onBulkExport,
  onClose,
  deps,
}: Readonly<ExportPopupModalProps>) {
  const { sendRequest, sendAbsoluteRequest } = useRemote();
  const loadData = deps?.loadData ?? loadPopupData;
  const loadPackage = deps?.loadPackage ?? loadStylePackage;
  const convert = deps?.convert ?? convertPdf;
  const download = deps?.download ?? downloadBlob;
  const downloadAttachments = deps?.downloadAttachments ?? downloadTestRunAttachments;

  const remote = useMemo(() => ({ sendRequest, sendAbsoluteRequest }), [sendRequest, sendAbsoluteRequest]);

  const documentType: DocumentType = document_.documentType;
  const autoSelectAvailable = isAutoSelectStylePackageAvailable(documentType, exportType);
  const shows = (field: ExportFieldName): boolean => isFieldVisible(field, documentType);

  const [data, setData] = useState<PopupData | null>(null);
  const [stylePackage, setStylePackage] = useState('');
  /** Bulk over documents or collections: the server picks the best package per item, so none is chosen here. */
  const [autoSelect, setAutoSelect] = useState(true);
  const [exposeSettings, setExposeSettings] = useState(false);
  const [exposePageWidthValidation, setExposePageWidthValidation] = useState(false);
  const [form, setForm] = useState<ExportForm | null>(null);
  const [fileName, setFileName] = useState('');
  const [invalidField, setInvalidField] = useState<ExportField | null>(null);

  /** What the form is busy with, or null. One overlay for all four operations, as the legacy popup had. */
  const [progress, setProgress] = useState<string | null>('Loading form data');

  const [warning, setWarning] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [validationOk, setValidationOk] = useState<string | null>(null);
  const [validation, setValidation] = useState<WidthValidationResult | null>(null);
  const [zoomed, setZoomed] = useState<number | null>(null);

  /** Which package load is the current one; a slower earlier one must not overwrite it. */
  const latestPackage = useRef(0);

  /** The form, which is what locates the dialog around it - see {@link useDropdownPopupsInDialog}. */
  const form_ = useRef<HTMLDivElement>(null);
  useDropdownPopupsInDialog(form_);

  const busy = progress !== null;

  /**
   * The `?query=` of the page URL. The item is on screen filtered, so an export started here should match
   * it - which is why it takes priority over the style package's own work items query.
   */
  const urlQuery = document_.urlQueryParameters?.query;

  const clearAlerts = useCallback(() => {
    setWarning(null);
    setError(null);
    setSuccess(null);
    setValidationOk(null);
    setValidation(null);
    setZoomed(null);
    setInvalidField(null);
  }, []);

  // Everything the popup offers, read once. Any failure among these reads leaves the form unusable and says
  // so, which is what the legacy popup did: it showed this one message and never enabled its Export button.
  useEffect(() => {
    let cancelled = false;
    loadData(sendRequest, { documentType, exportType, document: document_, identifiers })
      .then((loaded) => {
        if (cancelled) return;
        setData(loaded);
        setFileName(loaded.fileName);
        // The package used last is offered again, as long as this selection still allows it.
        const remembered = getCookie(SELECTED_STYLE_PACKAGE_COOKIE);
        const preselected = loaded.stylePackages.some((option) => option.id === remembered)
          ? (remembered as string)
          : (loaded.stylePackages[0]?.id ?? '');
        setStylePackage(preselected);
      })
      .catch((failure: unknown) => {
        if (cancelled) return;
        setError(withDetail(LOAD_ERROR, messageOf(failure)));
        setProgress(null);
      });
    return () => {
      cancelled = true;
    };
  }, [document_, documentType, exportType, identifiers, loadData, sendRequest]);

  // The selected style package decides every field below it, so it is read whenever it changes - the same
  // request the legacy popup made from its `change` handler.
  useEffect(() => {
    if (!data || !stylePackage) {
      return undefined;
    }
    setCookie(SELECTED_STYLE_PACKAGE_COOKIE, stylePackage);
    const sequence = ++latestPackage.current;
    setProgress('Loading style package data');
    let cancelled = false;
    loadPackage(sendRequest, stylePackage, document_.scope)
      .then((content: StylePackageSettings) => {
        if (cancelled || sequence !== latestPackage.current) return;
        setForm(toExportForm(content, { documentLanguage: data.documentLanguage, urlQuery }));
        setExposeSettings(!!content.exposeSettings);
        setExposePageWidthValidation(!!content.exposePageWidthValidation);
        setInvalidField(null);
        setProgress(null);
      })
      .catch((failure: unknown) => {
        if (cancelled || sequence !== latestPackage.current) return;
        setError(withDetail(PACKAGE_LOAD_ERROR, messageOf(failure)));
        setProgress(null);
      });
    return () => {
      cancelled = true;
    };
  }, [data, document_.scope, loadPackage, sendRequest, stylePackage, urlQuery]);

  const patch = (values: Partial<ExportForm>) => setForm((current) => (current ? { ...current, ...values } : current));

  /** The name to export under: what the user typed, or the server's default, always ending in `.pdf`. */
  const exportFileName = (): string => {
    const name = fileName || data?.fileName || '';
    return name && !name.endsWith('.pdf') ? `${name}.pdf` : name;
  };

  /** The export request, or null when a field is wrong - which is then marked and reported. */
  const prepareRequest = (name?: string): ExportParamsJson | null => {
    if (!form) {
      return null;
    }
    const built = buildExportParams(form, document_, {
      documentType,
      exportType,
      autoSelectStylePackage: autoSelectAvailable && autoSelect,
      fileName: name,
    });
    if ('error' in built) {
      setInvalidField(built.error.field);
      setError(built.error.message);
      return null;
    }
    setInvalidField(null);
    return built.params;
  };

  const exportToPdf = async () => {
    clearAlerts();
    const name = exportFileName();
    const params = prepareRequest(isFileNameOffered(exportType) ? name : undefined);
    if (params === null) {
      return;
    }

    // A bulk export is run by the widget that opened this dialog, one item at a time, with its own progress
    // dialog. The parameters are all it needed from here.
    if (exportType === 'BULK') {
      onClose();
      onBulkExport?.(params);
      return;
    }

    // A test run's attachments are downloaded next to the PDF unless they are embedded into it.
    if (documentType === 'TEST_RUN' && params.attachmentsFilter !== null && !params.embedAttachments) {
      void downloadAttachments(remote, {
        projectId: params.projectId ?? '',
        testRunId: document_.urlQueryParameters?.id ?? '',
        revision: params.revision,
        filter: params.attachmentsFilter,
        testCaseFieldId: params.testcaseFieldId,
      });
    }

    setProgress('Generating PDF');
    try {
      const result = await convert(remote, toRequestBody(params));
      if (result.warning) {
        setWarning(result.warning);
      }
      download(result.blob, name);
      setSuccess('PDF was successfully generated');
    } catch (failure) {
      setError(withDetail(EXPORT_ERROR, messageOf(failure)));
    } finally {
      setProgress(null);
    }
  };

  const validatePdf = async () => {
    clearAlerts();
    const params = prepareRequest();
    if (params === null) {
      return;
    }
    setProgress('Performing PDF validation');
    try {
      const response = await sendRequest({
        method: 'POST',
        url: `/validate?max-results=${MAX_PAGE_PREVIEWS + 1}`,
        contentType: 'application/json',
        body: toRequestBody(params),
      });
      if (!response.ok) {
        setError(withDetail(VALIDATION_ERROR, await errorMessageOf(response)));
        return;
      }
      const result = (await response.json()) as WidthValidationResult;
      if (result.invalidPages.length === 0) {
        setValidationOk('All pages are valid');
      } else {
        setValidation(result);
      }
    } catch {
      setError(VALIDATION_ERROR);
    } finally {
      setProgress(null);
    }
  };

  const childOptions = (setting: keyof PopupData['childNames']): SelectOption[] => data?.childNames[setting] ?? [];

  const invalidPages = validation?.invalidPages ?? [];
  const shownPages = invalidPages.slice(0, MAX_PAGE_PREVIEWS);
  const pagesWord = invalidPages.length === 1 ? 'page' : 'pages';
  const validationSummary =
    invalidPages.length > MAX_PAGE_PREVIEWS
      ? `Invalid pages found. First ${MAX_PAGE_PREVIEWS} of them:`
      : `${invalidPages.length} invalid ${pagesWord} found:`;

  /** The settings block is offered only where a package exposes them and no automatic pick is in effect. */
  const settingsShown = !!form && exposeSettings && !(autoSelectAvailable && autoSelect);
  const validationShown = isPageWidthValidationOffered(exportType, exposePageWidthValidation);

  return (
    <Modal
      open
      title="Export to PDF"
      okText="Export"
      cancelText="Close"
      okDisabled={busy || !form}
      onOk={() => void exportToPdf()}
      onCancel={onClose}
    >
      {/* `pdf-export-form` is what the stylesheet keys the dialog's own width off, so that the same
          stylesheet injected next to another dialog in the same shadow root - the bulk export progress
          dialog, in the widget - does not resize that one too. */}
      <div className="form-wrapper pdf-export-form" ref={form_}>
        {busy && (
          <div className="in-progress-overlay show">
            <span className="sbb-spinner" role="img" aria-label="Loading" />
            <span id="in-progress-message">{progress}</span>
          </div>
        )}

        {/* Only where there is something to say. The legacy markup carried this block with three hidden
            alerts inside it, so its 10px of padding sat above the form whether or not anything was shown. */}
        {(warning || error || success) && (
          <div className="notifications">
            {warning && <div className="alert alert-warning">{warning}</div>}
            {error && <div className="alert alert-error">{error}</div>}
            {success && <div className="alert alert-success">{success}</div>}
          </div>
        )}

        {autoSelectAvailable && (
          <div id="popup-auto-select-style-package-container" className="flex-container">
            <label htmlFor="popup-auto-select-style-package">
              <input
                id="popup-auto-select-style-package"
                type="checkbox"
                checked={autoSelect}
                onChange={(e) => setAutoSelect(e.target.checked)}
              />
              Automatically select a style package most suitable for each of the documents to be exported
            </label>
          </div>
        )}

        {!(autoSelectAvailable && autoSelect) && (
          <div id="popup-style-package" className="flex-container">
            <p>Select one of style packages in dropdown below which you wish to use during export.</p>
            <div className="flex-column">
              <div className="property-wrapper">
                <label htmlFor="popup-style-package-select" className="fixed-width w-1">
                  Style package:
                </label>
                <SearchableSelect
                  id="popup-style-package-select"
                  options={data?.stylePackages ?? []}
                  value={stylePackage}
                  onChange={setStylePackage}
                  disabled={busy}
                />
              </div>
            </div>
          </div>
        )}

        {settingsShown && form && (
          <div id="popup-style-package-content" className="group-start">
            <p>Selected style package exposes its settings, so you can redefine them.</p>

            <div className="flex-container">
              <div className="flex-column">
                <div className="property-wrapper">
                  <label htmlFor="popup-cover-page-checkbox" className="fixed-width w-1">
                    <input
                      id="popup-cover-page-checkbox"
                      type="checkbox"
                      checked={form.coverPageEnabled}
                      onChange={(e) => patch({ coverPageEnabled: e.target.checked })}
                    />
                    Cover page:
                  </label>
                  <div style={reserved(form.coverPageEnabled)}>
                    <SearchableSelect
                      id="popup-cover-page-selector"
                      options={childOptions('cover-page')}
                      value={childValue(childOptions('cover-page'), form.coverPage)}
                      onChange={(value) => patch({ coverPage: value })}
                      disabled={busy}
                    />
                  </div>
                </div>
                <div className="property-wrapper">
                  <label htmlFor="popup-css-selector" className="fixed-width w-1">
                    CSS:
                  </label>
                  <SearchableSelect
                    id="popup-css-selector"
                    options={childOptions('css')}
                    value={childValue(childOptions('css'), form.css)}
                    onChange={(value) => patch({ css: value })}
                    disabled={busy}
                  />
                </div>
              </div>
              <div className="flex-column">
                <div className="property-wrapper">
                  <label htmlFor="popup-header-footer-selector" className="fixed-width w-1">
                    Header/Footer:
                  </label>
                  <SearchableSelect
                    id="popup-header-footer-selector"
                    options={childOptions('header-footer')}
                    value={childValue(childOptions('header-footer'), form.headerFooter)}
                    onChange={(value) => patch({ headerFooter: value })}
                    disabled={busy}
                  />
                </div>
                <div className="property-wrapper">
                  <label htmlFor="popup-localization-selector" className="fixed-width w-1">
                    Localization:
                  </label>
                  <SearchableSelect
                    id="popup-localization-selector"
                    options={childOptions('localization')}
                    value={childValue(childOptions('localization'), form.localization)}
                    onChange={(value) => patch({ localization: value })}
                    disabled={busy}
                  />
                </div>
              </div>
            </div>

            {data?.webhooksEnabled && (
              <div className="flex-container group-start" id="webhooks-container">
                <div className="flex-column">
                  <div className="property-wrapper">
                    <label htmlFor="popup-webhooks-checkbox" className="fixed-width w-1">
                      <input
                        id="popup-webhooks-checkbox"
                        type="checkbox"
                        checked={form.webhooksEnabled}
                        onChange={(e) => patch({ webhooksEnabled: e.target.checked })}
                      />
                      Webhooks:
                    </label>
                    <div style={reserved(form.webhooksEnabled)}>
                      <SearchableSelect
                        id="popup-webhooks-selector"
                        options={childOptions('webhooks')}
                        value={childValue(childOptions('webhooks'), form.webhooks)}
                        onChange={(value) => patch({ webhooks: value })}
                        disabled={busy}
                      />
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/*
              One container for the whole settings block, NOT one per group of rows. A flex row is as tall
              as its taller column, so a column split across several containers cannot use the space a
              shorter neighbour leaves: the legacy popup had three of them here and "Headings color" sat
              alone beside four dropdowns, leaving a hole, while a row hidden for a document type left
              another one that the rows below it could not rise into. Two continuous columns have neither.

              Paper size and Orientation sit in the left column for the same reason - they balance the
              column that opens with the single colour picker. Keep both columns roughly equal in row count
              when adding a field, and check every document type: which rows appear is decided per type by
              `shows()`, so a field added to the longer column costs height only for some of them.
            */}
            <div className="flex-container group-start">
              <div className="property-wrapper full-row">
                <label htmlFor="popup-headers-color" className="fixed-width w-1">
                  Headings color:
                </label>
                <input
                  id="popup-headers-color"
                  type="color"
                  value={form.headersColor}
                  onChange={(e) => patch({ headersColor: e.target.value })}
                />
              </div>
              <div className="flex-column">
                <div className="property-wrapper">
                  <label htmlFor="popup-paper-size-selector" className="fixed-width w-1">
                    Paper size:
                  </label>
                  <SearchableSelect
                    id="popup-paper-size-selector"
                    options={PAPER_SIZES}
                    value={form.paperSize}
                    onChange={(value) => patch({ paperSize: value })}
                    disabled={busy}
                  />
                </div>
                <div className="property-wrapper">
                  <label htmlFor="popup-orientation-selector" className="fixed-width w-1">
                    Orientation:
                  </label>
                  <SearchableSelect
                    id="popup-orientation-selector"
                    options={ORIENTATIONS}
                    value={form.orientation}
                    onChange={(value) => patch({ orientation: value })}
                    disabled={busy}
                  />
                </div>
                {shows('fitToPage') && (
                  <div className="property-wrapper">
                    <label htmlFor="popup-fit-to-page">
                      <input
                        id="popup-fit-to-page"
                        type="checkbox"
                        checked={form.fitToPage}
                        onChange={(e) => patch({ fitToPage: e.target.checked })}
                      />
                      Fit images and tables to page
                    </label>
                  </div>
                )}
                <div className="property-wrapper">
                  <label htmlFor="popup-presentational-hints">
                    <input
                      id="popup-presentational-hints"
                      type="checkbox"
                      checked={form.followHTMLPresentationalHints}
                      onChange={(e) => patch({ followHTMLPresentationalHints: e.target.checked })}
                    />
                    Follow HTML presentational hints
                  </label>
                </div>
                {shows('renderComments') && (
                  <>
                    <div className="property-wrapper">
                      <label htmlFor="popup-render-comments">
                        <input
                          id="popup-render-comments"
                          type="checkbox"
                          checked={form.renderCommentsEnabled}
                          onChange={(e) => patch({ renderCommentsEnabled: e.target.checked })}
                        />
                        Comments rendering
                      </label>
                      <div style={reserved(form.renderCommentsEnabled)}>
                        <SearchableSelect
                          id="popup-render-comments-selector"
                          options={COMMENTS_RENDER_TYPES}
                          value={form.renderComments}
                          onChange={(value) => patch({ renderComments: value })}
                          disabled={busy}
                        />
                      </div>
                    </div>
                    {form.renderCommentsEnabled && (
                      <div className="property-wrapper" id="popup-render-comments-options-container">
                        <div id="popup-render-comments-options">
                          <label htmlFor="popup-include-unreferenced-comments" title={UNREFERENCED_COMMENTS_HELP}>
                            <input
                              id="popup-include-unreferenced-comments"
                              type="checkbox"
                              checked={form.includeUnreferencedComments}
                              onChange={(e) => patch({ includeUnreferencedComments: e.target.checked })}
                            />
                            include unreferenced
                          </label>
                          <label htmlFor="popup-render-native-comments" title={NATIVE_COMMENTS_HELP}>
                            <input
                              id="popup-render-native-comments"
                              type="checkbox"
                              checked={form.renderNativeComments}
                              onChange={(e) => {
                                patch({ renderNativeComments: e.target.checked });
                                // Sticky notes are not a PDF/A construct, so the popup says so as soon as
                                // they are asked for rather than after a non-compliant file was produced.
                                setWarning(e.target.checked ? STICKY_NOTES_WARNING : null);
                              }}
                            />
                            as sticky notes
                          </label>
                        </div>
                      </div>
                    )}
                  </>
                )}
                <div className="property-wrapper">
                  <label htmlFor="popup-watermark">
                    <input
                      id="popup-watermark"
                      type="checkbox"
                      checked={form.watermark}
                      onChange={(e) => patch({ watermark: e.target.checked })}
                    />
                    Watermark
                  </label>
                </div>
                {shows('customListStyles') && (
                  <div className="property-wrapper">
                    <label htmlFor="popup-custom-list-styles">
                      <input
                        id="popup-custom-list-styles"
                        type="checkbox"
                        checked={form.customListStylesEnabled}
                        onChange={(e) => patch({ customListStylesEnabled: e.target.checked })}
                      />
                      Custom styles of numbered lists
                    </label>
                    <input
                      id="popup-numbered-list-styles"
                      className={invalidField === 'numberedListStyles' ? 'grows error' : 'grows'}
                      type="text"
                      placeholder="eg. 1ai"
                      style={reserved(form.customListStylesEnabled)}
                      value={form.customNumberedListStyles}
                      onChange={(e) => patch({ customNumberedListStyles: e.target.value })}
                    />
                  </div>
                )}
                {shows('specificChapters') && (
                  <div className="property-wrapper">
                    <label htmlFor="popup-specific-chapters">
                      <input
                        id="popup-specific-chapters"
                        type="checkbox"
                        checked={form.specificChaptersEnabled}
                        onChange={(e) => patch({ specificChaptersEnabled: e.target.checked })}
                      />
                      Specific higher level chapters
                    </label>
                    <input
                      id="popup-chapters"
                      className={invalidField === 'chapters' ? 'grows error' : 'grows'}
                      type="text"
                      placeholder="eg. 1,2,4 etc."
                      style={reserved(form.specificChaptersEnabled)}
                      value={form.specificChapters}
                      onChange={(e) => patch({ specificChapters: e.target.value })}
                    />
                  </div>
                )}
                {shows('metadataFields') && (
                  <div className="property-wrapper">
                    <label htmlFor="popup-metadata-fields">
                      <input
                        id="popup-metadata-fields"
                        type="checkbox"
                        checked={form.metadataFieldsEnabled}
                        onChange={(e) => patch({ metadataFieldsEnabled: e.target.checked })}
                      />
                      Metadata fields
                    </label>
                    <input
                      id="popup-metadata-fields-input"
                      className="grows"
                      type="text"
                      placeholder="e.g. docOwner, docLanguage, customField*"
                      style={reserved(form.metadataFieldsEnabled)}
                      value={form.metadataFields}
                      onChange={(e) => patch({ metadataFields: e.target.value })}
                    />
                  </div>
                )}
              </div>
              <div className="flex-column">
                <div className="property-wrapper">
                  <label htmlFor="popup-pdf-variant-selector" className="fixed-width w-1">
                    PDF variant:
                  </label>
                  <SearchableSelect
                    id="popup-pdf-variant-selector"
                    options={PDF_VARIANTS}
                    value={form.pdfVariant}
                    onChange={(value) => patch({ pdfVariant: value })}
                    disabled={busy}
                  />
                </div>
                <div className="property-wrapper">
                  <label htmlFor="popup-image-density-selector" className="fixed-width w-1">
                    Image density:
                  </label>
                  <SearchableSelect
                    id="popup-image-density-selector"
                    options={IMAGE_DENSITIES}
                    value={form.imageDensity}
                    onChange={(value) => patch({ imageDensity: value })}
                    disabled={busy}
                  />
                </div>
                <div className="property-wrapper">
                  <label htmlFor="popup-full-fonts">
                    <input
                      id="popup-full-fonts"
                      type="checkbox"
                      checked={form.fullFonts}
                      onChange={(e) => patch({ fullFonts: e.target.checked })}
                    />
                    Embed full fonts (no subsetting)
                  </label>
                  <div className="more-info" title={FULL_FONTS_HELP} />
                </div>
                {shows('cutEmptyChapters') && (
                  <div className="property-wrapper">
                    <label htmlFor="popup-cut-empty-chapters">
                      <input
                        id="popup-cut-empty-chapters"
                        type="checkbox"
                        checked={form.cutEmptyChapters}
                        onChange={(e) => patch({ cutEmptyChapters: e.target.checked })}
                      />
                      Cut empty chapters (any level)
                    </label>
                  </div>
                )}
                {shows('cutEmptyWorkitemAttributes') && (
                  <div className="property-wrapper">
                    <label htmlFor="popup-cut-empty-wi-attributes">
                      <input
                        id="popup-cut-empty-wi-attributes"
                        type="checkbox"
                        checked={form.cutEmptyWorkitemAttributes}
                        onChange={(e) => patch({ cutEmptyWorkitemAttributes: e.target.checked })}
                      />
                      Cut empty Workitem attributes
                    </label>
                  </div>
                )}
                <div className="property-wrapper">
                  <label htmlFor="popup-cut-urls">
                    <input
                      id="popup-cut-urls"
                      type="checkbox"
                      checked={form.cutLocalURLs}
                      onChange={(e) => patch({ cutLocalURLs: e.target.checked })}
                    />
                    Cut local Polarion URLs
                  </label>
                </div>
                {shows('markReferencedWorkitems') && (
                  <div className="property-wrapper">
                    <label htmlFor="popup-mark-referenced-workitems">
                      <input
                        id="popup-mark-referenced-workitems"
                        type="checkbox"
                        checked={form.markReferencedWorkitems}
                        onChange={(e) => patch({ markReferencedWorkitems: e.target.checked })}
                      />
                      Mark referenced Workitems
                    </label>
                  </div>
                )}
                {shows('localizeEnums') && (
                  <div className="property-wrapper">
                    <label htmlFor="popup-localization">
                      <input
                        id="popup-localization"
                        type="checkbox"
                        checked={form.localizeEnums}
                        onChange={(e) => patch({ localizeEnums: e.target.checked })}
                      />
                      Localize enums
                    </label>
                    <div style={reserved(form.localizeEnums)}>
                      <SearchableSelect
                        id="popup-language"
                        options={LANGUAGES}
                        value={form.language}
                        onChange={(value) => patch({ language: value })}
                        disabled={busy}
                      />
                    </div>
                  </div>
                )}
                {shows('roles') && data && data.roles.length > 0 && (
                  <div className="property-wrapper" id="popup-roles-wrapper">
                    <label htmlFor="popup-selected-roles">
                      <input
                        id="popup-selected-roles"
                        type="checkbox"
                        checked={form.rolesEnabled}
                        onChange={(e) => patch({ rolesEnabled: e.target.checked })}
                      />
                      Specific Workitem roles
                    </label>
                    {form.rolesEnabled && (
                      <>
                        <SearchableSelect
                          id="popup-roles-selector"
                          multiple
                          options={data.roles}
                          value={form.linkedWorkitemRoles}
                          onChange={(values) => patch({ linkedWorkitemRoles: values })}
                          disabled={busy}
                        />
                        <SearchableSelect
                          id="popup-roles-direction-selector"
                          options={LINK_ROLE_DIRECTIONS}
                          value={form.linkRoleDirection}
                          onChange={(value) => patch({ linkRoleDirection: value })}
                          disabled={busy}
                        />
                      </>
                    )}
                  </div>
                )}
              </div>
            </div>

            {shows('workItemsQuery') && (
              <div className="flex-container group-start">
                <div className="property-wrapper wide" id="popup-work-items-query-wrapper">
                  <label htmlFor="popup-work-items-query" className="nowrap">
                    <input
                      id="popup-work-items-query"
                      type="checkbox"
                      checked={form.workItemsQueryEnabled}
                      onChange={(e) => patch({ workItemsQueryEnabled: e.target.checked })}
                    />
                    Work items query
                  </label>
                  <input
                    id="popup-work-items-query-input"
                    className="grows"
                    type="text"
                    title="Lucene query applied to filter work items within the document, e.g. 'type:requirement'."
                    placeholder="e.g. type:requirement"
                    style={reserved(form.workItemsQueryEnabled)}
                    value={form.workItemsQuery}
                    onChange={(e) => patch({ workItemsQuery: e.target.value })}
                  />
                </div>
              </div>
            )}

            {shows('testRunAttachments') && (
              <div className="flex-container group-start">
                <div className="flex-column">
                  <div className="property-wrapper">
                    <label htmlFor="popup-download-attachments">
                      <input
                        id="popup-download-attachments"
                        type="checkbox"
                        checked={form.downloadAttachments}
                        onChange={(e) => patch({ downloadAttachments: e.target.checked })}
                      />
                      Download attachments
                    </label>
                  </div>
                  {form.downloadAttachments && (
                    <div className="property-wrapper" id="popup-attachments-filter-container">
                      <label htmlFor="popup-attachments-filter">Attachments filter</label>
                      <input
                        id="popup-attachments-filter"
                        className="w-200"
                        type="text"
                        title="Filter for attachments to be downloaded, example: '*.pdf'"
                        placeholder="*.*"
                        value={form.attachmentsFilter}
                        onChange={(e) => patch({ attachmentsFilter: e.target.value })}
                      />
                    </div>
                  )}
                </div>
                <div className="flex-column">
                  {form.downloadAttachments && (
                    <div className="property-wrapper">
                      <label htmlFor="popup-embed-attachments" id="popup-embed-attachments-label">
                        <input
                          id="popup-embed-attachments"
                          type="checkbox"
                          checked={form.embedAttachments}
                          onChange={(e) => patch({ embedAttachments: e.target.checked })}
                        />
                        Embed attachments into resulted PDF
                      </label>
                    </div>
                  )}
                  {form.downloadAttachments && (
                    <div className="property-wrapper" id="popup-testcase-field-id-container">
                      <label htmlFor="popup-testcase-field-id" className="fixed-width w-1">
                        Custom field ID
                      </label>
                      <input
                        id="popup-testcase-field-id"
                        className="w-200"
                        type="text"
                        title="A boolean testcase field ID. Attachments will be downloaded only from the testcases which have True value in the provided field. Leaving field empty will process all testcases."
                        value={form.testcaseFieldId}
                        onChange={(e) => patch({ testcaseFieldId: e.target.value })}
                      />
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}

        {isFileNameOffered(exportType) && (
          <div className="property-wrapper" id="popup-filename-wrapper">
            <label htmlFor="popup-filename">File name:</label>
            <input
              id="popup-filename"
              className="grows"
              type="text"
              value={fileName}
              onChange={(e) => setFileName(e.target.value)}
            />
          </div>
        )}
      </div>

      {validationShown && (
        <div className="buttons-wrapper" id="popup-page-width-validation">
          <button type="button" id="popup-validate-pdf" disabled={busy} onClick={() => void validatePdf()}>
            <img src={validateIcon} alt="" />
            Validate pages width
          </button>
          {/* The result of a validation run. A validation that could not be run at all is reported in the
              notifications above instead, which is where the legacy popup put it. */}
          <div className="validation-alerts">
            {validationOk && <div className="alert alert-success">{validationOk}</div>}
            {validation && <div className="alert alert-error">{validationSummary}</div>}
          </div>
        </div>
      )}

      {validation && (
        <>
          <div id="page-previews" className="preview">
            {shownPages.map((page, index) => (
              <img
                // The previews have no identity of their own beyond their position in the answer.
                key={index}
                className={
                  zoomed === index ? 'popup-validate-result-img popup-img-zoomed' : 'popup-validate-result-img'
                }
                src={`data:image/png;base64,${page.content}`}
                alt=""
                onClick={() => setZoomed(zoomed === index ? null : index)}
              />
            ))}
          </div>
          <div id="suspicious-wi">
            {validation.suspiciousWorkItems.length > 0 && (
              <>
                Suspicious work items:
                <ul className="suspicious-list">
                  {validation.suspiciousWorkItems.map((item) => (
                    <li key={item.id}>
                      <a href={item.link} target="_blank" rel="noreferrer">
                        {item.id}
                      </a>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </div>
        </>
      )}
    </Modal>
  );
}
