package ch.sbb.polarion.extension.pdf_exporter.util.placeholder;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.Placeholder;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataHelper;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.tracker.model.IModule;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PlaceholderProcessor {

    private final PdfExporterPolarionService pdfExporterPolarionService;
    private final DocumentDataHelper documentDataHelper;

    public PlaceholderProcessor() {
        this.pdfExporterPolarionService = new PdfExporterPolarionService();
        this.documentDataHelper = new DocumentDataHelper(pdfExporterPolarionService);
    }

    public PlaceholderProcessor(PdfExporterPolarionService pdfExporterPolarionService, DocumentDataHelper documentDataHelper) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
        this.documentDataHelper = documentDataHelper;
    }

    public @NotNull String replacePlaceholders(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams, @NotNull String template) {
        PlaceholderValues placeholderValues = getPlaceholderValues(documentData, exportParams, template);
        return processPlaceholders(template, placeholderValues);
    }

    public @NotNull List<String> replacePlaceholders(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams, @NotNull List<String> templates) {
        PlaceholderValues placeholderValues = getPlaceholderValues(documentData, exportParams, templates);

        return processPlaceholders(templates, placeholderValues);
    }

    public @NotNull PlaceholderValues getPlaceholderValues(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams, @NotNull List<String> templates) {
        String revision = exportParams.getRevision() != null ? exportParams.getRevision() : documentData.getLastRevision();
        String baseLineName = documentData.getBaselineName();

        PlaceholderValues placeholderValues = PlaceholderValues.builder()
                .productName(pdfExporterPolarionService.getPolarionProductName())
                .productVersion(pdfExporterPolarionService.getPolarionVersion())
                .projectName(documentData.getProjectName())
                .revision(revision)
                .revisionAndBaseLineName(baseLineName != null ? (revision + " " + baseLineName) : revision)
                .baseLineName(baseLineName)
                .documentId(documentData.getId())
                .documentTitle(documentData.getTitle())
                .documentRevision(documentDataHelper.getDocumentStatus(exportParams.getRevision(), documentData))
                .build();
        if (documentData.getDocumentObject() instanceof IModule module) {
            placeholderValues.addCustomVariables(module, extractCustomPlaceholders(templates));
        }
        return placeholderValues;
    }

    public @NotNull PlaceholderValues getPlaceholderValues(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams, @NotNull String template) {
        return getPlaceholderValues(documentData, exportParams, List.of(template));
    }

    public @NotNull Set<String> extractCustomPlaceholders(@NotNull List<String> contents) {
        Set<String> allPlaceholders = new TreeSet<>();
        contents.forEach(section -> allPlaceholders.addAll(extractCustomPlaceholders(section)));
        return allPlaceholders;
    }

    public @NotNull Set<String> extractCustomPlaceholders(@NotNull String section) {
        Set<String> placeholders = new HashSet<>();
        RegexMatcher.get("\\{\\{\\s*(?<placeholder>\\w+)\\s*\\}\\}").processEntry(section, regexEngine -> {
            String placeholder = regexEngine.group("placeholder");
            if (!Placeholder.contains(placeholder)) {
                placeholders.add(placeholder);
            }
        });
        return placeholders;
    }

    public List<String> processPlaceholders(@NotNull List<String> templates, @NotNull PlaceholderValues placeholderValues) {
        return templates.stream()
                .map(template -> processPlaceholders(template, placeholderValues))
                .toList();
    }

    public @NotNull String processPlaceholders(@NotNull String template, @NotNull PlaceholderValues placeholderValues) {
        Map<String, String> variables = placeholderValues.getAllVariables();

        String processedText = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String regex = String.format("\\{\\{\\s*%s\\s*\\}\\}", entry.getKey());
            processedText = processedText.replaceAll(regex, entry.getValue() != null ? entry.getValue() : "");
        }

        return processedText;
    }
}
