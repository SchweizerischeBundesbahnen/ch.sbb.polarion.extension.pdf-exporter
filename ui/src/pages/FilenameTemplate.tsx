import CustomTemplatesPage from '../components/CustomTemplatesPage';
import TemplateQuickHelp from '../components/TemplateQuickHelp';

/**
 * PDF Exporter: Filename template - the templates the exported file is named after. One setting, no
 * named configurations, which is why the page carries no configuration selector.
 */
export default function FilenameTemplate() {
  return (
    <CustomTemplatesPage
      title="PDF Exporter: Filename template"
      feature="filename-template"
      named={false}
      defaultLabel="Use default templates"
      customLabel="Use custom templates"
      customIntro="Here you can define your custom filename templates. Choose them above to use them instead of the default ones. An empty template uses the default one."
      defaultIntro="Here are displayed default filename templates, which are used unless the custom ones are chosen above. They are displayed here only for informational purposes and can't be modified."
      editorsClassName="three-across"
      fields={[
        {
          key: 'documentNameTemplate',
          label: 'Document filename template:',
          language: 'velocity',
          placeholder: 'Enter file name template for exported Live Document, or leave empty for the default one',
        },
        {
          key: 'reportNameTemplate',
          label: 'Report filename template:',
          language: 'velocity',
          placeholder: 'Enter file name template for exported Live Report, or leave empty for the default one',
        },
        {
          key: 'testRunNameTemplate',
          label: 'Test run filename template:',
          language: 'velocity',
          placeholder: 'Enter file name template for exported Test Run, or leave empty for the default one',
        },
      ]}
      footer={
        <TemplateQuickHelp title="How to configure Filename template">
          <p>
            Filenames can be made scriptable by incorporating placeholders and velocity code. These filenames can
            contain Velocity expressions that are dynamically evaluated, allowing for the inclusion of dynamic values.
          </p>
          <p>
            Each template gets the object it names: <span className="monospace">$document</span> for a Live Document,{' '}
            <span className="monospace">$page</span> for a Live Report and <span className="monospace">$testrun</span>{' '}
            for a Test Run. <span className="monospace">$projectName</span> is there for all three.
          </p>
          <p>
            For example, in the report template:{' '}
            <span className="monospace">{'{{ PROJECT_NAME }} $page.spaceId $page.titleOrName $page.lastRevision'}</span>
          </p>
        </TemplateQuickHelp>
      }
    />
  );
}
