package ch.sbb.polarion.extension.pdf_exporter.util.placeholder;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters.LiveDocAdapter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.Placeholder;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.tracker.model.IModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PlaceholderProcessor {

    private final PdfExporterPolarionService pdfExporterPolarionService;

    public PlaceholderProcessor() {
        this.pdfExporterPolarionService = new PdfExporterPolarionService();
    }

    public PlaceholderProcessor(PdfExporterPolarionService pdfExporterPolarionService) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
    }

    public @NotNull String replacePlaceholders(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams, @NotNull String template) {
        return replacePlaceholders(documentData, exportParams, template, null);
    }

    public @NotNull String replacePlaceholders(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams, @NotNull String template, @Nullable PlaceholderValues overridenPlaceholderValues) {
        PlaceholderValues placeholderValues = getPlaceholderValues(documentData, exportParams, template);
        return processPlaceholders(template, placeholderValues, overridenPlaceholderValues);
    }

    public @NotNull List<String> replacePlaceholders(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams, @NotNull List<String> templates) {
        PlaceholderValues placeholderValues = getPlaceholderValues(documentData, exportParams, templates);

        return processPlaceholders(templates, placeholderValues);
    }

    public @NotNull PlaceholderValues getPlaceholderValues(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams, @NotNull List<String> templates) {
        String revision = exportParams.getRevision() != null ? exportParams.getRevision() : documentData.getLastRevision();
        String baselineName = documentData.getBaseline() != null ? documentData.getBaseline().asPlaceholder() : "";
        String documentFilter = exportParams.getUrlQueryParameters() != null ? exportParams.getUrlQueryParameters().get(LiveDocAdapter.URL_QUERY_PARAM_QUERY) : null;

        PlaceholderValues placeholderValues = PlaceholderValues.builder()
                .productName(pdfExporterPolarionService.getPolarionProductName())
                .productVersion(pdfExporterPolarionService.getPolarionVersion())
                .projectName(documentData.getId().getDocumentProject() != null ? documentData.getId().getDocumentProject().getName() : "")
                .revision(revision)
                .revisionAndBaseLineName(baselineName.isEmpty() ? revision : (revision + " " + baselineName))
                .baseLineName(baselineName)
                .documentId(documentData.getId().getDocumentId())
                .documentTitle(documentData.getTitle())
                .documentRevision(documentData.getRevisionPlaceholder())
                .documentFilter(documentFilter != null ? documentFilter : "")
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
        return processPlaceholders(template, placeholderValues, null);
    }

    public @NotNull String processPlaceholders(@NotNull String template, @NotNull PlaceholderValues placeholderValues, @Nullable PlaceholderValues overridenPlaceholderValues) {
        Map<String, String> variables = placeholderValues.getAllVariables();
        if (overridenPlaceholderValues != null) {
            variables.putAll(overridenPlaceholderValues.getDefinedVariables());
        }

        String processedText = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String regex = String.format("\\{\\{\\s*%s\\s*\\}\\}", entry.getKey());
            processedText = processedText.replaceAll(regex, entry.getValue() != null ? entry.getValue() : "");
        }

        return processedText;
    }
}
