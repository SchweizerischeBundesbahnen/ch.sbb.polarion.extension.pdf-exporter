import CustomTemplatesPage from '../components/CustomTemplatesPage';
import Placeholders from '../components/Placeholders';

/** PDF Exporter: Header and footer - the six cells printed on every page of the exported PDF. */
export default function HeaderFooter() {
  return (
    <CustomTemplatesPage
      title="PDF Exporter: Header and Footer"
      feature="header-footer"
      optInLabel="Use custom header and footer"
      customIntro="Here you can define your custom header and footer, and force them to be used instead of default ones by ticking checkbox above."
      defaultIntro="Here are displayed default header and footer, which will be used unless checkbox above is ticked. They are displayed here only for informational purposes and can't be modified."
      editorsClassName="three-across"
      fields={[
        {
          key: 'headerLeft',
          label: "Header's left part:",
          language: 'velocity',
          placeholder: "Enter template of header's left part here",
        },
        {
          key: 'headerCenter',
          label: "Header's center part:",
          language: 'velocity',
          placeholder: "Enter template of header's center part here",
        },
        {
          key: 'headerRight',
          label: "Header's right part:",
          language: 'velocity',
          placeholder: "Enter template of header's right part here",
        },
        {
          key: 'footerLeft',
          label: "Footer's left part:",
          language: 'velocity',
          placeholder: "Enter template of footer's left part here",
        },
        {
          key: 'footerCenter',
          label: "Footer's center part:",
          language: 'velocity',
          placeholder: "Enter template of footer's center part here",
        },
        {
          key: 'footerRight',
          label: "Footer's right part:",
          language: 'velocity',
          placeholder: "Enter template of footer's right part here",
        },
      ]}
      footer={<Placeholders />}
    />
  );
}
