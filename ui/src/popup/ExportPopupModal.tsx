import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Modal } from '@sbb-polarion/react-sbb-polarion';
import ToastHost from '../components/ToastHost';
import ExportFormView from '../export/ExportFormView';
import type { DocumentType, ExportType } from '../export/documentType';
import { isFileNameOffered } from '../export/documentType';
import type { DocIdentifier, PopupData } from '../export/exportData';
import { loadPopupData, loadStylePackage } from '../export/exportData';
import type { ExportForm } from '../export/exportForm';
import { toExportForm } from '../export/exportForm';
import type { ExportField, ExportParamsJson } from '../export/exportParams';
import { buildExportParams, toRequestBody } from '../export/exportParams';
import {
  ALL_PAGES_VALID,
  EXPORT_ERROR,
  EXPORT_SUCCESS,
  VALIDATION_ERROR,
  type WidthValidationResult,
  clearReports,
  messageOf,
  reportFailure,
  reportRefusal,
  reportSuccess,
  reportWarning,
  validatePageWidth,
  withDetail,
} from '../export/reporting';
import { convertPdf, downloadBlob, downloadTestRunAttachments } from '../services/conversion';
import { getCookie, setCookie } from '../services/cookies';
import type { DocumentIdentity } from '../services/exportContext';
import type { StylePackageSettings } from '../services/stylePackage';
import useRemote from '../services/useRemote';
import useDropdownPopupsInDialog from './dialogPortals';

/** The style package the user picked last, offered again next time. The legacy popup's own cookie name. */
const SELECTED_STYLE_PACKAGE_COOKIE = 'selected-style-package';

const LOAD_ERROR = 'Error occurred loading form data';
const PACKAGE_LOAD_ERROR = 'Error occurred loading style package data';

/** Every element id of this form is prefixed with it, which is what the legacy popup's markup used. */
const IDS = 'popup-';

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

/**
 * The "Export to PDF" dialog: the React port of `ExportPopup.js` + `popupForm.html`.
 *
 * Opened from four places - the document editor toolbar, the Live Report toolbar, the "Export to PDF Button"
 * report widget and the Bulk PDF Export widget - which is why it takes what it is exporting as props rather
 * than reading it itself, and why the bulk case hands its parameters back instead of converting.
 *
 * The chrome is RSP's shared `Modal` (a native `<dialog>`: the top layer, the backdrop and Escape for free),
 * so the micromodal library and stylesheet the legacy popup needed on the page are gone. The form inside it
 * is `export/ExportFormView.tsx`, which the document properties side panel renders as well; what is left
 * here is the dialog's own business - reading the data, remembering the style package in a cookie, running
 * the conversion (or handing a bulk export over) and validating the page width.
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

  /** Why the form cannot be used at all. Everything else it has to say is a toast - see `reporting.ts`. */
  const [loadError, setLoadError] = useState<string | null>(null);
  const [validationOk, setValidationOk] = useState<string | null>(null);
  const [validation, setValidation] = useState<WidthValidationResult | null>(null);
  const [zoomed, setZoomed] = useState<number | null>(null);

  /** Which package load is the current one; a slower earlier one must not overwrite it. */
  const latestPackage = useRef(0);

  /** The form, which is what locates the dialog around it - see {@link useDropdownPopupsInDialog}. */
  const form_ = useRef<HTMLDivElement>(null);
  useDropdownPopupsInDialog(form_);

  const busy = progress !== null;

  // Whatever the dialog reported goes with it. A toast outlives its host, so the side panel's host - which
  // stands down while this one is up (see ToastHost) - would be handed it the moment this dialog unmounts,
  // and the message would reappear as an echo of a dialog that is no longer there.
  useEffect(() => clearReports, []);

  /**
   * The `?query=` of the page URL. The item is on screen filtered, so an export started here should match
   * it - which is why it takes priority over the style package's own work items query.
   */
  const urlQuery = document_.urlQueryParameters?.query;

  /** What the last operation said, taken back before the next one starts. */
  const clearAlerts = useCallback(() => {
    clearReports();
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
        setLoadError(withDetail(LOAD_ERROR, messageOf(failure)));
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
        setLoadError(null);
        setProgress(null);
      })
      .catch((failure: unknown) => {
        if (cancelled || sequence !== latestPackage.current) return;
        setLoadError(withDetail(PACKAGE_LOAD_ERROR, messageOf(failure)));
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
      autoSelectStylePackage: autoSelect,
      fileName: name,
    });
    if ('error' in built) {
      setInvalidField(built.error.field);
      reportRefusal(built.error.message);
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
        reportWarning(result.warning);
      }
      download(result.blob, name);
      reportSuccess(EXPORT_SUCCESS);
    } catch (failure) {
      reportFailure(EXPORT_ERROR, failure);
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
      const result = await validatePageWidth(sendRequest, toRequestBody(params));
      if (result.invalidPages.length === 0) {
        setValidationOk(ALL_PAGES_VALID);
      } else {
        setValidation(result);
      }
    } catch (failure) {
      reportFailure(VALIDATION_ERROR, failure);
    } finally {
      setProgress(null);
    }
  };

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
      {/* Inside the dialog on purpose: it is a native `<dialog>` in the top layer, and a toast host outside
          it would be painted behind the dialog and dimmed by its backdrop. Outside `ExportFormView`, which
          is a query container and therefore a containing block for anything `position: fixed` inside it. */}
      <ToastHost />

      <ExportFormView
        ids={IDS}
        documentType={documentType}
        exportType={exportType}
        data={data}
        stylePackage={stylePackage}
        onStylePackage={setStylePackage}
        autoSelect={autoSelect}
        onAutoSelect={setAutoSelect}
        form={form}
        onPatch={patch}
        exposeSettings={exposeSettings}
        fileName={fileName}
        onFileName={setFileName}
        invalidField={invalidField}
        busy={busy}
        loadError={loadError}
        validation={{
          exposed: exposePageWidthValidation,
          onRun: () => void validatePdf(),
          disabled: busy,
          ok: validationOk,
          result: validation,
          zoomed,
          onZoom: setZoomed,
        }}
        formRef={form_}
        overlay={
          busy && (
            <div className="in-progress-overlay show">
              <span className="sbb-spinner" role="img" aria-label="Loading" />
              <span id="in-progress-message">{progress}</span>
            </div>
          )
        }
      />
    </Modal>
  );
}
