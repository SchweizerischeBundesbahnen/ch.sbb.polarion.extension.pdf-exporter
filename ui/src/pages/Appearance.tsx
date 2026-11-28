import { useCallback, useEffect, useState } from 'react';
import {
  ConfigurationButtons,
  PageLayout,
  type Revision,
  RevisionsTable,
  useConfirm,
} from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';
import { getScope } from '../services/scope';
import useNamedSettings from '../services/settings';

/** The named-settings feature the button configuration is stored under. */
const FEATURE = 'appearance';

/** The single setting name the generic framework uses when a feature has no named configurations. */
const DEFAULT_NAME = 'Default';

/** Content of the `appearance` setting, as the backend's AppearanceModel serializes it. */
interface AppearanceContent {
  liveDocButton?: boolean;
  liveReportButton?: boolean;
  liveReportAutoExpandTools?: boolean;
  classicWikiButton?: boolean;
  classicWikiAutoExpandTools?: boolean;
}

/** What the page edits: the same flags, each resolved to a value the checkboxes can bind to. */
interface AppearanceState {
  liveDocButton: boolean;
  liveReportButton: boolean;
  liveReportAutoExpandTools: boolean;
  classicWikiButton: boolean;
  classicWikiAutoExpandTools: boolean;
}

const ALL_BUTTONS: AppearanceState = {
  liveDocButton: true,
  liveReportButton: true,
  liveReportAutoExpandTools: false,
  classicWikiButton: true,
  classicWikiAutoExpandTools: false,
};

function toState(content: AppearanceContent): AppearanceState {
  return {
    liveDocButton: content.liveDocButton ?? ALL_BUTTONS.liveDocButton,
    liveReportButton: content.liveReportButton ?? ALL_BUTTONS.liveReportButton,
    liveReportAutoExpandTools: content.liveReportAutoExpandTools ?? ALL_BUTTONS.liveReportAutoExpandTools,
    classicWikiButton: content.classicWikiButton ?? ALL_BUTTONS.classicWikiButton,
    classicWikiAutoExpandTools: content.classicWikiAutoExpandTools ?? ALL_BUTTONS.classicWikiAutoExpandTools,
  };
}

/**
 * PDF Exporter: Appearance - which "Export to PDF" buttons the extension puts on a Polarion page.
 *
 * One setting named `Default` per scope. A project without one reads the Default Repository setting,
 * which is what the settings framework falls back to, so the Default Repository configures every
 * project at once. A project overrides it by saving its own, and goes back to inheriting by ticking
 * "Use the configuration from the Default Repository", which deletes it.
 */
