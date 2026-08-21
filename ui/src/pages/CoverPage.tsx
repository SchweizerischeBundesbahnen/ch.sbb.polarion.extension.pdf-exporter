import { useCallback, useEffect, useState } from 'react';
import { SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';
import CustomTemplatesPage from '../components/CustomTemplatesPage';
import { getScope } from '../services/scope';
import useRemote from '../services/useRemote';

const FEATURE = 'cover-page';

/**
 * The predefined cover-page templates shipped with the extension. Persisting one writes it into the
 * current scope as a named configuration, which is why the configuration list is reloaded afterwards -
 * the page below it then offers the new entry.
 *
 * The pane hides itself when the extension ships none, exactly as the JSP page did.
 */
function PredefinedTemplates({ onPersisted }: Readonly<{ onPersisted: () => void }>) {
  const { sendRequest } = useRemote();
  const scope = getScope();
  const [templates, setTemplates] = useState<string[]>([]);
  const [selected, setSelected] = useState('');

  useEffect(() => {
    let cancelled = false;
    sendRequest({ method: 'GET', url: `/settings/${FEATURE}/templates` })
      .then(async (response) => {
        if (cancelled || !response.ok) {
          if (!cancelled) toast.error('Error occurred loading the list of predefined templates.');
          return;
        }
        const names = (await response.json()) as string[];
        setTemplates(names);
        setSelected(names[0] ?? '');
      })
      .catch(() => {
        if (!cancelled) toast.error('Error occurred loading the list of predefined templates.');
      });
    return () => {
      cancelled = true;
    };
  }, [sendRequest]);

  const persist = useCallback(async () => {
    const response = await sendRequest({
      method: 'POST',
      url: `/settings/${FEATURE}/templates/${encodeURIComponent(selected)}?scope=${encodeURIComponent(scope)}`,
      contentType: 'application/json',
    });
    if (response.ok) {
      toast.success('Template successfully persisted.');
      onPersisted();
    } else {
      toast.error('Error occurred while persisting the template.');
    }
  }, [sendRequest, selected, scope, onPersisted]);

  if (templates.length === 0) return null;

  return (
    <div className="predefined-templates">
      <h2 className="align-left">Predefined Templates</h2>
      <p>
        In addition to a default template, there are more predefined ones which you can persist. Please select one from
        the dropdown below and click &apos;Persist&apos; button.
      </p>
      <div className="predefined-templates-row">
        <label htmlFor="templates-select">Predefined Templates:</label>
        <SearchableSelect
          id="templates-select"
          value={selected}
          onChange={setSelected}
          options={templates.map((name) => ({ id: name, name }))}
        />
        <button type="button" className="sbb-btn sbb-btn--control" onClick={() => void persist()}>
          <span className="button-image sbb-icon-save" aria-hidden="true" />
          <span>Persist</span>
        </button>
      </div>
    </div>
  );
}

/** PDF Exporter: Cover page - the HTML and CSS of the page printed before the document. */
export default function CoverPage() {
  // Persisting a predefined template adds a configuration, so the page has to re-read the list.
  const [reloadToken, setReloadToken] = useState(0);

  return (
    <CustomTemplatesPage
      key={reloadToken}
      title="PDF Exporter: Cover Page"
      feature={FEATURE}
      optInLabel="Use custom cover page"
      customIntro="Here you can define your custom cover page, and force it to be used instead of the default one by ticking checkbox above."
      defaultIntro="Here is displayed the default cover page, which will be used unless checkbox above is ticked. It is displayed here only for informational purposes and can't be modified."
      editorsClassName="two-across"
      fields={[
        {
          key: 'templateHtml',
          label: 'HTML:',
          language: 'velocity',
          placeholder: 'Enter HTML part of cover page template here',
        },
        {
          key: 'templateCss',
          label: 'CSS:',
          language: 'css',
          placeholder: 'Enter CSS part of cover page template here',
        },
      ]}
      footer={<PredefinedTemplates onPersisted={() => setReloadToken((t) => t + 1)} />}
    />
  );
}
