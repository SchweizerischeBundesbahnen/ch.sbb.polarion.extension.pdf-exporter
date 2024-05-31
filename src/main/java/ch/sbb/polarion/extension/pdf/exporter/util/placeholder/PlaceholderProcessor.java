package ch.sbb.polarion.extension.pdf.exporter.util.placeholder;

import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.headerfooter.Placeholder;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf.exporter.util.LiveDocHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderProcessor {

    private final PdfExporterPolarionService pdfExporterPolarionService;
    private final LiveDocHelper liveDocHelper;

    public PlaceholderProcessor() {
        this.pdfExporterPolarionService = new PdfExporterPolarionService();
        this.liveDocHelper = new LiveDocHelper(pdfExporterPolarionService);
    }

    public PlaceholderProcessor(PdfExporterPolarionService pdfExporterPolarionService, LiveDocHelper liveDocHelper) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
        this.liveDocHelper = liveDocHelper;
    }

    public String replacePlaceholders(LiveDocHelper.DocumentData documentData, ExportParams exportParams, String template) {
        PlaceholderValues placeholderValues = getPlaceholderValues(documentData, exportParams, template);
        return processPlaceholders(template, placeholderValues);
    }

    public List<String> replacePlaceholders(LiveDocHelper.DocumentData documentData, ExportParams exportParams, List<String> templates) {
        PlaceholderValues placeholderValues = getPlaceholderValues(documentData, exportParams, templates);

        return processPlaceholders(templates, placeholderValues);
    }

    public PlaceholderValues getPlaceholderValues(LiveDocHelper.DocumentData documentData, ExportParams exportParams, List<String> templates) {
        String revision = exportParams.getRevision() != null ? exportParams.getRevision() : documentData.getLastRevision();
        String baseLineName = documentData.getBaselineName();

        PlaceholderValues placeholderValues = PlaceholderValues.builder()
                .productName(pdfExporterPolarionService.getPolarionProductName())
                .productVersion(pdfExporterPolarionService.getPolarionVersion())
                .projectName(documentData.getProjectName())
                .revision(revision)
                .revisionAndBaseLineName(baseLineName != null ? (revision + " " + baseLineName) : revision)
                .baseLineName(baseLineName)
                .documentId(documentData.getDocumentId())
                .documentTitle(documentData.getDocumentTitle())
                .documentRevision(liveDocHelper.getDocumentStatus(exportParams.getRevision(), documentData))
                .build();
        if (documentData.getDocument() != null) {
            placeholderValues.addCustomVariables(documentData.getDocument(), extractCustomPlaceholders(templates));
        }
        return placeholderValues;
    }

    public PlaceholderValues getPlaceholderValues(LiveDocHelper.DocumentData documentData, ExportParams exportParams, String template) {
        return getPlaceholderValues(documentData, exportParams, List.of(template));
    }

    public Set<String> extractCustomPlaceholders(List<String> contents) {
        Set<String> allPlaceholders = new TreeSet<>();
        contents.forEach(section -> allPlaceholders.addAll(extractCustomPlaceholders(section)));
        return allPlaceholders;
    }

    public Set<String> extractCustomPlaceholders(String section) {
        Set<String> placeholders = new HashSet<>();
        Pattern pattern = Pattern.compile("\\{\\{\\s*(?<placeholder>\\w+)\\s*\\}\\}");
        Matcher matcher = pattern.matcher(section);
        while (matcher.find()) {
            String placeholder = matcher.group("placeholder");
            if (!Placeholder.contains(placeholder)) {
                placeholders.add(placeholder);
            }
        }
        return placeholders;
    }

    public List<String> processPlaceholders(@NotNull List<String> templates, @NotNull PlaceholderValues placeholderValues) {
        return templates.stream()
                .map(template -> processPlaceholders(template, placeholderValues))
                .toList();
    }

    public String processPlaceholders(@NotNull String template, @NotNull PlaceholderValues placeholderValues) {
        Map<String, String> variables = placeholderValues.getAllVariables();

        String processedText = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String regex = String.format("\\{\\{\\s*%s\\s*\\}\\}", entry.getKey());
            processedText = processedText.replaceAll(regex, entry.getValue() != null ? entry.getValue() : "");
        }

        return processedText;
    }
}
