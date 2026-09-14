import CustomTemplatesPage from '../components/CustomTemplatesPage';
import Placeholders from '../components/Placeholders';

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
      footer={<Placeholders />}
    />
  );
}
