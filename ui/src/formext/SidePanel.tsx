import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { SearchableSelect } from '@grigoriev/react-sbb-polarion';
import type { SelectOption } from '@grigoriev/react-sbb-polarion';
import validateIcon from '../assets/validate.svg';
import type { PanelData } from '../export/exportData';
import { loadPanelData, loadStylePackage } from '../export/exportData';
import type { ExportForm } from '../export/exportForm';
import { childValue, toExportForm } from '../export/exportForm';
import type { ExportField } from '../export/exportParams';
import { buildExportParams, toRequestBody } from '../export/exportParams';
import { convertPdf, downloadBlob, errorMessageOf } from '../services/conversion';
import type { DocumentIdentity } from '../services/exportContext';
import { currentDocumentLocation, toDocumentIdentity } from '../services/exportContext';
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

/** How many invalid page previews the panel shows; the endpoint is asked for one more to detect "more". */
const MAX_PAGE_PREVIEWS = 4;

/** Polarion's own PDF export icon, served by the platform - the icon the legacy panel used. */
const EXPORT_ICON = '/polarion/ria/images/dle/operations/actionPdfExport16.svg';

const STICKY_NOTES_WARNING =
  'Be aware that comments rendered in PDF as sticky notes are not compliant with any of PDF/A variants';

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

interface WidthValidationResult {
  invalidPages: { content: string }[];
  suspiciousWorkItems: { id: string; link: string }[];
}

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

/** `<prefix>` on its own line, then the detail - the legacy `prefix + ":<br>" + message`. */
const withDetail = (prefix: string, detail: string): string => (detail ? `${prefix}:\n${detail}` : prefix);

/** What a rejected conversion says, which is the server's message or nothing. */
const messageOf = (error: unknown): string => (error instanceof Error ? error.message : '');

/**
 * PDF Exporter's Document Properties side panel: the React port of `sidePanelContent.html` +
 * `ExportPanel.js`.
 *
 * It is mounted by `mountSidePanel` into a shadow root on the fragment div Polarion injects into the
 * document editor's Document Properties pane. The markup keeps the ids and classes of the legacy fragment,
 * so `side-panel.css` - which is that fragment's own CSS, injected into the same shadow root - styles it
 * unchanged and the panel looks exactly as it did.
 *
 * What did change is where the data comes from. `PdfExporterFormExtension` used to render this markup with
 * the style packages, setting names, link roles, file name and export permission already substituted into
 * it; now those are read over REST - the same endpoints the DLE toolbar popup has always read them from.
 * The document location and the conversion protocol used to come from the product's `ExportContext.js`,
 * loaded at runtime from the other webapp; both are `services/exportContext.ts` and `services/conversion.ts`
 * now, which this app owns and the popup shares.
 */
