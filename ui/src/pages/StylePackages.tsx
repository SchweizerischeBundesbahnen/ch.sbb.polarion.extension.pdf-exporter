import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ConfigurationButtons,
  ConfigurationsPane,
  type ConfigurationsPaneHandle,
  PageLayout,
  RevisionsTable,
  type SelectOption,
  type SettingName,
  useConfirm,
} from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';
import StylePackageSettingsView from '../export/StylePackageSettingsView';
import '../export/export-form-layout.css';
import { FieldCell, FieldRow, SwitchRow } from '../export/formRows';
import { getScope } from '../services/scope';
import useNamedSettings from '../services/settings';
import {
  CHILD_SETTINGS,
  type ChildNames,
  type ChildSetting,
  DEFAULT_HEADERS_COLOR,
  DEFAULT_IMAGE_DENSITY,
  DEFAULT_NAME,
  DEFAULT_ORIENTATION,
  DEFAULT_PAPER_SIZE,
  DEFAULT_PDF_VARIANT,
  DEFAULT_RENDER_COMMENTS,
  NO_CHILD_NAMES,
  type StylePackageSettings,
  type StylePackageVisibility,
  VISIBILITY_FEATURE,
} from '../services/stylePackage';
import useRemote from '../services/useRemote';

const FEATURE = 'style-package';

const WEIGHT_HELP =
  'A float number from 0.0 to 100, which will determine the position of current style package in the ' +
  'resulting style packages list. The higher the number, the higher its position will be.';

const MATCHING_QUERY_HELP =
  'A query to select documents to which this style package will be relevant. For documents not matching ' +
  "this query the style package won't be visible. If you want to make this style package be available to " +
  'all documents, just leave this field empty.';

const LANGUAGE_CUSTOM_FIELD_HELP =
  'ID of the LiveDoc custom field that holds the document language. Its value is used as-is as the ISO 639-1 ' +
  "code (e.g. 'de', 'en', 'fr') and injected into the exported HTML as the <html lang> attribute. The field may be a " +
  "text field or an enumeration; for an enumeration the option's ID is used, so the option IDs must be ISO 639-1 " +
  'codes. Leave empty to disable language injection.';

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
  firstPageHeaderFooterEnabled: boolean;
  firstPageHeaderFooter: string;
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
  languageCustomField: string;
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
 * options: the shared `inherited` flag, which the dropdown renders as a small italic "global" on the
 * right of the option and turns the names of this scope bold.
 */
