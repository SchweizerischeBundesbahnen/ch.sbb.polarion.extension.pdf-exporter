import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ConfigurationButtons,
  ConfigurationsPane,
  type ConfigurationsPaneHandle,
  PageLayout,
  RevisionsTable,
  SearchableSelect,
  type SelectOption,
  type SettingName,
  useConfirm,
} from '@grigoriev/react-sbb-polarion';
import { toast } from 'sonner';
import { getScope } from '../services/scope';
import useNamedSettings from '../services/settings';
import useRemote from '../services/useRemote';

const FEATURE = 'style-package';

/** The configuration every child dropdown falls back to, and the one whose matching query is unused. */
const DEFAULT_NAME = 'Default';

/** The `value` of the JSP page's color input, kept so an unset color looks the way it always did. */
const DEFAULT_HEADERS_COLOR = '#004d73';

/**
 * The named settings a style package points at. Each of them is an administration page of its own, so
 * this page only lists their names - it never reads their content.
 */
const CHILD_SETTINGS = ['cover-page', 'css', 'header-footer', 'localization', 'webhooks'] as const;
type ChildSetting = (typeof CHILD_SETTINGS)[number];
type ChildNames = Record<ChildSetting, SelectOption[]>;

const NO_CHILD_NAMES: ChildNames = {
  'cover-page': [],
  css: [],
  'header-footer': [],
  localization: [],
  webhooks: [],
};

const PAPER_SIZES: SelectOption[] = [
  { id: 'A5', name: 'A5' },
  { id: 'A4', name: 'A4' },
  { id: 'A3', name: 'A3' },
  { id: 'B5', name: 'B5' },
  { id: 'B4', name: 'B4' },
  { id: 'JIS_B5', name: 'JIS-B5' },
  { id: 'JIS_B4', name: 'JIS-B4' },
  { id: 'LETTER', name: 'Letter' },
  { id: 'LEGAL', name: 'Legal' },
  { id: 'LEDGER', name: 'Ledger' },
];

const ORIENTATIONS: SelectOption[] = [
  { id: 'PORTRAIT', name: 'Portrait' },
  { id: 'LANDSCAPE', name: 'Landscape' },
];

const PDF_VARIANTS: SelectOption[] = [
  { id: 'PDF_A_1A', name: 'pdf/a-1a' },
  { id: 'PDF_A_1B', name: 'pdf/a-1b' },
  { id: 'PDF_A_2A', name: 'pdf/a-2a' },
  { id: 'PDF_A_2B', name: 'pdf/a-2b' },
  { id: 'PDF_A_2U', name: 'pdf/a-2u' },
  { id: 'PDF_A_3A', name: 'pdf/a-3a' },
  { id: 'PDF_A_3B', name: 'pdf/a-3b' },
  { id: 'PDF_A_3U', name: 'pdf/a-3u' },
  { id: 'PDF_A_4E', name: 'pdf/a-4e' },
  { id: 'PDF_A_4F', name: 'pdf/a-4f' },
  { id: 'PDF_A_4U', name: 'pdf/a-4u' },
  { id: 'PDF_UA_1', name: 'pdf/ua-1' },
  { id: 'PDF_UA_2', name: 'pdf/ua-2' },
];

const IMAGE_DENSITIES: SelectOption[] = [
  { id: 'DPI_96', name: '96 dpi' },
  { id: 'DPI_192', name: '192 dpi' },
  { id: 'DPI_300', name: '300 dpi' },
  { id: 'DPI_600', name: '600 dpi' },
];

const COMMENTS_RENDER_TYPES: SelectOption[] = [
  { id: 'OPEN', name: 'Open' },
  { id: 'ALL', name: 'All' },
];

const LANGUAGES: SelectOption[] = [
  { id: 'de', name: 'Deutsch' },
  { id: 'fr', name: 'Français' },
  { id: 'it', name: 'Italiano' },
];

const LINK_ROLE_DIRECTIONS: SelectOption[] = [
  { id: 'BOTH', name: 'Both directions' },
  { id: 'DIRECT', name: 'Direct only' },
  { id: 'REVERSE', name: 'Reverse only' },
];

const WEIGHT_HELP =
  'A float number from 0.0 to 100, which will determine the position of current style package in the ' +
  'resulting style packages list. The higher the number, the higher its position will be.';

const MATCHING_QUERY_HELP =
  'A query to select documents to which this style package will be relevant. For documents not matching ' +
  "this query the style package won't be visible. If you want to make this style package be available to " +
  'all documents, just leave this field empty.';

const FULL_FONTS_HELP =
  'When enabled, fonts are embedded in their entirety without subsetting: full glyph coverage and better ' +
  'editability of the resulting PDF, at the cost of a larger file. This is not a robustness switch: ' +
  'subsetting already keeps a font whole when it cannot be applied, while skipping it can itself fail on ' +
  'a damaged font.';