export default function SidePanel({ deps }: Readonly<SidePanelProps>) {
  const { sendRequest, sendAbsoluteRequest } = useRemote();
  const loadData = deps?.loadData ?? loadPanelData;
  const loadPackage = deps?.loadPackage ?? loadStylePackage;
  const convert = deps?.convert ?? convertPdf;
  const download = deps?.download ?? downloadBlob;

  const remote = useMemo(() => ({ sendRequest, sendAbsoluteRequest }), [sendRequest, sendAbsoluteRequest]);

  const [data, setData] = useState<PanelData | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

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
  const [exportError, setExportError] = useState<string | null>(null);
  const [exportWarning, setExportWarning] = useState<string | null>(null);
  const [validationOk, setValidationOk] = useState<string | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
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

  const clearMessages = () => {
    setExportError(null);
    setExportWarning(null);
    setValidationOk(null);
    setValidationError(null);
    setValidation(null);
    setZoomed(null);
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
      setExportError(built.error.message);
      return null;
    }
    setInvalidField(null);
    return toRequestBody(built.params);
  };

  const exportToPdf = async () => {
    clearMessages();
    const name = exportFileName();
    const request = prepareRequest(name);
    if (request === null) {
      return;
    }
    setExporting(true);
    try {
      const result = await convert(remote, request);
      if (result.warning) {
        setExportWarning(result.warning);
      }
      download(result.blob, name);
    } catch (error) {
      setExportError(withDetail('Error occurred during PDF generation', messageOf(error)));
    } finally {
      setExporting(false);
    }
  };

  const validatePdf = async () => {
    clearMessages();
    const request = prepareRequest();
    if (request === null) {
      return;
    }
    setValidating(true);
    try {
      const response = await sendRequest({
        method: 'POST',
        url: `/validate?max-results=${MAX_PAGE_PREVIEWS + 1}`,
        contentType: 'application/json',
        body: request,
      });
      if (!response.ok) {
        setValidationError(withDetail('Error occurred validating pages width', await errorMessageOf(response)));
        return;
      }
      const result = (await response.json()) as WidthValidationResult;
      if (result.invalidPages.length === 0) {
        setValidationOk('All pages are valid');
      } else {
        setValidation(result);
      }
    } catch {
      setValidationError('Error occurred validating pages width');
    } finally {
      setValidating(false);
    }
  };

  const childOptions = (setting: keyof PanelData['childNames']): SelectOption[] => data?.childNames[setting] ?? [];

  if (loadError && !form) {
    return <div id="style-package-error">{loadError}</div>;
  }

  if (!data || !form) {
    return (
      <div className="panel-loading">
        <span className="sbb-spinner" role="img" aria-label="Loading" />
        <span className="panel-loading-message">{LOADING_MESSAGE}</span>
      </div>
    );
  }

  const invalidPages = validation?.invalidPages ?? [];
  const shownPages = invalidPages.slice(0, MAX_PAGE_PREVIEWS);
  const pagesWord = invalidPages.length === 1 ? 'page' : 'pages';
  const validationSummary =
    invalidPages.length > MAX_PAGE_PREVIEWS
      ? `Invalid pages found. First ${MAX_PAGE_PREVIEWS} of them:`
      : `${invalidPages.length} invalid ${pagesWord} found:`;

  const actionsDisabled = busy || loadingPackage || data.exportPermission !== 'granted';
  const permissionTitle =
    data.exportPermission === 'denied'
      ? NOT_AUTHORIZED
      : data.exportPermission === 'unknown'
        ? PERMISSION_UNKNOWN
        : undefined;

  return (
    <fieldset className="panel-fieldset" disabled={busy}>
      <p>Select one of style packages in dropdown below which you wish to use during export.</p>
      <div className="property-wrapper">
        <label htmlFor="style-package-select">Style package:</label>
        <SearchableSelect
          id="style-package-select"
          options={data.stylePackages}
          value={stylePackage}
          onChange={setStylePackage}
          disabled={busy}
        />
      </div>
      <div id="style-package-error">{loadError}</div>

      {exposeSettings && (
        <div id="style-package-content" className="group-start">
          <p>Selected style package exposes its settings, so you can redefine them.</p>

          <div className="property-wrapper">
            <label htmlFor="cover-page-checkbox">
              <input
                id="cover-page-checkbox"
                type="checkbox"
                checked={form.coverPageEnabled}
                onChange={(e) => patch({ coverPageEnabled: e.target.checked })}
              />
              Cover page:
            </label>
            {form.coverPageEnabled && (
              <SearchableSelect
                id="cover-page-selector"
                options={childOptions('cover-page')}
                value={childValue(childOptions('cover-page'), form.coverPage)}
                onChange={(value) => patch({ coverPage: value })}
                disabled={busy}
              />
            )}
          </div>

          <div className="property-wrapper">
            <label htmlFor="css-selector">CSS:</label>
            <SearchableSelect
              id="css-selector"
              options={childOptions('css')}
              value={childValue(childOptions('css'), form.css)}
              onChange={(value) => patch({ css: value })}
              disabled={busy}
            />
          </div>

          <div className="property-wrapper">
            <label htmlFor="header-footer-selector">Header/Footer:</label>
            <SearchableSelect
              id="header-footer-selector"
              options={childOptions('header-footer')}
              value={childValue(childOptions('header-footer'), form.headerFooter)}
              onChange={(value) => patch({ headerFooter: value })}
              disabled={busy}
            />
          </div>

          <div className="property-wrapper">
            <label htmlFor="localization-selector">Localization:</label>
            <SearchableSelect
              id="localization-selector"
              options={childOptions('localization')}
              value={childValue(childOptions('localization'), form.localization)}
              onChange={(value) => patch({ localization: value })}
              disabled={busy}
            />
          </div>

          {data.webhooksEnabled && (
            <div className="property-wrapper group-start">
              <label htmlFor="webhooks-checkbox">
                <input
                  id="webhooks-checkbox"
                  type="checkbox"
                  checked={form.webhooksEnabled}
                  onChange={(e) => patch({ webhooksEnabled: e.target.checked })}
                />
                Webhooks:
              </label>
              {form.webhooksEnabled && (
                <SearchableSelect
                  id="webhooks-selector"
                  options={childOptions('webhooks')}
                  value={childValue(childOptions('webhooks'), form.webhooks)}
                  onChange={(value) => patch({ webhooks: value })}
                  disabled={busy}
                />
              )}
            </div>
          )}

          <div className="property-wrapper group-start">
            <label htmlFor="headers-color">Headings color:</label>
            <input
              id="headers-color"
              type="color"
              value={form.headersColor}
              onChange={(e) => patch({ headersColor: e.target.value })}
            />
          </div>

          <div className="property-wrapper">
            <label htmlFor="paper-size-selector">Paper size:</label>
            <SearchableSelect
              id="paper-size-selector"
              options={PAPER_SIZES}
              value={form.paperSize}
              onChange={(value) => patch({ paperSize: value })}
              disabled={busy}
            />
          </div>

          <div className="property-wrapper">
            <label htmlFor="orientation-selector">Orientation:</label>
            <SearchableSelect
              id="orientation-selector"
              options={ORIENTATIONS}
              value={form.orientation}
              onChange={(value) => patch({ orientation: value })}
              disabled={busy}
            />
          </div>

          <div className="property-wrapper">
            <label htmlFor="pdf-variant-selector">PDF variant:</label>
            <SearchableSelect
              id="pdf-variant-selector"
              options={PDF_VARIANTS}
              value={form.pdfVariant}
              onChange={(value) => patch({ pdfVariant: value })}
              disabled={busy}
            />
          </div>

          <div className="property-wrapper">
            <label htmlFor="image-density-selector">Image density:</label>
            <SearchableSelect
              id="image-density-selector"
              options={IMAGE_DENSITIES}
              value={form.imageDensity}
              onChange={(value) => patch({ imageDensity: value })}
              disabled={busy}
            />
          </div>

          <div className="property-wrapper">
            <label htmlFor="full-fonts">
              <input
                id="full-fonts"
                type="checkbox"
                checked={form.fullFonts}
                onChange={(e) => patch({ fullFonts: e.target.checked })}
              />
              Embed full fonts (no subsetting)
            </label>
            <div className="more-info" title={FULL_FONTS_HELP} />
          </div>

          <div className="property-wrapper">
            <label htmlFor="fit-to-page">
              <input
                id="fit-to-page"
                type="checkbox"
                checked={form.fitToPage}
                onChange={(e) => patch({ fitToPage: e.target.checked })}
              />
              Fit images and tables to page
            </label>
          </div>

          <div className="property-wrapper">
            <label htmlFor="presentational-hints">
              <input
                id="presentational-hints"
                type="checkbox"
                checked={form.followHTMLPresentationalHints}
                onChange={(e) => patch({ followHTMLPresentationalHints: e.target.checked })}
              />
              Follow HTML presentational hints
            </label>
          </div>

          <div className="property-wrapper">
            <label htmlFor="render-comments">
              <input
                id="render-comments"
                type="checkbox"
                checked={form.renderCommentsEnabled}
                onChange={(e) => patch({ renderCommentsEnabled: e.target.checked })}
              />
              Comments rendering
            </label>
            {form.renderCommentsEnabled && (
              <SearchableSelect
                id="render-comments-selector"
                options={COMMENTS_RENDER_TYPES}
                value={form.renderComments}
                onChange={(value) => patch({ renderComments: value })}
                disabled={busy}
              />
            )}
          </div>

          {form.renderCommentsEnabled && (
            <div className="property-wrapper" id="render-comments-options" style={{ paddingLeft: 20 }}>
              <label htmlFor="include-unreferenced-comments" title={UNREFERENCED_COMMENTS_HELP}>
                <input
                  id="include-unreferenced-comments"
                  type="checkbox"
                  checked={form.includeUnreferencedComments}
                  onChange={(e) => patch({ includeUnreferencedComments: e.target.checked })}
                />
                include unreferenced
              </label>
              <label htmlFor="render-native-comments" title={NATIVE_COMMENTS_HELP}>
                <input
                  id="render-native-comments"
                  type="checkbox"
                  checked={form.renderNativeComments}
                  onChange={(e) => {
                    patch({ renderNativeComments: e.target.checked });
                    // Sticky notes are not a PDF/A construct, so the panel says so as soon as they are
                    // asked for rather than after an export has already produced a non-compliant file.
                    setExportWarning(e.target.checked ? STICKY_NOTES_WARNING : null);
                  }}
                />
                as sticky notes
              </label>
            </div>
          )}

          <div className="property-wrapper">
            <label htmlFor="watermark">
              <input
                id="watermark"
                type="checkbox"
                checked={form.watermark}
                onChange={(e) => patch({ watermark: e.target.checked })}
              />
              Watermark
            </label>
          </div>

          <div className="property-wrapper">
            <label htmlFor="cut-empty-chapters">
              <input
                id="cut-empty-chapters"
                type="checkbox"
                checked={form.cutEmptyChapters}
                onChange={(e) => patch({ cutEmptyChapters: e.target.checked })}
              />
              Cut empty chapters (any level)
            </label>
          </div>

          <div className="property-wrapper">
            <label htmlFor="cut-empty-wi-attributes">
              <input
                id="cut-empty-wi-attributes"
                type="checkbox"
                checked={form.cutEmptyWorkitemAttributes}
                onChange={(e) => patch({ cutEmptyWorkitemAttributes: e.target.checked })}
              />
              Cut empty Workitem attributes
            </label>
          </div>

          <div className="property-wrapper">
            <label htmlFor="cut-urls">
              <input
                id="cut-urls"
                type="checkbox"
                checked={form.cutLocalURLs}
                onChange={(e) => patch({ cutLocalURLs: e.target.checked })}
              />
              Cut local Polarion URLs
            </label>
          </div>

          <div className="property-wrapper">
            <label htmlFor="mark-referenced-workitems">
              <input
                id="mark-referenced-workitems"
                type="checkbox"
                checked={form.markReferencedWorkitems}
                onChange={(e) => patch({ markReferencedWorkitems: e.target.checked })}
              />
              Mark referenced Workitems
            </label>
          </div>

          <div className="property-wrapper">
            <label htmlFor="specific-chapters" className="w-chapters">
              <input
                id="specific-chapters"
                type="checkbox"
                checked={form.specificChaptersEnabled}
                onChange={(e) => patch({ specificChaptersEnabled: e.target.checked })}
              />
              Specific higher level chapters
            </label>
            {form.specificChaptersEnabled && (
              <input
                id="chapters"
                className={invalidField === 'chapters' ? 'grows error' : 'grows'}
                type="text"
                placeholder="eg. 1,2,4 etc."
                value={form.specificChapters}
                onChange={(e) => patch({ specificChapters: e.target.value })}
              />
            )}
          </div>

          <div className="property-wrapper">
            <label htmlFor="metadata-fields" className="w-metadata">
              <input
                id="metadata-fields"
                type="checkbox"
                checked={form.metadataFieldsEnabled}
                onChange={(e) => patch({ metadataFieldsEnabled: e.target.checked })}
              />
              Metadata fields
            </label>
            {form.metadataFieldsEnabled && (
              <input
                id="metadata-fields-input"
                className="grows"
                type="text"
                placeholder="e.g. docOwner, docLanguage, customField*"
                value={form.metadataFields}
                onChange={(e) => patch({ metadataFields: e.target.value })}
              />
            )}
          </div>

          <div className="property-wrapper">
            <label htmlFor="work-items-query" className="nowrap">
              <input
                id="work-items-query"
                type="checkbox"
                checked={form.workItemsQueryEnabled}
                onChange={(e) => patch({ workItemsQueryEnabled: e.target.checked })}
              />
              Work items query
            </label>
            {form.workItemsQueryEnabled && (
              <input
                id="work-items-query-input"
                className="grows"
                type="text"
                title="Lucene query applied to filter work items within the document, e.g. 'type:requirement'."
                placeholder="e.g. type:requirement"
                value={form.workItemsQuery}
                onChange={(e) => patch({ workItemsQuery: e.target.value })}
              />
            )}
          </div>

          <div className="property-wrapper">
            <label htmlFor="custom-list-styles" className="w-list-styles">
              <input
                id="custom-list-styles"
                type="checkbox"
                checked={form.customListStylesEnabled}
                onChange={(e) => patch({ customListStylesEnabled: e.target.checked })}
              />
              Custom styles of numbered lists
            </label>
            {form.customListStylesEnabled && (
              <input
                id="numbered-list-styles"
                className={invalidField === 'numberedListStyles' ? 'grows error' : 'grows'}
                type="text"
                placeholder="eg. 1ai"
                value={form.customNumberedListStyles}
                onChange={(e) => patch({ customNumberedListStyles: e.target.value })}
              />
            )}
          </div>

          <div className="property-wrapper">
            <label htmlFor="localization">
              <input
                id="localization"
                type="checkbox"
                checked={form.localizeEnums}
                onChange={(e) => patch({ localizeEnums: e.target.checked })}
              />
              Localize enums
            </label>
            {form.localizeEnums && (
              <SearchableSelect
                id="language"
                options={LANGUAGES}
                value={form.language}
                onChange={(value) => patch({ language: value })}
                disabled={busy}
              />
            )}
          </div>

          {/* Roles apply only where the project defines any; an empty list hid the whole group before too. */}
          {data.roles.length > 0 && (
            <div className="roles-fields">
              <div className="property-wrapper">
                <label htmlFor="selected-roles">
                  <input
                    id="selected-roles"
                    type="checkbox"
                    checked={form.rolesEnabled}
                    onChange={(e) => patch({ rolesEnabled: e.target.checked })}
                  />
                  Specific Workitem roles
                </label>
              </div>
              {form.rolesEnabled && (
                <div className="property-wrapper" id="roles-wrapper">
                  <SearchableSelect
                    id="roles-selector"
                    multiple
                    options={data.roles}
                    value={form.linkedWorkitemRoles}
                    onChange={(values) => patch({ linkedWorkitemRoles: values })}
                    disabled={busy}
                  />
                  <SearchableSelect
                    id="roles-direction-selector"
                    options={LINK_ROLE_DIRECTIONS}
                    value={form.linkRoleDirection}
                    onChange={(value) => patch({ linkRoleDirection: value })}
                    disabled={busy}
                  />
                </div>
              )}
            </div>
          )}
        </div>
      )}

      <div className="property-wrapper">
        <label htmlFor="filename" className="w-filename">
          File name:
        </label>
        <input id="filename" type="text" value={fileName} onChange={(e) => setFileName(e.target.value)} />
      </div>

      <div className="buttons-wrapper">
        <button type="button" id="export-pdf" disabled={actionsDisabled} title={permissionTitle} onClick={exportToPdf}>
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
        <div id="export-error">{exportError}</div>
        <div id="export-warning">{exportWarning}</div>
      </div>

      {exposePageWidthValidation && (
        <div className="buttons-wrapper" id="page-width-validation">
          <button
            type="button"
            id="validate-pdf"
            disabled={actionsDisabled}
            title={permissionTitle}
            onClick={() => void validatePdf()}
          >
            <img src={validateIcon} alt="" />
            Validate pages width
          </button>
          <div id="validate-ok">{validationOk}</div>
          <span
            id="validate-pdf-progress"
            className="sbb-spinner"
            role="img"
            aria-label="Loading"
            style={validating ? { display: 'inline-block' } : undefined}
          />
          <div id="validate-error">
            {validationError}
            {validation && (
              <>
                {validationSummary}
                <br />
                {shownPages.map((page, index) => (
                  <img
                    // The previews have no identity of their own beyond their position in the answer.
                    key={index}
                    className={zoomed === index ? 'validate-result-img img-zoomed' : 'validate-result-img'}
                    src={`data:image/png;base64,${page.content}`}
                    alt=""
                    onClick={() => setZoomed(zoomed === index ? null : index)}
                  />
                ))}
                {validation.suspiciousWorkItems.length > 0 && (
                  <>
                    <br />
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
              </>
            )}
          </div>
        </div>
      )}
    </fieldset>
  );
}
