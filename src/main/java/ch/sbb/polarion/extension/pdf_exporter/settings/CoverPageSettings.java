package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.subterra.base.location.ILocation;
import org.jetbrains.annotations.NotNull;

import jakarta.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("java:S2160") //Override the "equals" not needed here
public class CoverPageSettings extends GenericNamedSettings<CoverPageModel> {
    private static final Logger logger = Logger.getLogger(CoverPageSettings.class);
    public static final String FEATURE_NAME = "cover-page";
    public static final String DEFAULT_TEMPLATE = "English";
    public static final String TEMPLATES_JAR_PATH = "default/cover-page";
    public static final String SETTINGS_SVN_LOCATION = ".polarion/extensions/pdf-exporter/cover-page";
    public static final String TEMPLATE_IMAGE_PLACEHOLDER = "templateImage('%s')";
    private final Collection<String> imageExtensions = Arrays.asList(".png", ".jpg", ".jpeg");

    private final PdfExporterPolarionService pdfExporterPolarionService;

    public CoverPageSettings() {
        super(FEATURE_NAME);
        pdfExporterPolarionService = new PdfExporterPolarionService();
    }

    public CoverPageSettings(SettingsService settingsService, PdfExporterPolarionService pdfExporterPolarionService) {
        super(FEATURE_NAME, settingsService);
        this.pdfExporterPolarionService = pdfExporterPolarionService != null ? pdfExporterPolarionService : new PdfExporterPolarionService();
    }

    @Override
    public @NotNull CoverPageModel defaultValues() {
        return defaultValuesFor(DEFAULT_TEMPLATE);
    }

    public @NotNull CoverPageModel defaultValuesFor(@NotNull String template) {
        return CoverPageModel.builder()
                .templateHtml(ScopeUtils.getFileContent(String.format("%s/%s/template.html", TEMPLATES_JAR_PATH, template)))
                .templateCss(ScopeUtils.getFileContent(String.format("%s/%s/template.css", TEMPLATES_JAR_PATH, template)))
                .build();
    }

    public String getNonClashingName(@NotNull String template, @NotNull Collection<SettingName> persistedNames) {
        boolean namesClashing = persistedNames.stream().map(SettingName::getName).anyMatch(name -> name.equals(template));
        if (namesClashing) {
            RegexMatcher matcher = RegexMatcher.get(RegexMatcher.quote(template) + " \\((?<index>\\d+?)\\)");

            AtomicInteger index = new AtomicInteger(1);
            for (SettingName persistedName : persistedNames) {
                matcher.processEntry(persistedName.getName(), regexEngine -> {
                    try {
                        int persistedIndex = Integer.parseInt(regexEngine.group("index"));
                        if (persistedIndex >= index.get()) {
                            index.set(persistedIndex + 1);
                        }
                    } catch (NumberFormatException ex) {
                        // Just ignore
                    }
                });
            }
            return String.format("%s (%s)", template, index);
        } else {
            return template;
        }
    }

    @SuppressWarnings("java:S5042")
    public Set<String> getPredefinedTemplates() {
        Set<String> predefinedTemplates = new TreeSet<>();

        RegexMatcher matcher = RegexMatcher.get(String.format("%s/(?<template>[\\S ]+?)/template.html", TEMPLATES_JAR_PATH));

        CodeSource src = CoverPageSettings.class.getProtectionDomain().getCodeSource();
        try (ZipInputStream zip = new ZipInputStream(src.getLocation().openStream())) {
            ZipEntry ze;
            while ((ze = zip.getNextEntry()) != null) {
                matcher.processEntry(ze.getName(), regexEngine -> predefinedTemplates.add(regexEngine.group("template")));
            }
        } catch (IOException ex) {
            throw new InternalServerErrorException(ex);
        }
        return predefinedTemplates;
    }

    public void processImagePaths(CoverPageModel model, String template, String scope, UUID coverPageUuid) {
        Set<String> imageFileNames = getTemplateImageFileNames(template);

        for (String imageFileName : imageFileNames) {
            String imagePlaceholder = String.format(TEMPLATE_IMAGE_PLACEHOLDER, imageFileName);
            if (model.getTemplateCss().contains(imagePlaceholder)) {
                String persistedPath = persistTemplateImage(template, scope, imageFileName, coverPageUuid);
                if (persistedPath != null) {
                    model.setTemplateCss(model.getTemplateCss().replace(imagePlaceholder, String.format("{{ IMAGE: '%s'}}", persistedPath)));
                }
            }
        }
    }

