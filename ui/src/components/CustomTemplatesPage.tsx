import { type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  CodeEditor,
  type CodeLanguage,
  ConfigurationButtons,
  ConfigurationsPane,
  type ConfigurationsPaneHandle,
  PageLayout,
  RevisionsTable,
  SearchableSelect,
  Tabs,
  useConfirm,
} from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';
import { getScope } from '../services/scope';
import useNamedSettings from '../services/settings';
import CompareWithDefault from './CompareWithDefault';

/** One editable template of the page: which field of the settings document it edits, and how it looks. */
export interface TemplateField {
  /** Key in the settings document, e.g. `documentNameTemplate` or `headerLeft`. */
  key: string;
  label: string;
  language: CodeLanguage;
  placeholder?: string;
}

/**
 * The settings documents these pages edit: the choice between built-in and custom values, one string per field,
 * the hash of the built-in values the custom ones were copied from, and whether those changed since.
 */
export type TemplateSettings = Record<string, string | boolean | undefined> & {
  useCustomValues?: boolean;
  defaultHash?: string;
  /** The built-in template the custom ones were copied from, for a page with more than one. */
  defaultSource?: string;
  defaultChanged?: boolean;
};

/** Where "Copy" takes the templates from, for a page with more built-in templates than the default one. */
export interface CopySources {
  options: Array<{ id: string; name: string }>;
  initial: string;
  /** Reads one of them, to compare with or to review against: nothing is persisted. */
  load: (id: string) => Promise<TemplateSettings>;
  /** Copies one of them into a configuration, which gets the images the template refers to. */
  copy: (id: string, configuration: string | null) => Promise<TemplateSettings>;
}

interface CustomTemplatesPageProps {
  title: string;
  /** Named-settings feature id, e.g. `header-footer`. */
  feature: string;
  /** Whether the feature has named configurations (all but the filename templates do). */
  named?: boolean;
  /** Label of the choice to use the built-in templates, e.g. "Use default cover page". */
  defaultLabel: string;
  /** Label of the choice to use the custom templates, e.g. "Use custom cover page". */
  customLabel: string;
  customIntro: ReactNode;
  defaultIntro: ReactNode;
  fields: TemplateField[];
  /** Extra content below the editors, e.g. the supported-placeholders table. */
  footer?: ReactNode;
  /** Class on the editor grid, so a page can lay its fields out (three across, two rows of three...). */
  editorsClassName?: string;
  /** The built-in templates to copy from; without it "Copy from default" copies the default ones. */
  copySources?: CopySources;
  /** Asked before saving custom templates in use which are empty. No question without it. */
  emptyWarning?: string;
  /** Whether the custom templates count as empty for `emptyWarning`. All fields blank without it. */
  isEmpty?: (values: Record<string, string>) => boolean;
}

/** The single always-present setting of a feature that has no named configurations. */
const DEFAULT_NAME = 'Default';

const hashOf = (content: TemplateSettings): string | undefined =>
  typeof content.defaultHash === 'string' ? content.defaultHash : undefined;
const sourceOf = (content: TemplateSettings): string | undefined =>
  typeof content.defaultSource === 'string' ? content.defaultSource : undefined;

/**
 * The shape three administration pages of this extension share: the choice between the built-in and the custom
 * templates, then two tabs - the custom templates and the built-in ones read-only for reference - over one named
 * settings document.
 *
 * The custom templates are stored whatever the choice says, and the choice alone decides which ones an export
 * uses. Only the tab of the chosen templates opens, the other one is disabled; the custom templates are kept while
 * the built-in ones are chosen, and show again once the custom ones are. A custom template usually
 * starts as a copy of a built-in one, so the page copies one on request, remembers which version it copied, and
 * compares the custom templates with the built-in ones once a newer version of the extension changed those.
 *
 * Filename template, header & footer and cover page differ only in which fields they edit, in their explanatory
 * copy and in where a copy comes from, so they are three thin pages around this one component rather than three
 * copies of it. It stays here rather than in react-sbb-polarion: the shape is this extension's (and
 * docx-exporter's), not something every extension has.
 */
