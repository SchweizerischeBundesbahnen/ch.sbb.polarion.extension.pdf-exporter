import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import ToastHost from '../components/ToastHost';
import ExportFormView from '../export/ExportFormView';
import type { PanelData } from '../export/exportData';
import { loadPanelData, loadStylePackage } from '../export/exportData';
import type { ExportForm } from '../export/exportForm';
import { toExportForm } from '../export/exportForm';
import type { ExportField } from '../export/exportParams';
import { buildExportParams, toRequestBody } from '../export/exportParams';
import {
  ALL_PAGES_VALID,
  EXPORT_ERROR,
  EXPORT_SUCCESS,
  VALIDATION_ERROR,
  type WidthValidationResult,
  clearReports,
  reportFailure,
  reportRefusal,
  reportSuccess,
  reportWarning,
  validatePageWidth,
} from '../export/reporting';
import { convertPdf, downloadBlob } from '../services/conversion';
import type { DocumentIdentity } from '../services/exportContext';
import { currentDocumentLocation, toDocumentIdentity } from '../services/exportContext';
import type { StylePackageSettings } from '../services/stylePackage';
import useRemote from '../services/useRemote';

/** Polarion's own PDF export icon, served by the platform - the icon the legacy panel used. */
const EXPORT_ICON = '/polarion/ria/images/dle/operations/actionPdfExport16.svg';

const PACKAGE_LOAD_ERROR = 'There was an error loading style package settings. Please, contact administrator';

/**
 * What the panel says while it reads what it offers.
 *
 * Deliberately generic. The export popup names the step it is on ("Loading form data") because it shows
 * this overlay for several different operations and has to say which; the panel shows it for one, and that
 * one is eight parallel reads - so naming any of them would say less than nothing. `Loading...` is the
 * wording the other extensions' loading states use.
 */
const LOADING_MESSAGE = 'Loading...';

const NOT_AUTHORIZED = 'You are not allowed to export PDF for this project';

/**
 * Why the export buttons are off when the permission could not be read at all. Both cases keep them off -
 * the check fails closed - but only an explicit refusal can be reported as one.
 */
const PERMISSION_UNKNOWN = 'Could not check whether you are allowed to export. Please, reload the page.';

/** The panel's element ids are the legacy fragment's own, which carry no prefix. */
const IDS = '';

/** What the panel reaches outside itself for, so the dev harness and the tests can replace it. */
export interface SidePanelDependencies {
  /** Where the document is. Read from the editor URL when not given, which is what happens in Polarion. */
  location?: DocumentIdentity;
  loadData?: typeof loadPanelData;
  loadPackage?: typeof loadStylePackage;
  convert?: typeof convertPdf;
  download?: typeof downloadBlob;
}

export interface SidePanelProps {
  deps?: SidePanelDependencies;
}

/**
 * PDF Exporter's Document Properties side panel: the React port of `sidePanelContent.html` +
 * `ExportPanel.js`.
 *
 * It is mounted by `mountSidePanel` into a shadow root on the fragment div Polarion injects into the
 * document editor's Document Properties pane. The form itself is `export/ExportFormView.tsx`, which the
 * "Export to PDF" dialog renders as well - the two used to be two copies of the same form; what is left
 * here is the panel's own business: reading the data, running the conversion, validating the page width,
 * and the "Export to PDF" button, which is the panel's own and not the dialog's footer.
 *
 * What did change against the legacy fragment is where the data comes from. `PdfExporterFormExtension` used
 * to render this markup with the style packages, setting names, link roles, file name and export permission
 * already substituted into it; now those are read over REST - the same endpoints the DLE toolbar popup has
 * always read them from. The document location and the conversion protocol used to come from the product's
 * `ExportContext.js`, loaded at runtime from the other webapp; both are `services/exportContext.ts` and
 * `services/conversion.ts` now, which this app owns and the popup shares.
 */
