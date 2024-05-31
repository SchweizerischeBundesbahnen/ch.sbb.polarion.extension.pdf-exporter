package ch.sbb.polarion.extension.pdf.exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf.exporter.util.MediaUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            Pattern pattern = Pattern.compile(template + " \\((?<index>\\d+?)\\)");

            int index = 1;
            for (SettingName persistedName : persistedNames) {
                Matcher matcher = pattern.matcher(persistedName.getName());
                if (matcher.find()) {
                    try {
                        int persistedIndex = Integer.parseInt(matcher.group("index"));
                        if (persistedIndex >= index) {
                            index = persistedIndex + 1;
                        }
                    } catch (NumberFormatException ex) {
                        // Just ignore
                    }
                }
            }
            return String.format("%s (%s)", template, index);
        } else {
            return template;
        }
    }

    @SuppressWarnings("java:S5042")
    public Set<String> getPredefinedTemplates() {
        Set<String> predefinedTemplates = new TreeSet<>();

        Pattern pattern = Pattern.compile(String.format("%s/(?<template>[\\S ]+?)/template.html", TEMPLATES_JAR_PATH));

        CodeSource src = CoverPageSettings.class.getProtectionDomain().getCodeSource();
        try (ZipInputStream zip = new ZipInputStream(src.getLocation().openStream())) {
            ZipEntry ze;
            while ((ze = zip.getNextEntry()) != null) {
                Matcher matcher = pattern.matcher(ze.getName());
                if (matcher.find()) {
                    predefinedTemplates.add(matcher.group("template"));
                }
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
        Pattern pattern = Pattern.compile(String.format("%s/%s/(?<image>[\\S ]+(?:\\.jpg|\\.jpeg|\\.png))", TEMPLATES_JAR_PATH, template));

        Set<String> fileNames = new HashSet<>();
        CodeSource src = CoverPageSettings.class.getProtectionDomain().getCodeSource();
        try (ZipInputStream zip = new ZipInputStream(src.getLocation().openStream())) {
            ZipEntry ze;
            while ((ze = zip.getNextEntry()) != null) {
                Matcher matcher = pattern.matcher(ze.getName());
                if (matcher.find()) {
                    fileNames.add(matcher.group("image"));
                }
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

    @SuppressWarnings("unchecked")
    public void deleteCoverPageImages(String coverPageName, String scope) {
        String uuid = getIdByName(scope, true, coverPageName);
        ILocation coverPageFolderLocation = ScopeUtils.getContextLocation(scope).append(getSettingsFolder());
        final IRepositoryReadOnlyConnection readOnlyConnection = pdfExporterPolarionService.getReadOnlyConnection(coverPageFolderLocation);
        List<Location> subLocations = readOnlyConnection.getSubLocations(coverPageFolderLocation, false);
        subLocations.forEach(location -> {
            String locationFileName = location.getLastComponent();
            if (locationFileName.startsWith(uuid) && imageExtensions.contains(locationFileName.substring(locationFileName.lastIndexOf(".")))) {
                getSettingsService().delete(location);
            }
        });
    }

    public String processImagePlaceholders(@NotNull String css) {
        Pattern pattern = Pattern.compile("(?<placeholder>\\{\\{\\s*IMAGE:\\s*'(?<imagePath>.+)'\\s*\\}\\})");
        Matcher matcher = pattern.matcher(css);
        while (matcher.find()) {
            String imagePath = matcher.group("imagePath");
            String imageFormat = MediaUtils.getImageFormat(imagePath);
            byte[] fileContent = MediaUtils.getBinaryFileFromSvn(imagePath);
            if (fileContent != null) {
                String placeholder = matcher.group("placeholder");
                String imageInBase64 = Base64.getEncoder().encodeToString(fileContent);
                css = css.replace(placeholder, String.format("url('data:%s;base64,%s')", imageFormat, imageInBase64));
            }
        }
        return css;
    }

}
