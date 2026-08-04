import type { BulkExportItems, WidgetShim } from './types';

/**
 * The data the dev harness and the tests mount the widget with. It mirrors what the endpoint answers
 * for a widget listing test runs, cells included: those arrive as the HTML Polarion rendered.
 */
export const SAMPLE_SHIM: WidgetShim = {
  descriptor: 'ZGV2LWhhcm5lc3M',
  signature: 'dev-harness',
  title: 'Test Runs',
  documentType: 'TEST_RUN',
  exportPages: false,
};

const testRun = (id: string, status: string, template: string, author: string, created: string) => ({
  readable: true,
  type: 'TestRun',
  projectId: 'elibrary',
  spaceId: null,
  id,
  name: null,
  cells: [
    `<span class="polarion-JSRichTextEditor-link"><a href="#">${id}</a></span>`,
    `<span>${status}</span>`,
    `<a href="#">${template}</a>`,
    `<a href="#">${author}</a>`,
    `<span>${created}</span>`,
  ],
});

export const SAMPLE_ITEMS: BulkExportItems = {
  columns: [
    { id: 'id', label: 'ID' },
    { id: 'status', label: 'Status' },
    { id: 'template', label: 'Template' },
    { id: 'author', label: 'Author' },
    { id: 'created', label: 'Created' },
  ],
  items: [
    testRun('build_quick-20170211-141155', 'Passed', 'xUnit Build Test', 'Melanie Test', '2017-02-11 13:11'),
    testRun('0_9b RT', 'Verified Passed', 'Release Test', 'Melanie Test', '2017-05-18 11:03'),
    testRun('0_9b FMST', 'Verified Passed', 'Full Manual System Test', 'Melanie Test', '2016-02-04 18:16'),
    testRun('1_0 UAT', 'Failed', 'User Acceptance Test', 'Melanie Test', '2017-05-18 11:34'),
  ],
  totalCount: 4,
  countMessage: '4 items found',
  openInTableUrl: '/polarion/#/project/elibrary/testruns',
  query: 'type:testrun AND project.id:elibrary',
};

export const SAMPLE_ITEMS_WITH_UNREADABLE: BulkExportItems = {
  ...SAMPLE_ITEMS,
  items: [
    ...SAMPLE_ITEMS.items.slice(0, 2),
    { readable: false, message: 'You do not have permission to read this item' },
  ],
  totalCount: 3,
  countMessage: '3 items found',
};

export const SAMPLE_ITEMS_EMPTY: BulkExportItems = {
  ...SAMPLE_ITEMS,
  items: [],
  totalCount: 0,
  countMessage: '0 items found',
};

export const SAMPLE_ITEMS_TRUNCATED: BulkExportItems = {
  ...SAMPLE_ITEMS,
  totalCount: 120,
  countMessage: 'Showing 4 of 120 items',
};