const WORK_ITEMS_QUERY_HELP =
  "Lucene query applied to filter work items within the document, e.g. 'type:requirement'. Leave empty to " +
  'include all work items.';

/** Content of one named `style-package` configuration, as `StylePackageModel` serializes it. */
interface StylePackageSettings {
  matchingQuery?: string | null;
  weight?: number | null;
  exposeSettings?: boolean;
  coverPage?: string | null;
  headerFooter?: string | null;
  css?: string | null;
  localization?: string | null;
  webhooks?: string | null;
  headersColor?: string | null;
  paperSize?: string | null;
  orientation?: string | null;
  pdfVariant?: string | null;
  imageDensity?: string | null;
  fullFonts?: boolean;
  fitToPage?: boolean;
  renderComments?: string | null;
  renderNativeComments?: boolean;
  includeUnreferencedComments?: boolean;
  watermark?: boolean;
  markReferencedWorkitems?: boolean;
  cutEmptyChapters?: boolean;
  cutEmptyWorkitemAttributes?: boolean;
  cutLocalURLs?: boolean;
  followHTMLPresentationalHints?: boolean;
  specificChapters?: string | null;
  metadataFields?: string | null;
  customNumberedListStyles?: string | null;
  language?: string | null;
  linkedWorkitemRoles?: string[] | null;
  linkRoleDirection?: string | null;
  workItemsQuery?: string | null;
  exposePageWidthValidation?: boolean;
  attachmentsFilter?: string | null;
  testcaseFieldId?: string | null;
  embedAttachments?: boolean;
}

/**
 * The form behind the page. It is not the stored document: a setting the document expresses as "null
 * means off" is two fields here - the checkbox that switches it on and the value it carries - so
 * unticking a box does not throw away what the administrator typed before ticking it again.
 */
interface Form {
  matchingQuery: string;
  weight: string;
  exposeSettings: boolean;
  coverPageEnabled: boolean;
  coverPage: string;
  css: string;
  headerFooter: string;
  localization: string;
  webhooksEnabled: boolean;
  webhooks: string;
  headersColor: string;
  paperSize: string;
  orientation: string;
  pdfVariant: string;
  imageDensity: string;
  fullFonts: boolean;
  fitToPage: boolean;
  followHTMLPresentationalHints: boolean;
  renderCommentsEnabled: boolean;
  renderComments: string;
  includeUnreferencedComments: boolean;
  renderNativeComments: boolean;
  watermark: boolean;
  cutEmptyChapters: boolean;
  cutEmptyWorkitemAttributes: boolean;
  cutLocalURLs: boolean;
  markReferencedWorkitems: boolean;
  customListStylesEnabled: boolean;
  customNumberedListStyles: string;
  specificChaptersEnabled: boolean;
  specificChapters: string;
  metadataFieldsEnabled: boolean;
  metadataFields: string;
  localizeEnums: boolean;
  language: string;
  rolesEnabled: boolean;
  linkedWorkitemRoles: string[];
  linkRoleDirection: string;
  workItemsQueryEnabled: boolean;
  workItemsQuery: string;
  downloadAttachments: boolean;
  attachmentsFilter: string;
  testcaseFieldId: string;
  embedAttachments: boolean;
  exposePageWidthValidation: boolean;
}

/**
 * The legacy `StylePackageUtils.adjustWeight`, ported unchanged: clamp above 100, keep one decimal,
 * and fall back to 50 for anything that is not `NNN.N` - an empty or nonsense entry included.
 */
function adjustWeight(raw: string): string {
  let value = parseFloat(raw);
  if (value > 100) {
    value = 100;
  }
  if (value % 1 !== 0) {
    value = parseFloat(value.toFixed(1));
  }
  return /^\d{1,3}(\.\d)?$/.test(String(value)) ? String(value) : '50';
}

/**
 * A name that belongs to a parent scope is marked the same way `ConfigurationsPane` marks its own
 * options, so "(inherited)" means one thing everywhere on the administration pages.
 */
function toOption(name: SettingName, scope: string): SelectOption {
  return { id: name.name, name: name.scope === scope ? name.name : `${name.name} (inherited)` };
}

