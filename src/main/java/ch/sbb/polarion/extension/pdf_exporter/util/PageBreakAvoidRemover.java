package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.util.HtmlUtils;
import org.jetbrains.annotations.NotNull;

public class PageBreakAvoidRemover {
    private static final String PAGE_BREAK_AVOID_TABLE_START = "<table style=\"page-break-inside:avoid;\">";
    private static final String TABLE_START = "<table";
    private static final String TABLE_END = "</table>";
    private static final String CELL_START = "<td>";
    private static final String CELL_END = "</td>";

    @NotNull
    String removePageBreakAvoids(@NotNull String html) {
        // Polarion wraps content of a work item as it is into table's cell with table's styling "page-break-inside: avoid"
        // if it's configured to avoid page breaks:
        //
        // <table style="page-break-inside:avoid;">
        //   <tr>
        //     <td>
        //       <CONTENT>
        //     </td>
        //   </tr>
        // </table>
        //
        // This styling "page-break-inside: avoid" doesn't influence rendering by pd4ml converter,
        // but breaks rendering of tables with help of WeasyPrint. More over this configuration was initially introduced
        // for pd4ml converter because table headers are not repeated at page start when table takes more than 1 page.
        // Last drawback is not applied to WeasyPrint and thus such workaround can be safely removed.
        //
        // Taking into account that work item content can also contain tables this task should be done with cautious.
        // Removing "page-break-inside:avoid;" from table's styling doesn't help, tables are still broken. So, solution
        // is to remove that table wrapping at all. As a result above example should become just:
        //
        // <CONTENT>
        //

        StringBuilder result = new StringBuilder();

        int tableStart = html.indexOf(PAGE_BREAK_AVOID_TABLE_START);

        int tableEnd = HtmlUtils.getEnding(html, tableStart, TABLE_START, TABLE_END);
        if (tableStart >= 0 && tableEnd > 0) {
            result.append(html, 0, tableStart);
        } else {
            return html;
        }

        while (tableStart >= 0 && tableEnd > 0) {
            String tableExcerpt = html.substring(tableStart, tableEnd);
            int cellStart = tableExcerpt.indexOf(CELL_START);
            int cellEnd = tableExcerpt.lastIndexOf(CELL_END);

            // Insert into result buffer content of extracted table between opening and closing most outer td-tag
            result.append(tableExcerpt, cellStart + CELL_START.length(), cellEnd);

            tableStart = html.indexOf(PAGE_BREAK_AVOID_TABLE_START, tableEnd);
            if (tableEnd < (html.length() - 1)) {
                result.append(html, tableEnd, tableStart < 0 ? html.length() : tableStart);
            }
            tableEnd = HtmlUtils.getEnding(html, tableStart, TABLE_START, TABLE_END);
        }

        return result.toString();
    }

}