function toOption(name: SettingName, scope: string): SelectOption {
  return { id: name.name, name: name.name, inherited: name.scope !== scope };
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
    firstPageHeaderFooterEnabled: !!content.firstPageHeaderFooter,
    firstPageHeaderFooter: content.firstPageHeaderFooter ?? DEFAULT_NAME,
    localization: content.localization ?? DEFAULT_NAME,
    webhooksEnabled: !!content.webhooks,
    webhooks: content.webhooks ?? DEFAULT_NAME,
    headersColor: content.headersColor ?? DEFAULT_HEADERS_COLOR,
    paperSize: content.paperSize ?? DEFAULT_PAPER_SIZE,
    orientation: content.orientation ?? DEFAULT_ORIENTATION,
    pdfVariant: content.pdfVariant ?? DEFAULT_PDF_VARIANT,
    imageDensity: content.imageDensity ?? DEFAULT_IMAGE_DENSITY,
    fullFonts: !!content.fullFonts,
    fitToPage: !!content.fitToPage,
    followHTMLPresentationalHints: !!content.followHTMLPresentationalHints,
    renderCommentsEnabled: !!content.renderComments,
    renderComments: content.renderComments ?? DEFAULT_RENDER_COMMENTS,
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
    languageCustomField: content.languageCustomField ?? '',
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
  const visibility = useNamedSettings<StylePackageVisibility>(VISIBILITY_FEATURE);
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

  // Which style packages this scope offers at all: a document of its own, above every package, edited
  // through the dialog the pane opens from its "Change visibility" button. The page keeps the value
  // because the pane neither reads nor stores it - it only shows it and hands back what was picked.
  const [hideGlobalPackages, setHideGlobalPackages] = useState(false);
  const [visibilityInherited, setVisibilityInherited] = useState(false);
  const [visibilityLoaded, setVisibilityLoaded] = useState(false);
  const [visibilityError, setVisibilityError] = useState(false);

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

  /** Whether the scope stores the visibility document itself, or reads the one of the global scope. */
  const loadVisibilityOwnership = useCallback(async () => {
    if (scope === '') {
      setVisibilityInherited(false);
      return;
    }
    try {
      const names = await visibility.loadConfigurationNames(scope);
      setVisibilityInherited(!names.some((name) => name.scope === scope));
    } catch {
      // Purely informational: a failure here must not stand in the way of reading or writing the switch.
      setVisibilityInherited(false);
    }
  }, [scope, visibility]);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const content = await visibility.loadContent(DEFAULT_NAME, scope);
        if (cancelled) return;
        setHideGlobalPackages(!!content.hideGlobalStylePackages);
        setVisibilityError(false);
      } catch {
        if (cancelled) return;
        setVisibilityError(true);
      }
      setVisibilityLoaded(true);
      await loadVisibilityOwnership();
    })();
    return () => {
      cancelled = true;
    };
  }, [visibility, scope, loadVisibilityOwnership]);

  /**
   * Stores what the pane's visibility dialog was set to. The pane reloads its list of packages once this
   * resolves, which is what takes the inherited packages out of the dropdown and puts the "Default" the
   * project just received into it.
   *
   * A failure is rethrown on purpose: the dialog stays open and says so. The toast is still worth it - it
   * carries the message of the backend ("no permission for this project"), which the dialog does not.
   */
  const saveVisibility = async (hidden: boolean) => {
    toast.dismiss();
    try {
      await visibility.saveContent(DEFAULT_NAME, scope, { hideGlobalStylePackages: hidden });
    } catch (e) {
      toast.error((e as Error).message || 'Error occurred during saving the data.');
      throw e;
    }
    setHideGlobalPackages(hidden);
    toast.success('Data successfully saved.');
    await loadVisibilityOwnership();
  };

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
      firstPageHeaderFooter: form.firstPageHeaderFooterEnabled
        ? childValue('header-footer', form.firstPageHeaderFooter)
        : null,
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
      languageCustomField: form.languageCustomField.trim() || null,
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
        {visibilityError && (
          <div className="alert alert-error">
            There was an error loading the visibility of the style packages of the global level.
          </div>
        )}
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
        visibility={{
          globalHidden: hideGlobalPackages,
          onChange: saveVisibility,
          // Nothing to change while the stored value is unknown, so the button stays out of reach
          disabled: !visibilityLoaded || visibilityError,
          note: (
            <>
              <p>
                The style package named &quot;Default&quot; can never be missing: as long as this project has none of
                its own, it is the built-in one of the extension. Save a &quot;Default&quot; on this project to decide
                what it contains.
              </p>
              {visibilityInherited && (
                <p>
                  <i>This value is the one inherited from the global scope. Updating it stores it on this project.</i>
                </p>
              )}
            </>
          ),
        }}
      />

      <fieldset className="style-packages-page" disabled={editingName}>
        {/* The rows of the export form, so that a style package reads here as it does where it is used (#1178). */}
        <div className="pdf-export-form pdf-exporter">
          {/* Weight and matching query: what decides the order of the list and which documents see it. */}
          <div className="pdf-section single-column group-start">
            <FieldRow
              label={
                <>
                  Weight:
                  <span className="more-info" title={WEIGHT_HELP} />
                </>
              }
              labelFor="style-package-weight"
            >
              <FieldCell>
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
              </FieldCell>
            </FieldRow>
            {matchingQueryShown && (
              <FieldRow
                rowId="matching-query-container"
                label={
                  <>
                    Matching query:
                    <span className="more-info" title={MATCHING_QUERY_HELP} />
                  </>
                }
                labelFor="matching-query"
              >
                <FieldCell grows>
                  <input
                    id="matching-query"
                    type="text"
                    value={form.matchingQuery}
                    onChange={(e) => patch({ matchingQuery: e.target.value })}
                  />
                </FieldCell>
              </FieldRow>
            )}
          </div>

          <div className="pdf-section single-column group-start">
            <SwitchRow
              id="exposeSettings"
              label="Expose style package settings to be redefined on UI"
              checked={form.exposeSettings}
              onChange={(checked) => patch({ exposeSettings: checked })}
            />
          </div>

          {/* The settings themselves, as the export dialog and the Document Properties pane show them. Every row
              is offered: a style package serves every document type, whichever of them shows a row. */}
          <div className="settings-block group-start">
            <StylePackageSettingsView
              ids=""
              form={form}
              onPatch={patch}
              childNames={childNames}
              childNamesLoading={childNamesLoading}
              roles={roleOptions}
              rolesLoading={rolesLoading}
              webhooksEnabled={!!webhooksEnabled}
              shows={() => true}
              busy={false}
            />
          </div>

          {/* Which language a document is written in: a field of the document, rather than a setting of the export */}
          <div className="pdf-section single-column group-start group-end">
            <FieldRow
              className="tight"
              label={
                <>
                  Document Language custom field
                  <span className="more-info" title={LANGUAGE_CUSTOM_FIELD_HELP} />
                </>
              }
              labelFor="language-custom-field"
            >
              <FieldCell grows>
                <input
                  id="language-custom-field"
                  type="text"
                  placeholder="docLanguage"
                  value={form.languageCustomField}
                  onChange={(e) => patch({ languageCustomField: e.target.value })}
                />
              </FieldCell>
            </FieldRow>
          </div>

          {/* A test run's own attachments. No other document type is exported with any. */}
          <h2 className="align-left">Test Run attachments</h2>
          <p>
            These options apply to the export of a Test Run only. Its attachments are downloaded next to the PDF, or
            embedded into it, and the mask and the test case field below say which of them. An export of a Live
            Document, a Live Report or a Wiki page carries no attachments, whatever is chosen here.
          </p>
          {/* Download and embed side by side, the mask and the test case field under them, as before */}
          <div className="pdf-section group-end">
            <SwitchRow
              id="download-attachments"
              label="Download attachments"
              checked={form.downloadAttachments}
              onChange={(checked) =>
                patch({
                  downloadAttachments: checked,
                  // Switching it on with no filter yet means "every attachment", which the legacy page wrote
                  // into the field so the stored value says the same thing.
                  attachmentsFilter: checked && !form.attachmentsFilter ? '*.*' : form.attachmentsFilter,
                })
              }
            />
            {form.downloadAttachments && (
              <>
                <SwitchRow
                  id="embed-attachments"
                  label="Embed attachments into resulted PDF"
                  checked={form.embedAttachments}
                  onChange={(checked) => patch({ embedAttachments: checked })}
                />
                <FieldRow label="Attachments filter:" labelFor="attachments-filter">
                  <FieldCell grows>
                    <input
                      id="attachments-filter"
                      type="text"
                      title="Filter for attachments to be downloaded, example: '*.pdf'"
                      placeholder="*.*"
                      value={form.attachmentsFilter}
                      onChange={(e) => patch({ attachmentsFilter: e.target.value })}
                    />
                  </FieldCell>
                </FieldRow>
                <FieldRow
                  label="Custom field ID:"
                  labelFor="testcase-field-id"
                  title="A boolean testcase field ID. Attachments will be downloaded only from the testcases which have True value in the provided field. Leaving field empty will process all testcases."
                >
                  <FieldCell grows>
                    <input
                      id="testcase-field-id"
                      type="text"
                      value={form.testcaseFieldId}
                      onChange={(e) => patch({ testcaseFieldId: e.target.value })}
                    />
                  </FieldCell>
                </FieldRow>
              </>
            )}
          </div>

          <h2 className="align-left">PDF Exporter dialog configuration</h2>
          <div className="pdf-section single-column">
            <SwitchRow
              id="expose-page-width-validation"
              label="Expose page width validation controls"
              checked={form.exposePageWidthValidation}
              onChange={(checked) => patch({ exposePageWidthValidation: checked })}
            />
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