function toForm(content: StylePackageSettings): Form {
  // The legacy page derived one checkbox from two fields: either of them makes attachments downloaded.
  const attachments = !!content.attachmentsFilter || !!content.testcaseFieldId;
  const roles = content.linkedWorkitemRoles ?? [];
  return {
    matchingQuery: content.matchingQuery ?? '',
    weight: content.weight === null || content.weight === undefined ? '' : String(content.weight),
    exposeSettings: !!content.exposeSettings,
    coverPageEnabled: !!content.coverPage,
    coverPage: content.coverPage ?? DEFAULT_NAME,
    css: content.css ?? DEFAULT_NAME,
    headerFooter: content.headerFooter ?? DEFAULT_NAME,
    localization: content.localization ?? DEFAULT_NAME,
    webhooksEnabled: !!content.webhooks,
    webhooks: content.webhooks ?? DEFAULT_NAME,
    headersColor: content.headersColor ?? DEFAULT_HEADERS_COLOR,
    paperSize: content.paperSize ?? 'A4',
    orientation: content.orientation ?? 'PORTRAIT',
    pdfVariant: content.pdfVariant ?? 'PDF_A_2B',
    imageDensity: content.imageDensity ?? 'DPI_96',
    fullFonts: !!content.fullFonts,
    fitToPage: !!content.fitToPage,
    followHTMLPresentationalHints: !!content.followHTMLPresentationalHints,
    renderCommentsEnabled: !!content.renderComments,
    renderComments: content.renderComments ?? 'OPEN',
    includeUnreferencedComments: !!content.includeUnreferencedComments,
    renderNativeComments: !!content.renderNativeComments,
    watermark: !!content.watermark,
    cutEmptyChapters: !!content.cutEmptyChapters,
    cutEmptyWorkitemAttributes: !!content.cutEmptyWorkitemAttributes,
    cutLocalURLs: !!content.cutLocalURLs,
    markReferencedWorkitems: !!content.markReferencedWorkitems,
    customListStylesEnabled: !!content.customNumberedListStyles,
    customNumberedListStyles: content.customNumberedListStyles ?? '',
    specificChaptersEnabled: !!content.specificChapters,
    specificChapters: content.specificChapters ?? '',
    metadataFieldsEnabled: !!content.metadataFields,
    metadataFields: content.metadataFields ?? '',
    localizeEnums: !!content.language,
    language: content.language ?? 'de',
    rolesEnabled: roles.length > 0,
    linkedWorkitemRoles: roles,
    linkRoleDirection: content.linkRoleDirection ?? 'BOTH',
    workItemsQueryEnabled: !!content.workItemsQuery,
    workItemsQuery: content.workItemsQuery ?? '',
    downloadAttachments: attachments,
    attachmentsFilter: content.attachmentsFilter ?? '',
    testcaseFieldId: content.testcaseFieldId ?? '',
    embedAttachments: attachments && !!content.embedAttachments,
    exposePageWidthValidation: !!content.exposePageWidthValidation,
  };
}

const EMPTY_FORM = toForm({});

/**
 * PDF Exporter: Style Packages - the named bundles of export settings offered on the export dialog,
 * one configuration at a time.
 *
 * It is the widest settings page of the extension: a style package points at four other named
 * settings (cover page, CSS, header/footer, localization) plus the optional webhooks, and carries the
 * ~30 switches that decide what the renderer does with the document. The layout is the two-column
 * arrangement of the JSP page it replaces, section by section, so an administrator finds every control
 * where it always was.
 *
 * The names of the child settings are read once, when the page opens. A style package cannot be
 * configured without them, which is why a failure there - or an empty list - is reported as an error
 * rather than as an empty dropdown.
 */