    @SuppressWarnings("java:S5042")
    public Set<String> getTemplateImageFileNames(String template) {
        RegexMatcher matcher = RegexMatcher.get(String.format("%s/%s/(?<image>[\\S ]+(?:\\.jpg|\\.jpeg|\\.png))", RegexMatcher.quote(TEMPLATES_JAR_PATH), RegexMatcher.quote(template)));

        Set<String> fileNames = new HashSet<>();
        CodeSource src = CoverPageSettings.class.getProtectionDomain().getCodeSource();
        try (ZipInputStream zip = new ZipInputStream(src.getLocation().openStream())) {
            ZipEntry ze;
            while ((ze = zip.getNextEntry()) != null) {
                matcher.processEntry(ze.getName(), regexEngine -> fileNames.add(regexEngine.group("image")));
            }
        } catch (IOException ex) {
            throw new InternalServerErrorException(ex);
        }
        return fileNames;
    }

    public String persistTemplateImage(String template, String scope, String imageFileName, UUID coverPageUuid) {
        String jarFilePath = String.format("/%s/%s/%s", TEMPLATES_JAR_PATH, template, imageFileName);
        byte[] fileContent = MediaUtils.getBinaryFileFromJar(jarFilePath);
        if (fileContent != null) {
            ILocation location = ScopeUtils.getContextLocation(scope).append(String.format("%s/%s_%s", SETTINGS_SVN_LOCATION, coverPageUuid, imageFileName));
            getSettingsService().save(location, fileContent);
            return location.getLocationPath();
        } else {
            logger.error("Error reading template image content from: " + jarFilePath);
            return null;
        }
    }

    /**
     * Deletes a cover page together with the images that belong to it.
     * <p>
     * The images are separate files in SVN named after the setting's UUID, and there is no atomic way
     * to remove both: two repository operations, either of which can fail. What can be chosen is which
     * failure is possible. The UUID is resolved first, so it is in hand regardless of what happens to
     * the setting, and the setting goes first:
     * <ul>
     *   <li>if deleting the setting fails, nothing has happened yet - the cover page is still whole;</li>
     *   <li>if deleting an image fails afterwards, the cover page is gone and some of its files linger
     *       in the repository, unreferenced. Waste, not a broken configuration.</li>
     * </ul>
     * The other order - images first, as the administration page used to do with two requests - has
     * the failure that matters: a cover page that still exists, still selectable, and no longer
     * renders what it was configured to render.
     */
    @Override
    public void delete(@NotNull String scope, @NotNull SettingId id) {
        String uuid = id.isUseName() ? getIdByName(scope, true, id.getIdentifier()) : id.getIdentifier();
        // Throws if the setting does not exist, which is also what guarantees the id resolved: past
        // this line there is a cover page that was just deleted, so it had a UUID to be found by.
        super.delete(scope, id);
        try {
            deleteImages(scope, Objects.requireNonNull(uuid));
        } catch (RuntimeException e) {
            // The cover page itself is gone, which is what was asked for; say what was left behind
            // rather than failing a deletion that already happened.
            logger.error("Cover page '%s' was deleted, but its images could not be removed from %s"
                    .formatted(id.getIdentifier(), scope), e);
        }
    }

    public void deleteCoverPageImages(String coverPageName, String scope) {
        String uuid = getIdByName(scope, true, coverPageName);
        if (uuid != null) {
            deleteImages(scope, uuid);
        }
    }

    /** Removes the image files stored next to the setting, which are the ones prefixed with its UUID. */
    private void deleteImages(String scope, @NotNull String uuid) {
        ILocation coverPageFolderLocation = ScopeUtils.getContextLocation(scope).append(getSettingsFolder());
        final IRepositoryReadOnlyConnection readOnlyConnection = pdfExporterPolarionService.getReadOnlyConnection(coverPageFolderLocation);
        List<ILocation> subLocations = readOnlyConnection.getSubLocations(coverPageFolderLocation, false);
        subLocations.forEach(location -> {
            String locationFileName = location.getLastComponent();
            if (locationFileName.startsWith(uuid) && imageExtensions.contains(locationFileName.substring(locationFileName.lastIndexOf(".")))) {
                getSettingsService().delete(location);
            }
        });
    }

    public String processImagePlaceholders(@NotNull String css) {

        Map<String, String> placeholders = new HashMap<>();

        RegexMatcher.get("(?<placeholder>\\{\\{\\s*IMAGE:\\s*'(?<imagePath>.+)'\\s*\\}\\})")
                .processEntry(css, regexEngine -> placeholders.put(regexEngine.group("placeholder"), regexEngine.group("imagePath")));

        for (Map.Entry<String, String> placeholderEntry : placeholders.entrySet()) {
            String imagePath = placeholderEntry.getValue();
            String imageFormat = MediaUtils.getImageFormat(imagePath);
            byte[] fileContent = MediaUtils.getBinaryFileFromSvn(imagePath);
            if (fileContent != null) {
                String placeholder = placeholderEntry.getKey();
                String imageInBase64 = Base64.getEncoder().encodeToString(fileContent);
                css = css.replace(placeholder, String.format("url('data:%s;base64,%s')", imageFormat, imageInBase64));
            }
        }
        return css;
    }

}
