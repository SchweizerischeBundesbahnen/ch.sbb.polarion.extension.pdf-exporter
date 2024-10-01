package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.alm.server.api.model.rp.widget.AbstractWidgetRenderer;
import com.polarion.alm.shared.api.model.rp.parameter.BooleanParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.ui.shared.LinearGradientColor;
import com.polarion.core.util.StringUtils;
import com.polarion.platform.persistence.model.IPObjectList;
import org.jetbrains.annotations.NotNull;

import static ch.sbb.polarion.extension.pdf_exporter.widgets.BulkPdfExportWidget.*;

public class BulkPdfExportWidgetRenderer extends AbstractWidgetRenderer {
    private PdfExporterPolarionService polarionService = new PdfExporterPolarionService();

    private final String projectId;
    private final boolean includeDocuments;
    private final boolean includeReports;
    private final boolean includeTestRuns;

    public BulkPdfExportWidgetRenderer(@NotNull RichPageWidgetCommonContext context) {
        super(context);

        projectId = context.getDisplayedScope().projectId();

        includeDocuments = ((BooleanParameter) context.parameter(INCLUDE_DOCUMENTS)).value();
        includeReports = ((BooleanParameter) context.parameter(INCLUDE_REPORTS)).value();
        includeTestRuns = ((BooleanParameter) context.parameter(INCLUDE_TEST_RUNS)).value();
    }

    @Override
    protected void render(@NotNull HtmlFragmentBuilder builder) {
        HtmlTagBuilder wrap = builder.tag().div();
        wrap.attributes().className("polarion-PdfExporter-BulkExportWidget");

        HtmlTagBuilder header = wrap.append().tag().div();
        header.attributes().className("header");

        renderExportButton(header);

        HtmlTagBuilder description = header.append().tag().div();
        description.append().tag().p().append().text("Please select items below which you want to export and click button above");

        HtmlTagBuilder exportItems = wrap.append().tag().div();
        exportItems.attributes().className("export-items");

        if (includeDocuments) {
            final HtmlTagBuilder columnContent = renderColumn(exportItems, "Documents:");
            //language=JS
            columnContent.attributes().onClick("BulkPdfExporter.updateBulkExportButton(this);");
            IPObjectList<IModule> documents = polarionService.getDocuments(projectId);
            if (documents.isEmpty()) {
                renderNoData(columnContent, "No documents found");
            } else {
                documents.forEach(document -> renderExportItem(columnContent, new ExportItem(document)));
            }
        }

        if (includeReports) {
            final HtmlTagBuilder columnContent = renderColumn(exportItems, "Reports:");
            //language=JS
            columnContent.attributes().onClick("BulkPdfExporter.updateBulkExportButton(this);");
            IPObjectList<IRichPage> reports = polarionService.getReports(projectId);
            if (reports.isEmpty()) {
                renderNoData(columnContent, "No reports found");
            } else {
                reports.forEach(report -> renderExportItem(columnContent, new ExportItem(report)));
            }
        }

        if (includeTestRuns) {
            final HtmlTagBuilder columnContent = renderColumn(exportItems, "Test Runs:");
            //language=JS
            columnContent.attributes().onClick("BulkPdfExporter.updateBulkExportButton(this);");
            IPObjectList<ITestRun> testRuns = polarionService.getTestRuns(projectId);
            if (testRuns.isEmpty()) {
                renderNoData(columnContent, "No test runs found");
            } else {
                testRuns.forEach(testRun -> renderExportItem(columnContent, new ExportItem(testRun)));
            }
        }
    }

    private void renderExportButton(@NotNull HtmlTagBuilder header) {
        LinearGradientColor color = new LinearGradientColor();
        HtmlTagBuilder buttonSpan = header.append().tag().span();
        buttonSpan.attributes().className("polarion-TestsExecutionButton-link").title("Please, select at least one item to be exported first");

        HtmlTagBuilder a = buttonSpan.append().tag().a();
        HtmlTagBuilder button = a.append().tag().div();
        button.attributes().className("polarion-TestsExecutionButton-buttons polarion-TestsExecutionButton-buttons-defaultCursor").style(color.getStyle());

        HtmlTagBuilder content = button.append().tag().table();
        content.attributes().className("polarion-TestsExecutionButton-buttons-content");

        HtmlTagBuilder labelCell = content.append().tag().tr().append().tag().td();
        labelCell.attributes().className("polarion-TestsExecutionButton-buttons-content-labelCell");
        HtmlTagBuilder label = labelCell.append().tag().div();
        label.attributes().className("polarion-TestsExecutionButton-labelTextNew");

        label.append().text("Export to PDF");
    }

    private @NotNull HtmlTagBuilder renderColumn(@NotNull HtmlTagBuilder wrap, @NotNull String title) {
        HtmlTagBuilder column = wrap.append().tag().div();
        column.attributes().className("column");
        HtmlTagBuilder header = column.append().tag().h3();
        header.append().text(title);
        HtmlTagBuilder columnContent = column.append().tag().div();
        columnContent.attributes().className("column-content");
        return columnContent;
    }

    private void renderNoData(@NotNull HtmlTagBuilder columnContent, @NotNull String noDataLabel) {
        HtmlTagBuilder noData = columnContent.append().tag().div();
        noData.attributes().className("no-data");
        noData.append().text(noDataLabel);
    }

    private void renderExportItem(@NotNull HtmlTagBuilder columnContent, @NotNull ExportItem exportItem) {
        HtmlTagBuilder itemDiv = columnContent.append().tag().div();
        HtmlTagBuilder label = itemDiv.append().tag().byName("label");
        HtmlTagBuilder checkbox = label.append().tag().byName("input");
        checkbox.attributes()
                .byName("type", "checkbox")
                .className("export-item")
                .byName("data-type", exportItem.type)
                .byName("data-space", exportItem.space)
                .byName("data-id", exportItem.id);
        label.append().text(exportItem.title);
    }

    private static class ExportItem {
        private final String space;
        private final String id;
        private final String title;
        private final String type;

        ExportItem(IModule document) {
            space = document.getModuleFolder();
            id = document.getId();
            title = document.getModuleNameWithSpace();
            type = document.getPrototype().getName();
        }

        ExportItem(IRichPage report) {
            space = report.getSpaceId();
            id = report.getId();
            title = report.getPageNameWithSpace();
            type = report.getPrototype().getName();
        }

        ExportItem(ITestRun testRun) {
            space = null;
            id = testRun.getId();
            title = testRun.getId() + (!StringUtils.isEmpty(testRun.getTitle()) ? " / " + testRun.getTitle() : "");
            type = testRun.getPrototype().getName();
        }
    }
}