export default function Appearance() {
  const scope = getScope();
  const projectScope = scope !== '';
  const settings = useNamedSettings<AppearanceContent>(FEATURE);
  const { confirm, confirmDialog } = useConfirm();

  const [state, setState] = useState<AppearanceState>(ALL_BUTTONS);
  const [inherited, setInherited] = useState(false);
  const [hasOwnSetting, setHasOwnSetting] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [loadingError, setLoadingError] = useState(false);
  const [showRevisions, setShowRevisions] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);

  /**
   * Whether this scope stores a setting of its own. `readNames` answers with one `Default` entry
   * whose `scope` is the scope the file was found in, so the entry naming this scope is the
   * project's own file and anything else is the Default Repository's.
   */
  const readOwnership = useCallback(async (): Promise<boolean> => {
    if (!projectScope) {
      return true;
    }
    const names = await settings.loadConfigurationNames(scope);
    return names.some((name) => name.name === DEFAULT_NAME && name.scope === scope);
  }, [settings, scope, projectScope]);

  const load = useCallback(
    async (revision?: string) => {
      // The content read for a project scope already falls back to the Default Repository, so the
      // inherited values are what the controls show while "use the global configuration" is ticked.
      const [own, content] = await Promise.all([readOwnership(), settings.loadContent(DEFAULT_NAME, scope, revision)]);
      setHasOwnSetting(own);
      setInherited(!own);
      setState(toState(content));
      setLoadingError(false);
    },
    [readOwnership, settings, scope],
  );

  useEffect(() => {
    let cancelled = false;
    load()
      .catch(() => {
        if (!cancelled) setLoadingError(true);
      })
      .finally(() => {
        if (!cancelled) setLoaded(true);
      });
    return () => {
      cancelled = true;
    };
  }, [load]);

  const patch = (change: Partial<AppearanceState>) => setState((current) => ({ ...current, ...change }));

  /**
   * Switches a toolbar button on or off, and clears its "keep the tools toolbar expanded" option when it
   * goes off. The option only has an effect while the button is drawn, so leaving it ticked behind a
   * disabled control would read as something that still happens.
   */
  const patchButton = (button: 'liveReportButton' | 'classicWikiButton', display: boolean) => {
    const expand = button === 'liveReportButton' ? 'liveReportAutoExpandTools' : 'classicWikiAutoExpandTools';
    patch(display ? { [button]: true } : { [button]: false, [expand]: false });
  };

  const handleSave = async () => {
    toast.dismiss();
    try {
      if (inherited) {
        // Inheriting is the absence of a setting in this scope, so it is stored by deleting the
        // project's own file rather than by writing a flag into it.
        if (hasOwnSetting) {
          await settings.deleteConfiguration(DEFAULT_NAME, scope);
        }
        await load();
        toast.success('This project now uses the configuration of the Default Repository.');
      } else {
        await settings.saveContent(DEFAULT_NAME, scope, state);
        setHasOwnSetting(true);
        toast.success('Data successfully saved.');
      }
      setRevisionsToken((t) => t + 1);
    } catch (e) {
      toast.error((e as Error).message || 'Error occurred during saving the data.');
    }
  };

  const handleCancel = async () => {
    if (!(await confirm('Are you sure you want to cancel editing and revert all changes made?'))) return;
    toast.dismiss();
    try {
      await load();
    } catch {
      setLoadingError(true);
    }
  };

  const handleRevertToDefault = async () => {
    if (!(await confirm('Are you sure you want to return the default values?'))) return;
    toast.dismiss();
    try {
      setState(toState(await settings.loadDefaultContent()));
      setInherited(false);
      toast.success('Default values loaded. Save the data to apply them.');
    } catch {
      setLoadingError(true);
    }
  };

  const handleRevertToRevision = async (revision: Revision) => {
    toast.dismiss();
    try {
      setState(toState(await settings.loadContent(DEFAULT_NAME, scope, revision.name)));
      setInherited(false);
      toast.success(`Reverted to revision ${revision.name}. Remember to save the configuration.`);
    } catch {
      setLoadingError(true);
    }
  };

  if (!loaded) {
    return (
      <PageLayout title="PDF Exporter: Appearance">
        <p>Loading...</p>
      </PageLayout>
    );
  }

  return (
    <PageLayout title="PDF Exporter: Appearance">
      <div className="notifications">
        {loadingError && (
          <div className="alert alert-error">
            Error occurred loading the data. Be sure Polarion is started and accessible.
          </div>
        )}
      </div>

      <div className="appearance-page">
        {projectScope && (
          <label className="appearance-inherit" htmlFor="appearance-inherit">
            <input
              id="appearance-inherit"
              type="checkbox"
              checked={inherited}
              onChange={(e) => setInherited(e.target.checked)}
            />
            Use the configuration from the Default Repository
          </label>
        )}

        <fieldset className="appearance-options" disabled={inherited}>
          <h2 className="align-left">Document editor</h2>
          <label htmlFor="live-doc-button">
            <input
              id="live-doc-button"
              type="checkbox"
              checked={state.liveDocButton}
              onChange={(e) => patch({ liveDocButton: e.target.checked })}
            />
            Display the &quot;Export to PDF&quot; toolbar button
          </label>

          <h2 className="align-left">Live Report</h2>
          <label htmlFor="live-report-button">
            <input
              id="live-report-button"
              type="checkbox"
              checked={state.liveReportButton}
              onChange={(e) => patchButton('liveReportButton', e.target.checked)}
            />
            Display the &quot;Export to PDF&quot; toolbar button
          </label>
          <label className="appearance-sub-option" htmlFor="live-report-expand">
            <input
              id="live-report-expand"
              type="checkbox"
              checked={state.liveReportAutoExpandTools}
              disabled={!state.liveReportButton}
              onChange={(e) => patch({ liveReportAutoExpandTools: e.target.checked })}
            />
            Keep the tools toolbar expanded
          </label>

          <h2 className="align-left">Classic Wiki</h2>
          <label htmlFor="classic-wiki-button">
            <input
              id="classic-wiki-button"
              type="checkbox"
              checked={state.classicWikiButton}
              onChange={(e) => patchButton('classicWikiButton', e.target.checked)}
            />
            Display the &quot;Export to PDF&quot; toolbar button
          </label>
          <label className="appearance-sub-option" htmlFor="classic-wiki-expand">
            <input
              id="classic-wiki-expand"
              type="checkbox"
              checked={state.classicWikiAutoExpandTools}
              disabled={!state.classicWikiButton}
              onChange={(e) => patch({ classicWikiAutoExpandTools: e.target.checked })}
            />
            Keep the tools toolbar expanded
          </label>
        </fieldset>

        <ConfigurationButtons
          onSave={() => void handleSave()}
          onCancel={() => void handleCancel()}
          onRevertToDefault={() => void handleRevertToDefault()}
          onToggleRevisions={() => setShowRevisions((v) => !v)}
          revisionsShown={showRevisions}
        />

        {showRevisions && (
          <RevisionsTable
            name={DEFAULT_NAME}
            scope={scope}
            reloadToken={revisionsToken}
            loadRevisions={settings.loadRevisions}
            onRevert={(revision) => void handleRevertToRevision(revision)}
          />
        )}
      </div>

      {confirmDialog}

      <div className="quick-help">
        <h2 className="align-left">Quick Help</h2>
        <div className="quick-help-text">
          <p>
            Here you choose where the extension puts its &quot;Export to PDF&quot; button. A button which is switched
            off is not drawn at all; the export itself stays available through the Document Properties side panel, the
            report widgets and the REST API.
          </p>
          <h3>Scope</h3>
          <p>
            What you save in the Default Repository applies to every project which has no configuration of its own. A
            project administrator overrides it on this page in the project, and returns to the global configuration by
            ticking &quot;Use the configuration from the Default Repository&quot; and saving.
          </p>
          <h3>Keep the tools toolbar expanded</h3>
          <p>
            Polarion renders the Live Report and Classic Wiki toolbars collapsed behind an &quot;Expand Tools&quot;
            handle on every page open, and does not remember that you expanded them. Tick this and the extension expands
            the toolbar for you, so its button is visible without a click.
          </p>
        </div>
      </div>
    </PageLayout>
  );
}
