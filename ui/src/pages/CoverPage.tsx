import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import CustomTemplatesPage, { type CopySources, type TemplateSettings } from '../components/CustomTemplatesPage';
import { getScope } from '../services/scope';
import useRemote from '../services/useRemote';

const FEATURE = 'cover-page';
/** The predefined template the default cover page is, and so the one a copy starts from. */
const DEFAULT_TEMPLATE = 'English';

/**
 * PDF Exporter: Cover page - the HTML and CSS of the page printed before the document.
 *
 * The extension ships predefined templates, and the default cover page is one of them. A custom cover page is
 * copied from any of them, into the configuration being edited.
 */
export default function CoverPage() {
  const { sendRequest } = useRemote();
  const scope = getScope();
  const [templates, setTemplates] = useState<string[]>([]);

  useEffect(() => {
    let cancelled = false;
    sendRequest({ method: 'GET', url: `/settings/${FEATURE}/templates` })
      .then(async (response) => {
        if (cancelled) return;
        if (!response.ok) {
          toast.error('Error occurred loading the list of predefined templates.');
          return;
        }
        setTemplates((await response.json()) as string[]);
      })
      .catch(() => {
        if (!cancelled) toast.error('Error occurred loading the list of predefined templates.');
      });
    return () => {
      cancelled = true;
    };
  }, [sendRequest]);

  const copySources = useMemo<CopySources | undefined>(
    () =>
      templates.length === 0
        ? undefined
        : {
            options: templates.map((name) => ({ id: name, name })),
            initial: templates.includes(DEFAULT_TEMPLATE) ? DEFAULT_TEMPLATE : templates[0],
            load: async (template: string, configuration: string | null) => {
              // POST: the images of the template are persisted for the configuration the template is copied into
              const query = `scope=${encodeURIComponent(scope)}${configuration ? `&name=${encodeURIComponent(configuration)}` : ''}`;
              const response = await sendRequest({
                method: 'POST',
                url: `/settings/${FEATURE}/templates/${encodeURIComponent(template)}/content?${query}`,
                contentType: 'application/json',
              });
              if (!response.ok) throw new Error(`Cannot read the predefined template '${template}'`);
              return (await response.json()) as TemplateSettings;
            },
          },
    [templates, sendRequest, scope],
  );

  return (
    <CustomTemplatesPage
      title="PDF Exporter: Cover Page"
      feature={FEATURE}
      defaultLabel="Use default cover page"
      customLabel="Use custom cover page"
      customIntro="Here you can define your custom cover page. Choose it above to use it instead of the default one. To start from a predefined template, copy it and edit the copy."
      defaultIntro="Here is displayed the default cover page, which is used unless the custom one is chosen above. It is displayed here only for informational purposes and can't be modified."
      editorsClassName="two-across"
      copySources={copySources}
      emptyWarning="The custom cover page has no HTML, so the exported PDF gets a blank cover page. Save anyway?"
      isEmpty={(values) => (values.templateHtml ?? '').trim() === ''}
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
    />
  );
}
