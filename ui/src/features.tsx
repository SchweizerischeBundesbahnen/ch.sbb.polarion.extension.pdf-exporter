import type { ComponentType } from 'react';
import About from './pages/About';
import Authorization from './pages/Authorization';
import CoverPage from './pages/CoverPage';
import Css from './pages/Css';
import Disclaimer from './pages/Disclaimer';
import FilenameTemplate from './pages/FilenameTemplate';
import HeaderFooter from './pages/HeaderFooter';
import StylePackageWeights from './pages/StylePackageWeights';
import UserGuide from './pages/UserGuide';

/**
 * A single navigable page of the app. The `id` is what appears in the URL as `?feature=<id>` and is
 * also what `hivemodule.xml` points its admin extenders at, so the ids here and the extender ids must
 * stay identical - a typo is a blank page in Polarion and no test catches it.
 *
 * The administration menu has more entries than the list below: the ones still served by JSP (style
 * packages, the Velocity/CSS editors, localization, webhooks) keep pointing at the
 * `pdf-exporter-admin` webapp until they are converted too.
 */
export interface Feature {
  id: string;
  label: string;
  description: string;
  component: ComponentType;
}

export const FEATURES: Feature[] = [
  {
    id: 'about',
    label: 'About',
    description: 'Extension version and general information.',
    component: About,
  },
  {
    id: 'disclaimer',
    label: 'Usage Disclaimer',
    description: 'The terms this extension is provided under.',
    component: Disclaimer,
  },
  {
    id: 'user-guide',
    label: 'User Guide',
    description: 'How to use the extension, generated from USER_GUIDE.md.',
    component: UserGuide,
  },
  {
    id: 'css',
    label: 'CSS',
    description: 'The custom stylesheet appended to the generated PDFs.',
    component: Css,
  },
  {
    id: 'cover-page',
    label: 'Cover Page',
    description: 'The HTML and CSS of the page printed before the document.',
    component: CoverPage,
  },
  {
    id: 'header-footer',
    label: 'Header and Footer',
    description: 'The six cells printed on every page of the exported PDF.',
    component: HeaderFooter,
  },
  {
    id: 'filename',
    label: 'Filename template',
    description: 'How exported documents, reports and test runs are named.',
    component: FilenameTemplate,
  },
  {
    id: 'style-package-weights',
    label: 'Style Package Weights',
    description: 'Order the style packages; the top one is preselected on the export panel.',
    component: StylePackageWeights,
  },
  {
    id: 'authorization',
    label: 'Authorization',
    description: 'Configure which global and project roles are allowed to export.',
    component: Authorization,
  },
];

export function findFeature(id: string | null): Feature | undefined {
  return FEATURES.find((f) => f.id === id);
}