export default function CustomTemplatesPage({
  title,
  feature,
  named = true,
  defaultLabel,
  customLabel,
  customIntro,
  defaultIntro,
  fields,
  footer,
  editorsClassName,
  copySources,
  emptyWarning,
  isEmpty,
}: Readonly<CustomTemplatesPageProps>) {
  const scope = getScope();
  // A new configuration starts with empty templates, not in use: the built-in ones apply until the administrator
  // writes some. Keyed by the field names, since the pages hand a new `fields` array to every render.
  const fieldKeys = fields.map((field) => field.key).join(',');
  const initialContent = useMemo<TemplateSettings>(
    () => ({ useCustomValues: false, ...Object.fromEntries(fieldKeys.split(',').map((key) => [key, ''])) }),
    [fieldKeys],
  );
  const settings = useNamedSettings<TemplateSettings>(feature, initialContent);
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
  const [defaultHash, setDefaultHash] = useState<string | undefined>(undefined);
  const [defaultSource, setDefaultSource] = useState<string | undefined>(undefined);
  const [defaultChanged, setDefaultChanged] = useState(false);
  const [copySource, setCopySource] = useState(copySources?.initial ?? '');
  // The built-in values shown in the comparison, and the predefined template they are, when the page has several.
  const [comparison, setComparison] = useState<{ values: Record<string, string>; template?: string } | null>(null);
  const [selectedConfig, setSelectedConfig] = useState<string | null>(named ? null : DEFAULT_NAME);
  const [editingName, setEditingName] = useState(false);
  // The default templates apply until a configuration says otherwise, so their tab is the one open.
  const [activeTab, setActiveTab] = useState<'custom' | 'default'>('default');
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
      // A configuration opens on the templates it uses: those are the ones an export gets.
      setActiveTab(content.useCustomValues ? 'custom' : 'default');
      setDefaultHash(hashOf(content));
      setDefaultSource(sourceOf(content));
      setDefaultChanged(!!content.defaultChanged);
      // A load that succeeded after an earlier failure would otherwise keep the banner up over good
      // data, telling the administrator the page could not read what it is showing.
      setContentError(false);
    },
    [toValues],
  );

  // The template the custom templates were copied from, when it is still one of the sources.
  const knownSource =
    defaultSource && copySources?.options.some((option) => option.id === defaultSource) ? defaultSource : undefined;

  // The sources arrive after the page, once their list is read. The one the custom templates were copied from is offered first.
  useEffect(() => {
    if (copySources) setCopySource(knownSource ?? copySources.initial);
  }, [copySources, knownSource]);

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

  /** Chooses the templates an export uses, and shows them: the built-in ones to read, the custom ones to edit. */
  const chooseTemplates = (custom: boolean) => {
    setUseCustomValues(custom);
    setActiveTab(custom ? 'custom' : 'default');
  };

  const hasCustomValues = fields.some((field) => (values[field.key] ?? '').trim() !== '');

  // Only a copy persists what a predefined template brings along, reading one for a comparison or a review does not.
  // A comparison is against the template chosen in "Copy from", which starts on the one the custom templates were copied
  // from. A review is against that one whatever "Copy from" shows, since it re-bases the custom templates onto it.
  const readBuiltIn = (template: string) => (copySources ? copySources.load(template) : settings.loadDefaultContent());
  const copyBuiltIn = () =>
    copySources ? copySources.copy(copySource, selectedConfig) : settings.loadDefaultContent();

  // Copy, compare and review wait for a request, during which another configuration may be loaded: a result which
  // belongs to the previous one is dropped, the way a stale load is (see latestLoad).
  const handleCopy = async () => {
    const seq = latestLoad.current;
    if (
      hasCustomValues &&
      !(await confirm('Are you sure you want to replace the custom templates with the default ones?'))
    ) {
      return;
    }
    try {
      const content = await copyBuiltIn();
      if (seq !== latestLoad.current) return;
      latestLoad.current += 1;
      setValues(toValues(content));
      setDefaultHash(hashOf(content));
      setDefaultSource(sourceOf(content));
      setDefaultChanged(false);
    } catch {
      toast.error('Error occurred loading the default templates.');
    }
  };

  const handleCompare = async () => {
    const seq = latestLoad.current;
    const template = copySource;
    try {
      const content = await readBuiltIn(template);
      if (seq !== latestLoad.current) return;
      setComparison({ values: toValues(content), template: copySources ? template : undefined });
    } catch {
      toast.error('Error occurred loading the default templates.');
    }
  };

  /** Takes the current built-in templates as the ones the custom templates are up to date with. */
  const handleMarkReviewed = async () => {
    const seq = latestLoad.current;
    try {
      const content = await readBuiltIn(knownSource ?? copySource);
      if (seq !== latestLoad.current) return;
      setDefaultHash(hashOf(content));
      setDefaultSource(sourceOf(content));
      setDefaultChanged(false);
      toast.success('Marked as reviewed. Remember to save the configuration.');
    } catch {
      toast.error('Error occurred loading the default templates.');
    }
  };

  const changedDefaultMessage = () => {
    const review = 'Compare them to take over what you need, then mark the change as reviewed.';
    if (!copySources) return `The default templates changed since the custom ones were copied from them. ${review}`;
    if (knownSource)
      return `The template "${knownSource}" changed since the custom templates were copied from it. ${review}`;
    return `The template the custom templates were copied from changed since. Choose it in "Copy from" first. ${review}`;
  };

  const handleSave = async () => {
    if (!selectedConfig) return;
    toast.dismiss();
    const empty = isEmpty ? isEmpty(values) : !hasCustomValues;
    if (useCustomValues && emptyWarning && empty && !(await confirm(emptyWarning))) return;
    // The templates are stored whatever the choice says: switching to the built-in ones must not throw away
    // what the administrator wrote.
    const content: TemplateSettings = { useCustomValues };
    for (const field of fields) {
      content[field.key] = values[field.key] ?? '';
    }
    if (defaultHash) content.defaultHash = defaultHash;
    if (defaultSource) content.defaultSource = defaultSource;
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

  const editors = (readOnly: boolean) => {
    const classes = ['template-editors', editorsClassName].filter(Boolean);
    return (
      <div className={classes.join(' ')}>
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
  };

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
        <div className="mode-options input-group" role="radiogroup" aria-label={title}>
          <label htmlFor="use-default-values">
            <input
              id="use-default-values"
              type="radio"
              name={`${feature}-mode`}
              checked={!useCustomValues}
              onChange={() => chooseTemplates(false)}
            />
            {defaultLabel}
          </label>
          <label htmlFor="use-custom-values">
            <input
              id="use-custom-values"
              type="radio"
              name={`${feature}-mode`}
              checked={useCustomValues}
              onChange={() => chooseTemplates(true)}
            />
            {customLabel}
          </label>
        </div>

        {useCustomValues && defaultChanged && (
          <div className="alert alert-warning default-changed">
            {changedDefaultMessage()}
            <button
              type="button"
              className="sbb-btn sbb-btn--control mark-as-reviewed"
              onClick={() => void handleMarkReviewed()}
            >
              <span>Mark as reviewed</span>
            </button>
          </div>
        )}

        <Tabs
          items={[
            // Only the tab of the templates an export uses opens: the other ones do not apply.
            { id: 'custom', label: 'Custom Templates', disabled: !useCustomValues },
            { id: 'default', label: 'Default Templates', disabled: useCustomValues },
          ]}
          activeId={activeTab}
          onSelect={(id) => setActiveTab(id as 'custom' | 'default')}
          name={`${feature}-tab`}
          ariaLabel={title}
        />

        <div className="tab-panel">
          <p>{activeTab === 'custom' ? customIntro : defaultIntro}</p>
          {activeTab === 'custom' && (
            <div className="template-actions">
              {copySources && (
                <>
                  <label htmlFor="copy-source-select">Copy from:</label>
                  <SearchableSelect
                    id="copy-source-select"
                    value={copySource}
                    onChange={setCopySource}
                    options={copySources.options}
                    searchable={false}
                  />
                </>
              )}
              <button
                type="button"
                className="sbb-btn sbb-btn--control copy-from-default"
                onClick={() => void handleCopy()}
              >
                <span>{copySources ? 'Copy' : 'Copy from default'}</span>
              </button>
              <button
                type="button"
                className="sbb-btn sbb-btn--control compare-with-default-button"
                onClick={() => void handleCompare()}
              >
                <span>Compare with default</span>
              </button>
            </div>
          )}
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
      <CompareWithDefault
        open={comparison !== null}
        fields={fields}
        custom={values}
        builtIn={comparison?.values ?? {}}
        defaultLabel={comparison?.template ? `Default: ${comparison.template}` : undefined}
        onClose={() => setComparison(null)}
      />
      {confirmDialog}
    </PageLayout>
  );
}
