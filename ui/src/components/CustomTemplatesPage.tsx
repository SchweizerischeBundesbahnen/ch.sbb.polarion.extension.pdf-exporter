import { type ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import {
  CodeEditor,
  type CodeLanguage,
  ConfigurationButtons,
  ConfigurationsPane,
  type ConfigurationsPaneHandle,
  PageLayout,
  RevisionsTable,
  Tabs,
  useConfirm,
} from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';
import { getScope } from '../services/scope';
import useNamedSettings from '../services/settings';

/** One editable template of the page: which field of the settings document it edits, and how it looks. */
export interface TemplateField {
  /** Key in the settings document, e.g. `documentNameTemplate` or `headerLeft`. */
  key: string;
  label: string;
  language: CodeLanguage;
  placeholder?: string;
}

/** The settings documents these pages edit all carry the opt-in flag plus one string per field. */
export type TemplateSettings = Record<string, string | boolean> & { useCustomValues?: boolean };

interface CustomTemplatesPageProps {
  title: string;
  /** Named-settings feature id, e.g. `header-footer`. */
  feature: string;
  /** Whether the feature has named configurations (all but the filename templates do). */
  named?: boolean;
  /** Label of the opt-in checkbox, e.g. "Use custom templates". */
  optInLabel: string;
  customIntro: ReactNode;
  defaultIntro: ReactNode;
  fields: TemplateField[];
  /** Extra content below the editors, e.g. the supported-placeholders table. */
  footer?: ReactNode;
  /** Class on the editor grid, so a page can lay its fields out (three across, two rows of three...). */
  editorsClassName?: string;
}

/** The single always-present setting of a feature that has no named configurations. */
const DEFAULT_NAME = 'Default';

/**
 * The shape three administration pages of this extension share: an opt-in checkbox, then two tabs -
 * the custom templates, editable, and the built-in ones read-only for reference - over one named
 * settings document.
 *
 * Filename template, header & footer and cover page differ only in which fields they edit and in
 * their explanatory copy, so they are three thin pages around this one component rather than three
 * copies of it. It stays here rather than in react-sbb-polarion: the shape is this extension's (and
 * docx-exporter's), not something every extension has.
 */
export default function CustomTemplatesPage({
  title,
  feature,
  named = true,
  optInLabel,
  customIntro,
  defaultIntro,
  fields,
  footer,
  editorsClassName,
}: Readonly<CustomTemplatesPageProps>) {
  const scope = getScope();
  const settings = useNamedSettings<TemplateSettings>(feature);
  const { confirm, confirmDialog } = useConfirm();
  const paneRef = useRef<ConfigurationsPaneHandle>(null);

  /**
   * Which load is the current one. Filling the editors is a request the administrator can outrun -
   * by typing, by picking another configuration, by reverting to a revision - and the slowest
   * response would otherwise land last and win. Only the newest one writes.
   */
  const latestLoad = useRef(0);

  const [values, setValues] = useState<Record<string, string>>({});
  const [defaults, setDefaults] = useState<Record<string, string>>({});
  const [useCustomValues, setUseCustomValues] = useState(false);
  const [selectedConfig, setSelectedConfig] = useState<string | null>(named ? null : DEFAULT_NAME);
  const [editingName, setEditingName] = useState(false);
  const [activeTab, setActiveTab] = useState<'custom' | 'default'>('custom');
  const [showRevisions, setShowRevisions] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);
  // Two independent reads feed this page - the built-in values and the selected configuration - and a
  // banner that says "could not read the data" must not be taken down by whichever of them succeeded
  // last. Each owns its flag; the banner is their union.
  const [defaultsError, setDefaultsError] = useState(false);
  const [contentError, setContentError] = useState(false);
  const loadingError = defaultsError || contentError;

  const toValues = useCallback(
    (content: TemplateSettings): Record<string, string> =>
      Object.fromEntries(fields.map((f) => [f.key, String(content[f.key] ?? '')])),
    [fields],
  );

  const applyContent = useCallback(
    (content: TemplateSettings) => {
      latestLoad.current += 1;
      setValues(toValues(content));
      setUseCustomValues(!!content.useCustomValues);
      // A load that succeeded after an earlier failure would otherwise keep the banner up over good
      // data, telling the administrator the page could not read what it is showing.
      setContentError(false);
    },
    [toValues],
  );

  // The built-in templates: the same document for every configuration, so fetched once.
  useEffect(() => {
    let cancelled = false;
    settings
      .loadDefaultContent()
      .then((content) => {
        if (cancelled) return;
        setDefaults(toValues(content));
        setDefaultsError(false);
      })
      .catch(() => {
        if (!cancelled) setDefaultsError(true);
      });
    return () => {
      cancelled = true;
    };
  }, [settings, toValues]);

  // A page without named configurations loads its single setting itself; with them, the pane does it.
  useEffect(() => {
    if (named) return;
    let cancelled = false;
    settings
      .loadContent(DEFAULT_NAME, scope)
      .then((content) => {
        if (!cancelled) applyContent(content);
      })
      .catch(() => {
        if (!cancelled) setContentError(true);
      });
    return () => {
      cancelled = true;
    };
  }, [named, settings, scope, applyContent]);

  const handleSave = async () => {
    if (!selectedConfig) return;
    toast.dismiss();
    // Turning the opt-in off clears the templates, exactly as the legacy pages did: the stored
    // document then says "not in use" rather than keeping values nothing reads.
    const content: TemplateSettings = { useCustomValues };
    for (const field of fields) {
      content[field.key] = useCustomValues ? (values[field.key] ?? '') : '';
    }
    try {
      await settings.saveContent(selectedConfig, scope, content);
      toast.success('Data successfully saved.');
      if (named) await paneRef.current?.reloadNames();
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
      setContentError(true);
    }
  };

  const editors = (readOnly: boolean) => (
    <div className={editorsClassName ? `template-editors ${editorsClassName}` : 'template-editors'}>
      {fields.map((field) => (
        <div className="template-editor" key={field.key}>
          <div className="label-block">
            <label htmlFor={`${readOnly ? 'default' : 'custom'}-${field.key}`}>{field.label}</label>
          </div>
          <CodeEditor
            language={field.language}
            id={`${readOnly ? 'default' : 'custom'}-${field.key}`}
            value={(readOnly ? defaults : values)[field.key] ?? ''}
            onChange={(value) => setValues((current) => ({ ...current, [field.key]: value }))}
            placeholder={readOnly ? undefined : field.placeholder}
            readOnly={readOnly}
          />
        </div>
      ))}
    </div>
  );

  return (
    <PageLayout title={title}>
      <div className="notifications">
        {loadingError && (
          <div className="alert alert-error">
            Error occurred loading the data. Be sure Polarion is started and accessible.
          </div>
        )}
      </div>

      {named && (
        <ConfigurationsPane<TemplateSettings>
          ref={paneRef}
          scope={scope}
          service={settings}
          cookieKey={`selected-configuration-${feature}`}
          onContentLoaded={applyContent}
          onSelectedChange={setSelectedConfig}
          onEditingNameChange={setEditingName}
        />
      )}

      <fieldset className="templates-page" disabled={editingName}>
        <div className="checkbox input-group">
          <label htmlFor="use-custom-values">
            <input
              id="use-custom-values"
              type="checkbox"
              checked={useCustomValues}
              onChange={(e) => setUseCustomValues(e.target.checked)}
            />
            {optInLabel}
          </label>
        </div>

        <Tabs
          items={[
            { id: 'custom', label: 'Custom Templates' },
            { id: 'default', label: 'Default Templates' },
          ]}
          activeId={activeTab}
          onSelect={(id) => setActiveTab(id as 'custom' | 'default')}
          name={`${feature}-tab`}
          ariaLabel={title}
        />

        <div className="tab-panel">
          <p>{activeTab === 'custom' ? customIntro : defaultIntro}</p>
          {editors(activeTab === 'default')}
        </div>

        <ConfigurationButtons
          onSave={() => void handleSave()}
          onCancel={() => void handleCancel()}
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

        {footer}
      </fieldset>
      {confirmDialog}
    </PageLayout>
  );
}
