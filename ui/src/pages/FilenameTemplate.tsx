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
      optInLabel="Use custom templates"
      customIntro="Here you can define your custom filename templates, and force them to be used instead of default ones by ticking checkbox above."
      defaultIntro="Here are displayed default filename templates, which will be used unless checkbox above is ticked. They are displayed here only for informational purposes and can't be modified."
      editorsClassName="three-across"
      fields={[
        {
          key: 'documentNameTemplate',
          label: 'Document filename template:',
          language: 'velocity',
          placeholder: 'Enter file name template for exported Live Document',
        },
        {
          key: 'reportNameTemplate',
          label: 'Report filename template:',
          language: 'velocity',
          placeholder: 'Enter file name template for exported Live Report',
        },
        {
          key: 'testRunNameTemplate',
          label: 'Test run filename template:',
          language: 'velocity',
          placeholder: 'Enter file name template for exported Test Run',
        },
      ]}
      footer={<Placeholders />}
    />
  );
}