export default function SidePanel({ deps }: Readonly<SidePanelProps>) {
  const { sendRequest, sendAbsoluteRequest } = useRemote();
  const loadData = deps?.loadData ?? loadPanelData;
  const loadPackage = deps?.loadPackage ?? loadStylePackage;
  const convert = deps?.convert ?? convertPdf;
  const download = deps?.download ?? downloadBlob;

  const remote = useMemo(() => ({ sendRequest, sendAbsoluteRequest }), [sendRequest, sendAbsoluteRequest]);

  const [data, setData] = useState<PanelData | null>(null);

  const [stylePackage, setStylePackage] = useState('');
  /** Which of the selected package's settings the user may redefine, and whether Validate is offered. */
  const [exposeSettings, setExposeSettings] = useState(false);
  const [exposePageWidthValidation, setExposePageWidthValidation] = useState(false);
  const [form, setForm] = useState<ExportForm | null>(null);
  const [loadingPackage, setLoadingPackage] = useState(false);

  const [fileName, setFileName] = useState('');
  const [invalidField, setInvalidField] = useState<ExportField | null>(null);

  const [exporting, setExporting] = useState(false);
  const [validating, setValidating] = useState(false);
  /** Why the panel cannot be used at all. Everything else it has to say is a toast - see `reporting.ts`. */
  const [loadError, setLoadError] = useState<string | null>(null);
  const [validationOk, setValidationOk] = useState<string | null>(null);
  const [validation, setValidation] = useState<WidthValidationResult | null>(null);
  const [zoomed, setZoomed] = useState<number | null>(null);

  /** Which package load is the current one; a slower earlier one must not overwrite it. */
  const latestPackage = useRef(0);

  const busy = exporting || validating;

  /** Where the document lives, read out of the editor URL the way the product's ExportContext read it. */
  const document_: DocumentIdentity = useMemo(
    () => deps?.location ?? toDocumentIdentity(currentDocumentLocation({ documentType: 'LIVE_DOC' })),
    [deps?.location],
  );

  /**
   * The `?query=` of the editor URL. The document is on screen filtered, so an export started here should
   * match it - which is why it takes priority over the style package's own work items query.
   */
  const urlQuery = document_.urlQueryParameters?.query;

  // Everything the panel offers. The style packages and the option lists are required - there is nothing to
  // choose from without them - so a failure here is reported instead of an empty panel.
  useEffect(() => {
    let cancelled = false;
    loadData(sendRequest, document_)
      .then((loaded) => {
        if (cancelled) return;
        setData(loaded);
        setFileName(loaded.fileName);
        setStylePackage(loaded.stylePackages[0]?.id ?? '');
        setLoadError(null);
      })
      .catch(() => {
        if (!cancelled) setLoadError(PACKAGE_LOAD_ERROR);
      });
    return () => {
      cancelled = true;
    };
  }, [document_, loadData, sendRequest]);

  const applyPackage = useCallback(
    (content: StylePackageSettings, documentLanguage: string | null) => {
      setForm(toExportForm(content, { documentLanguage, urlQuery }));
      setExposeSettings(!!content.exposeSettings);
      setExposePageWidthValidation(!!content.exposePageWidthValidation);
      setInvalidField(null);
    },
    [urlQuery],
  );

  // The selected style package decides every field below it, so it is read whenever it changes - the same
  // request the legacy panel made from its `change` handler.
  useEffect(() => {
    if (!data || !stylePackage) {
      return undefined;
    }
    const sequence = ++latestPackage.current;
    setLoadingPackage(true);
    let cancelled = false;
    loadPackage(sendRequest, stylePackage, document_.scope)
      .then((content) => {
        if (cancelled || sequence !== latestPackage.current) return;
        applyPackage(content, data.documentLanguage);
        setLoadError(null);
        setLoadingPackage(false);
      })
      .catch(() => {
        if (cancelled || sequence !== latestPackage.current) return;
        setLoadError(PACKAGE_LOAD_ERROR);
        setLoadingPackage(false);
      });
    return () => {
      cancelled = true;
    };
  }, [applyPackage, data, document_, loadPackage, sendRequest, stylePackage]);

  const patch = (values: Partial<ExportForm>) => setForm((current) => (current ? { ...current, ...values } : current));

  /** The name to export under: what the user typed, or the server's default, always ending in `.pdf`. */
  const exportFileName = (): string => {
    const name = fileName || data?.fileName || '';
    return name && !name.endsWith('.pdf') ? `${name}.pdf` : name;
  };

  /** What the last operation said, taken back before the next one starts. */
  const clearAlerts = () => {
    clearReports();
    setValidationOk(null);
    setValidation(null);
    setZoomed(null);
    setInvalidField(null);
  };

  /** The export request, or null when a field is wrong - which is then marked and reported. */
  const prepareRequest = (name?: string): string | null => {
    if (!form) {
      return null;
    }
    const built = buildExportParams(form, document_, {
      documentType: 'LIVE_DOC',
      exportType: 'SINGLE',
      fileName: name,
    });
    if ('error' in built) {
      setInvalidField(built.error.field);
      reportRefusal(built.error.message);
      return null;
    }
    setInvalidField(null);
    return toRequestBody(built.params);
  };

  const exportToPdf = async () => {
    clearAlerts();
    const name = exportFileName();
    const request = prepareRequest(name);
    if (request === null) {
      return;
    }
    setExporting(true);
    try {
      const result = await convert(remote, request);
      if (result.warning) {
        reportWarning(result.warning);
      }
      download(result.blob, name);
      reportSuccess(EXPORT_SUCCESS);
    } catch (failure) {
      reportFailure(EXPORT_ERROR, failure);
    } finally {
      setExporting(false);
    }
  };

  const validatePdf = async () => {
    clearAlerts();
    const request = prepareRequest();
    if (request === null) {
      return;
    }
    setValidating(true);
    try {
      const result = await validatePageWidth(sendRequest, request);
      if (result.invalidPages.length === 0) {
        setValidationOk(ALL_PAGES_VALID);
      } else {
        setValidation(result);
      }
    } catch (failure) {
      reportFailure(VALIDATION_ERROR, failure);
    } finally {
      setValidating(false);
    }
  };

  // Nothing to show a form for: the option lists the panel offers could not be read at all. Reported as the
  // same alert the form itself would carry, which is the one the export dialog shows in the same case.
  if (loadError && !form) {
    return (
      <div className="notifications">
        <div id="load-error" className="alert alert-error">
          {loadError}
        </div>
      </div>
    );
  }

  if (!data || !form) {
    return (
      <div className="panel-loading">
        <span className="sbb-spinner" role="img" aria-label="Loading" />
        <span className="panel-loading-message">{LOADING_MESSAGE}</span>
      </div>
    );
  }

  const actionsDisabled = busy || loadingPackage || data.exportPermission !== 'granted';
  const permissionTitle =
    data.exportPermission === 'denied'
      ? NOT_AUTHORIZED
      : data.exportPermission === 'unknown'
        ? PERMISSION_UNKNOWN
        : undefined;

  return (
    <>
      {/* Outside the fieldset, which is disabled while an export runs, and outside `ExportFormView`, which
          is a query container and therefore a containing block for anything `position: fixed` inside it. */}
      <ToastHost />

      <fieldset className="panel-fieldset" disabled={busy}>
        <ExportFormView
          ids={IDS}
          documentType="LIVE_DOC"
          exportType="SINGLE"
          data={data}
          stylePackage={stylePackage}
          onStylePackage={setStylePackage}
          autoSelect={false}
          onAutoSelect={() => {}}
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
            disabled: actionsDisabled,
            running: validating,
            title: permissionTitle,
            ok: validationOk,
            result: validation,
            zoomed,
            onZoom: setZoomed,
          }}
          actions={
            <div className="buttons-wrapper">
              <button
                type="button"
                id="export-pdf"
                disabled={actionsDisabled}
                title={permissionTitle}
                onClick={() => void exportToPdf()}
              >
                <img src={EXPORT_ICON} alt="" />
                Export to PDF
              </button>
              <span
                id="export-pdf-progress"
                className="sbb-spinner"
                role="img"
                aria-label="Loading"
                style={exporting ? { display: 'inline-block' } : undefined}
              />
            </div>
          }
        />
      </fieldset>
    </>
  );
}