export default function StylePackages() {
  const scope = getScope();
  const settings = useNamedSettings<StylePackageSettings>(FEATURE);
  const { sendRequest } = useRemote();
  const { confirm, confirmDialog } = useConfirm();
  const paneRef = useRef<ConfigurationsPaneHandle>(null);

  /** Which load is the current one; only the newest writes (see CustomTemplatesPage for why). */
  const latestLoad = useRef(0);

  const [form, setForm] = useState<Form>(EMPTY_FORM);
  const [selectedConfig, setSelectedConfig] = useState<string | null>(null);
  const [editingName, setEditingName] = useState(false);
  const [showRevisions, setShowRevisions] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);
  const [loadingError, setLoadingError] = useState(false);

  const [childNames, setChildNames] = useState<ChildNames>(NO_CHILD_NAMES);
  const [childNamesLoading, setChildNamesLoading] = useState(true);
  const [childNamesError, setChildNamesError] = useState(false);

  const [roleOptions, setRoleOptions] = useState<SelectOption[]>([]);
  const [rolesLoading, setRolesLoading] = useState(true);
  const [rolesError, setRolesError] = useState(false);

  /** Whether the installation has webhooks at all; unknown until the status is read. */
  const [webhooksEnabled, setWebhooksEnabled] = useState<boolean | null>(null);

  const patch = (values: Partial<Form>) => setForm((current) => ({ ...current, ...values }));

  useEffect(() => {
    let cancelled = false;
    Promise.all(
      CHILD_SETTINGS.map(async (setting) => {
        const response = await sendRequest({
          method: 'GET',
          url: `/settings/${setting}/names?scope=${encodeURIComponent(scope)}`,
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const names = (await response.json()) as SettingName[];
        // An empty list is a failure too, as it was on the legacy page: a style package has to point at
        // an existing configuration, so there is nothing to choose from and nothing to save.
        if (names.length === 0) throw new Error(`no ${setting} configurations`);
        return [setting, names.map((name) => toOption(name, scope))] as const;
      }),
    )
      .then((entries) => {
        if (cancelled) return;
        setChildNames({ ...NO_CHILD_NAMES, ...Object.fromEntries(entries) } as ChildNames);
        setChildNamesError(false);
        setChildNamesLoading(false);
      })
      .catch(() => {
        if (cancelled) return;
        setChildNamesError(true);
        setChildNamesLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [sendRequest, scope]);

  useEffect(() => {
    let cancelled = false;
    sendRequest({ method: 'GET', url: `/link-role-names?scope=${encodeURIComponent(scope)}` })
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json() as Promise<string[]>;
      })
      .then((names) => {
        if (cancelled) return;
        setRoleOptions(names.map((name) => ({ id: name, name })));
        setRolesError(false);
        setRolesLoading(false);
      })
      .catch(() => {
        if (cancelled) return;
        setRolesError(true);
        setRolesLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [sendRequest, scope]);

  // Webhooks are an installation-wide switch, so the row that points at a webhooks configuration is
  // there only when they are on. A read that fails leaves it hidden without claiming anything: the
  // stored value is saved back untouched either way, so nothing is lost.
  useEffect(() => {
    let cancelled = false;
    sendRequest({ method: 'GET', url: '/webhooks/status' })
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json() as Promise<{ enabled?: boolean }>;
      })
      .then((status) => {
        if (!cancelled) setWebhooksEnabled(!!status?.enabled);
      })
      .catch(() => {
        if (!cancelled) setWebhooksEnabled(false);
      });
    return () => {
      cancelled = true;
    };
  }, [sendRequest]);

  /**
   * The configuration a child dropdown actually points at. A stored name that the scope no longer
   * offers falls back to Default, exactly as the legacy page did - but only once the list is known,
   * so a failed or pending read cannot rewrite a perfectly good reference.
   */
  const childValue = useCallback(
    (setting: ChildSetting, value: string): string => {
      const options = childNames[setting];
      if (options.length === 0 || options.some((option) => option.id === value)) {
        return value;
      }
      return DEFAULT_NAME;
    },
    [childNames],
  );

  const applyContent = useCallback((content: StylePackageSettings) => {
    latestLoad.current += 1;
    setForm(toForm(content));
    // A load that succeeded after an earlier failure would otherwise keep the banner up over good data.
    setLoadingError(false);
  }, []);

  const handleSave = async () => {
    if (!selectedConfig) return;
    toast.dismiss();
    const weight = adjustWeight(form.weight);
    // Anything switched off is stored as null rather than as a stale value, which is what makes the
    // checkbox and the stored document agree - the legacy page wrote the very same body.
    const content: StylePackageSettings = {
      matchingQuery: form.matchingQuery,
      weight: Number(weight),
      exposeSettings: form.exposeSettings,
      coverPage: form.coverPageEnabled ? childValue('cover-page', form.coverPage) : null,
      css: childValue('css', form.css),
      headerFooter: childValue('header-footer', form.headerFooter),
      localization: childValue('localization', form.localization),
      webhooks: form.webhooksEnabled ? childValue('webhooks', form.webhooks) : null,
      headersColor: form.headersColor,
      paperSize: form.paperSize,
      orientation: form.orientation,
      pdfVariant: form.pdfVariant,
      imageDensity: form.imageDensity,
      fitToPage: form.fitToPage,
      renderComments: form.renderCommentsEnabled ? form.renderComments : null,
      renderNativeComments: form.renderNativeComments,
      includeUnreferencedComments: form.includeUnreferencedComments,
      watermark: form.watermark,
      markReferencedWorkitems: form.markReferencedWorkitems,
      cutEmptyChapters: form.cutEmptyChapters,
      cutEmptyWorkitemAttributes: form.cutEmptyWorkitemAttributes,
      cutLocalURLs: form.cutLocalURLs,
      followHTMLPresentationalHints: form.followHTMLPresentationalHints,
      specificChapters: form.specificChaptersEnabled ? form.specificChapters : null,
      metadataFields: form.metadataFieldsEnabled ? form.metadataFields : null,
      customNumberedListStyles: form.customListStylesEnabled ? form.customNumberedListStyles : null,
      language: form.localizeEnums ? form.language : null,
      linkedWorkitemRoles: form.rolesEnabled ? form.linkedWorkitemRoles : null,
      linkRoleDirection: form.rolesEnabled ? form.linkRoleDirection : null,
      exposePageWidthValidation: form.exposePageWidthValidation,
      attachmentsFilter: form.downloadAttachments ? form.attachmentsFilter : null,
      testcaseFieldId: form.downloadAttachments ? form.testcaseFieldId : null,
      embedAttachments: form.downloadAttachments && form.embedAttachments,
      fullFonts: form.fullFonts,
      workItemsQuery: form.workItemsQueryEnabled ? form.workItemsQuery : null,
    };
    try {
      await settings.saveContent(selectedConfig, scope, content);
      // The weight decides the order of the list, so the pane reloads it after a save, and the field
      // shows the value that was actually stored.
      patch({ weight });
      toast.success('Data successfully saved.');
      await paneRef.current?.reloadNames();
      setRevisionsToken((t) => t + 1);
    } catch (e) {
      toast.error((e as Error).message || 'Error occurred during saving the data.');
    }
  };

  const reload = async (revision?: string) => {
    if (!selectedConfig) return;
    const seq = ++latestLoad.current;
    const content = await settings.loadContent(selectedConfig, scope, revision);
    if (seq !== latestLoad.current) return;
    applyContent(content);
  };

  const handleCancel = async () => {
    if (!(await confirm('Are you sure you want to cancel editing and revert all changes made?'))) return;
    toast.dismiss();
    try {
      await reload();
    } catch {
      setLoadingError(true);
    }
  };

  const handleRevertToDefault = async () => {
    if (!(await confirm('Are you sure you want to return the default value?'))) return;
    toast.dismiss();
    const seq = ++latestLoad.current;
    try {
      const content = await settings.loadDefaultContent();
      if (seq !== latestLoad.current) return;
      applyContent(content);
      toast.success('Default values loaded. Save the data to apply them.');
    } catch {
      setLoadingError(true);
    }
  };

  /** The Default style package applies to every document, so it has no query to match one. */
  const matchingQueryShown = selectedConfig !== DEFAULT_NAME;

  return (
    <PageLayout title="PDF Exporter: Style Packages">
      <div className="notifications">
        {loadingError && (
          <div className="alert alert-error">
            Error occurred loading the data. Be sure Polarion is started and accessible.
          </div>
        )}
        {childNamesError && (
          <div className="alert alert-error">
            There was an error loading names of children configurations. Please, contact project/system administrator to
            solve the issue, a style package can&apos;t be configured without them.
          </div>
        )}
        {rolesError && <div className="alert alert-error">There was an error loading link role names.</div>}
      </div>

      <p>
        There can be multiple named style packages. Please, choose one you would like to modify in dropdown below. Be
        aware that &quot;Default&quot; style package on global scope can&apos;t be deleted or renamed.
      </p>

      <ConfigurationsPane<StylePackageSettings>
        ref={paneRef}
        scope={scope}
        service={settings}
        cookieKey={`selected-configuration-${FEATURE}`}
        label="style package"
        onContentLoaded={applyContent}
        onSelectedChange={setSelectedConfig}
        onEditingNameChange={setEditingName}
      />

      <fieldset className="style-packages-page" disabled={editingName}>
        {/* Weight and matching query: what decides the order of the list and which documents see it. */}
        <div className="flex-container section">
          <div className="flex-column">
            <div className="input-group flex-centered">
              <label htmlFor="style-package-weight">Weight:</label>
              <span className="more-info" title={WEIGHT_HELP} />
              <input
                id="style-package-weight"
                className="weight-input"
                type="number"
                min="1"
                max="100"
                step="0.1"
                value={form.weight}
                onChange={(e) => patch({ weight: e.target.value })}
                onBlur={() => patch({ weight: adjustWeight(form.weight) })}
              />
            </div>
          </div>
          {matchingQueryShown && (
            <div className="flex-grow" id="matching-query-container">
              <div className="input-group flex-centered">
                <label htmlFor="matching-query">Matching query:</label>
                <span className="more-info" title={MATCHING_QUERY_HELP} />
                <input
                  id="matching-query"
                  className="flex-grow matching-query-input"
                  type="text"
                  value={form.matchingQuery}
                  onChange={(e) => patch({ matchingQuery: e.target.value })}
                />
              </div>
            </div>
          )}
        </div>

        <div className="flex-container section">
          <div className="flex-column">
            <div className="checkbox input-group">
              <label htmlFor="exposeSettings">
                <input
                  id="exposeSettings"
                  type="checkbox"
                  checked={form.exposeSettings}
                  onChange={(e) => patch({ exposeSettings: e.target.checked })}
                />
                Expose style package settings to be redefined on UI
              </label>
            </div>
          </div>
        </div>

        {/* The other named settings this style package points at. */}
        <div className="flex-container">
          <div className="flex-column">
            <div className="checkbox input-group">
              <label htmlFor="cover-page-checkbox" className="cover-page-label">
                <input
                  id="cover-page-checkbox"
                  type="checkbox"
                  checked={form.coverPageEnabled}
                  onChange={(e) => patch({ coverPageEnabled: e.target.checked })}
                />
                Cover page
              </label>
              {form.coverPageEnabled && (
                <SearchableSelect
                  id="cover-page-select"
                  options={childNames['cover-page']}
                  loading={childNamesLoading}
                  value={childValue('cover-page', form.coverPage)}
                  onChange={(value) => patch({ coverPage: value })}
                />
              )}
            </div>
            <div className="input-group">
              <label htmlFor="css-select">CSS:</label>
              <SearchableSelect
                id="css-select"
                options={childNames.css}
                loading={childNamesLoading}
                value={childValue('css', form.css)}
                onChange={(value) => patch({ css: value })}
              />
            </div>
          </div>
          <div className="flex-column">
            <div className="input-group">
              <label htmlFor="header-footer-select">Header/Footer:</label>
              <SearchableSelect
                id="header-footer-select"
                options={childNames['header-footer']}
                loading={childNamesLoading}
                value={childValue('header-footer', form.headerFooter)}
                onChange={(value) => patch({ headerFooter: value })}
              />
            </div>
            <div className="input-group">
              <label htmlFor="localization-select">Localization:</label>
              <SearchableSelect
                id="localization-select"
                options={childNames.localization}
                loading={childNamesLoading}
                value={childValue('localization', form.localization)}
                onChange={(value) => patch({ localization: value })}
              />
            </div>
          </div>
        </div>

        {webhooksEnabled && (
          <div className="flex-container section">
            <div className="flex-column">
              <div className="checkbox input-group">
                <label htmlFor="webhooks-checkbox" className="webhooks-label">
                  <input
                    id="webhooks-checkbox"
                    type="checkbox"
                    checked={form.webhooksEnabled}
                    onChange={(e) => patch({ webhooksEnabled: e.target.checked })}
                  />
                  Use webhooks
                </label>
                {form.webhooksEnabled && (
                  <SearchableSelect
                    id="webhooks-select"
                    options={childNames.webhooks}
                    loading={childNamesLoading}
                    value={childValue('webhooks', form.webhooks)}
                    onChange={(value) => patch({ webhooks: value })}
                  />
                )}
              </div>
            </div>
          </div>
        )}

        {/* How the page itself is printed. */}
        <div className="flex-container section">
          <div className="flex-column">
            <div className="input-group">
              <label htmlFor="headers-color">Headings color:</label>
              <input
                id="headers-color"
                type="color"
                value={form.headersColor}
                onChange={(e) => patch({ headersColor: e.target.value })}
              />
            </div>
          </div>
          <div className="flex-column">
            <div className="input-group">
              <label htmlFor="paper-size-select">Paper Size:</label>
              <SearchableSelect
                id="paper-size-select"
                options={PAPER_SIZES}
                value={form.paperSize}
                onChange={(value) => patch({ paperSize: value })}
              />
            </div>
            <div className="input-group">
              <label htmlFor="orientation-select">Orientation:</label>
              <SearchableSelect
                id="orientation-select"
                options={ORIENTATIONS}
                value={form.orientation}
                onChange={(value) => patch({ orientation: value })}
              />
            </div>
            <div className="input-group">
              <label htmlFor="pdf-variant-select">PDF Variant:</label>
              <SearchableSelect
                id="pdf-variant-select"
                options={PDF_VARIANTS}
                value={form.pdfVariant}
                onChange={(value) => patch({ pdfVariant: value })}
              />
            </div>
            <div className="input-group">
              <label htmlFor="image-density-select">Image density:</label>
              <SearchableSelect
                id="image-density-select"
                options={IMAGE_DENSITIES}
                value={form.imageDensity}
                onChange={(value) => patch({ imageDensity: value })}
              />
            </div>
            {/* `flex-centered` for the same reason the weight and query rows have it: it is what centers
                the info icon against the text next to it. */}
            <div className="checkbox input-group flex-centered">
              <label htmlFor="full-fonts">
                <input
                  id="full-fonts"
                  type="checkbox"
                  checked={form.fullFonts}
                  onChange={(e) => patch({ fullFonts: e.target.checked })}
                />
                Embed full fonts (no subsetting)
              </label>
              <span className="more-info" title={FULL_FONTS_HELP} />
            </div>
          </div>
        </div>

        {/* What the renderer does with the content. */}
        <div className="flex-container">
          <div className="flex-column">
            <div className="checkbox input-group">
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
            <div className="checkbox input-group">
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
            <div className="checkbox input-group">
              <label htmlFor="render-comments">
                <input
                  id="render-comments"
                  type="checkbox"
                  checked={form.renderCommentsEnabled}
                  onChange={(e) => patch({ renderCommentsEnabled: e.target.checked })}
                />
                Comments rendering
              </label>
              {/* Kept in the layout while it is off, as the JSP page did: the row below it would jump
                  otherwise. */}
              <span className={form.renderCommentsEnabled ? 'render-comments-select' : 'render-comments-select hidden'}>
                <SearchableSelect
                  id="render-comments-select"
                  options={COMMENTS_RENDER_TYPES}
                  disabled={!form.renderCommentsEnabled}
                  value={form.renderComments}
                  onChange={(value) => patch({ renderComments: value })}
                />
              </span>
            </div>
            {form.renderCommentsEnabled && (
              <div className="checkbox input-group render-comments-options">
                <label
                  htmlFor="include-unreferenced-comments"
                  title="Unreferenced comments will be rendered at the end of the document"
                >
                  <input
                    id="include-unreferenced-comments"
                    type="checkbox"
                    checked={form.includeUnreferencedComments}
                    onChange={(e) => patch({ includeUnreferencedComments: e.target.checked })}
                  />
                  include unreferenced
                </label>
                <label
                  htmlFor="render-native-comments"
                  title="Comments will be transformed into native PDF sticky notes/bubbles"
                >
                  <input
                    id="render-native-comments"
                    type="checkbox"
                    checked={form.renderNativeComments}
                    onChange={(e) => patch({ renderNativeComments: e.target.checked })}
                  />
                  as sticky notes
                </label>
              </div>
            )}
            <div className="checkbox input-group">
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
          </div>
          <div className="flex-column">
            <div className="checkbox input-group">
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
            <div className="checkbox input-group">
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
            <div className="checkbox input-group">
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
            <div className="checkbox input-group">
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
          </div>
        </div>

        {/* Switches that carry a value of their own. */}
        <div className="flex-container">
          <div className="flex-column">
            <div className="checkbox input-group with-value">
              <label htmlFor="custom-list-styles">
                <input
                  id="custom-list-styles"
                  type="checkbox"
                  checked={form.customListStylesEnabled}
                  onChange={(e) => patch({ customListStylesEnabled: e.target.checked })}
                />
                Custom styles of numbered lists
              </label>
              <input
                id="numbered-list-styles"
                className={form.customListStylesEnabled ? 'grows' : 'grows hidden'}
                type="text"
                placeholder="eg. 1ai"
                disabled={!form.customListStylesEnabled}
                value={form.customNumberedListStyles}
                onChange={(e) => patch({ customNumberedListStyles: e.target.value })}
              />
            </div>
            <div className="checkbox input-group with-value">
              <label htmlFor="specific-chapters">
                <input
                  id="specific-chapters"
                  type="checkbox"
                  checked={form.specificChaptersEnabled}
                  onChange={(e) => patch({ specificChaptersEnabled: e.target.checked })}
                />
                Specific higher level chapters
              </label>
              <input
                id="chapters"
                className={form.specificChaptersEnabled ? 'grows' : 'grows hidden'}
                type="text"
                placeholder="eg. 1,2,4 etc."
                disabled={!form.specificChaptersEnabled}
                value={form.specificChapters}
                onChange={(e) => patch({ specificChapters: e.target.value })}
              />
            </div>
            <div className="checkbox input-group with-value">
              <label htmlFor="metadata-fields">
                <input
                  id="metadata-fields"
                  type="checkbox"
                  checked={form.metadataFieldsEnabled}
                  onChange={(e) => patch({ metadataFieldsEnabled: e.target.checked })}
                />
                Metadata fields
              </label>
              <input
                id="metadata-fields-input"
                className={form.metadataFieldsEnabled ? 'grows' : 'grows hidden'}
                type="text"
                placeholder="e.g. docOwner, docLanguage, customField*"
                disabled={!form.metadataFieldsEnabled}
                value={form.metadataFields}
                onChange={(e) => patch({ metadataFields: e.target.value })}
              />
            </div>
          </div>
          <div className="flex-column">
            <div className="checkbox input-group">
              <label htmlFor="localization">
                <input
                  id="localization"
                  type="checkbox"
                  checked={form.localizeEnums}
                  onChange={(e) => patch({ localizeEnums: e.target.checked })}
                />
                Localize enums
              </label>
              <span className={form.localizeEnums ? 'language-select' : 'language-select hidden'}>
                <SearchableSelect
                  id="language-select"
                  options={LANGUAGES}
                  disabled={!form.localizeEnums}
                  value={form.language}
                  onChange={(value) => patch({ language: value })}
                />
              </span>
            </div>
            <div className="checkbox input-group roles-group">
              <label htmlFor="selected-roles">
                <input
                  id="selected-roles"
                  type="checkbox"
                  checked={form.rolesEnabled}
                  onChange={(e) => patch({ rolesEnabled: e.target.checked })}
                />
                Specific Workitem roles
              </label>
              {form.rolesEnabled && (
                <>
                  <div className="roles-select">
                    <SearchableSelect
                      id="roles-select"
                      multiple
                      options={roleOptions}
                      loading={rolesLoading}
                      value={form.linkedWorkitemRoles}
                      onChange={(values) => patch({ linkedWorkitemRoles: values })}
                    />
                  </div>
                  <div className="roles-select">
                    <SearchableSelect
                      id="roles-direction-select"
                      options={LINK_ROLE_DIRECTIONS}
                      value={form.linkRoleDirection}
                      onChange={(value) => patch({ linkRoleDirection: value })}
                    />
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="flex-container section">
          <div className="flex-grow">
            <div className="input-group flex-centered work-items-query-group">
              <label htmlFor="work-items-query-checkbox">
                <input
                  id="work-items-query-checkbox"
                  type="checkbox"
                  checked={form.workItemsQueryEnabled}
                  onChange={(e) => patch({ workItemsQueryEnabled: e.target.checked })}
                />
                Work items query
              </label>
              <span className="more-info" title={WORK_ITEMS_QUERY_HELP} />
              <input
                id="work-items-query"
                className={form.workItemsQueryEnabled ? 'grows' : 'grows hidden'}
                type="text"
                placeholder="e.g. type:requirement"
                disabled={!form.workItemsQueryEnabled}
                value={form.workItemsQuery}
                onChange={(e) => patch({ workItemsQuery: e.target.value })}
              />
            </div>
          </div>
        </div>

        {/* Attachments of the exported work items. */}
        <div className="flex-container section">
          <div className="flex-column">
            <div className="checkbox input-group">
              <label htmlFor="download-attachments">
                <input
                  id="download-attachments"
                  type="checkbox"
                  checked={form.downloadAttachments}
                  onChange={(e) =>
                    patch({
                      downloadAttachments: e.target.checked,
                      // Switching it on with no filter yet means "every attachment", which the legacy
                      // page wrote into the field so the stored value says the same thing.
                      attachmentsFilter: e.target.checked && !form.attachmentsFilter ? '*.*' : form.attachmentsFilter,
                    })
                  }
                />
                Download attachments
              </label>
            </div>
            {form.downloadAttachments && (
              <div className="input-group">
                <label htmlFor="attachments-filter">Attachments filter:</label>
                <input
                  id="attachments-filter"
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
              <>
                <div className="checkbox input-group">
                  <label htmlFor="embed-attachments">
                    <input
                      id="embed-attachments"
                      type="checkbox"
                      checked={form.embedAttachments}
                      onChange={(e) => patch({ embedAttachments: e.target.checked })}
                    />
                    Embed attachments into resulted PDF
                  </label>
                </div>
                <div className="input-group">
                  <label htmlFor="testcase-field-id">Custom field ID:</label>
                  <input
                    id="testcase-field-id"
                    type="text"
                    title="A boolean testcase field ID. Attachments will be downloaded only from the testcases which have True value in the provided field. Leaving field empty will process all testcases."
                    value={form.testcaseFieldId}
                    onChange={(e) => patch({ testcaseFieldId: e.target.value })}
                  />
                </div>
              </>
            )}
          </div>
        </div>

        <h2 className="align-left">PDF Exporter dialog configuration</h2>
        <div className="flex-container">
          <div className="flex-column">
            <div className="checkbox input-group">
              <label htmlFor="expose-page-width-validation">
                <input
                  id="expose-page-width-validation"
                  type="checkbox"
                  checked={form.exposePageWidthValidation}
                  onChange={(e) => patch({ exposePageWidthValidation: e.target.checked })}
                />
                Expose page width validation controls
              </label>
            </div>
          </div>
        </div>

        <ConfigurationButtons
          onSave={() => void handleSave()}
          onCancel={() => void handleCancel()}
          onRevertToDefault={() => void handleRevertToDefault()}
          onToggleRevisions={() => setShowRevisions((v) => !v)}
          revisionsShown={showRevisions}
        />

        {showRevisions && selectedConfig && (
          <RevisionsTable
            name={selectedConfig}
            scope={scope}
            reloadToken={revisionsToken}
            loadRevisions={settings.loadRevisions}
            onRevert={(revision) => void reload(revision.name)}
          />
        )}
      </fieldset>
      {confirmDialog}
    </PageLayout>
  );
}
