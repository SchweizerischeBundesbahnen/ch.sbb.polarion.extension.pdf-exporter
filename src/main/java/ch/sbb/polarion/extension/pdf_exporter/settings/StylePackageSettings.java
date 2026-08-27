package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ImageDensity;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import com.polarion.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class StylePackageSettings extends GenericNamedSettings<StylePackageModel> {
    public static final String FEATURE_NAME = "style-package";
    public static final String DEFAULT_HEADERS_COLOR = "#000028";

    /** Where the "hide the global style packages" flag is read from; resolved on first use. */
    private StylePackageVisibilitySettings stylePackageVisibilitySettings;

    public StylePackageSettings() {
        super(FEATURE_NAME);
    }

    public StylePackageSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @VisibleForTesting
    public StylePackageSettings(SettingsService settingsService, @NotNull StylePackageVisibilitySettings stylePackageVisibilitySettings) {
        super(FEATURE_NAME, settingsService);
        this.stylePackageVisibilitySettings = stylePackageVisibilitySettings;
    }

    @Override
    public void beforeSave(@NotNull StylePackageModel what) {
        adjustAndValidateWeight(what);
        validateMatchingQuery(what);
    }

    /**
     * The names of the style packages a scope may use. A project which hides the global level keeps only its
     * own packages, plus "Default" - that name may never be missing from the list.
     */
    @Override
    public Collection<SettingName> readNames(@NotNull String scope) {
        Collection<SettingName> names = super.readNames(scope);
        if (!globalPackagesHidden(scope)) {
            return names;
        }

        List<SettingName> visible = new ArrayList<>(names.stream().filter(name -> Objects.equals(name.getScope(), scope)).toList());
        if (visible.stream().noneMatch(name -> DEFAULT_NAME.equals(name.getName()))) {
            // The project has no "Default" of its own (it was deleted, or the flag comes from the global
            // scope): the synthetic entry keeps the name resolvable, read() answers it with the built-in values.
            visible.add(0, SettingName.builder().id(DEFAULT_NAME).name(DEFAULT_NAME).scope(DEFAULT_SCOPE).build());
        }
        return visible;
    }

    /**
     * Reads a style package. For a scope which hides the global level the inherited documents are out of
     * reach: a name that is not persisted in the scope itself is unknown, "Default" alone falling back to
     * the built-in values.
     */
    @Override
    public @NotNull StylePackageModel read(@NotNull String scope, @NotNull SettingId id, @Nullable String revisionName) {
        if (id.isUseName() && globalPackagesHidden(scope) && getIdByName(scope, true, id.getIdentifier()) == null) {
            return handleMissingValue(id);
        }
        return super.read(scope, id, revisionName);
    }

    private boolean globalPackagesHidden(@NotNull String scope) {
        StylePackageVisibilitySettings settings = stylePackageVisibilitySettings != null ? stylePackageVisibilitySettings : StylePackageVisibilitySettings.registered();
        return settings != null && settings.isGlobalStylePackagesHidden(scope);
    }

    /**
     * Loads a style package by name. In OSGi, other bundles may use a different SettingId class
     * instance due to classloader boundaries, so using the name avoids ClassCastException.
     */
    public @NotNull StylePackageModel loadByName(@Nullable String projectId, @NotNull String name) {
        return load(projectId, SettingId.fromName(name));
    }

    @Override
    public @NotNull StylePackageModel defaultValues() {
        return StylePackageModel.builder()
                .coverPage(DEFAULT_NAME)
                .headerFooter(DEFAULT_NAME)
                .css(DEFAULT_NAME)
                .localization(DEFAULT_NAME)
                .webhooks(DEFAULT_NAME)
                .headersColor(DEFAULT_HEADERS_COLOR)
                .paperSize(PaperSize.A4.name())
                .orientation(Orientation.PORTRAIT.name())
                .pdfVariant(PdfVariant.PDF_A_2B.name())
                .imageDensity(ImageDensity.DPI_96.name())
                .fitToPage(true)
                .renderComments(CommentsRenderType.OPEN)
                .cutEmptyWorkitemAttributes(true)
                .followHTMLPresentationalHints(true)
                .weight(StylePackageModel.DEFAULT_WEIGHT)
                .build();
    }

    @VisibleForTesting
    void adjustAndValidateWeight(StylePackageModel model) {
        Float weight = model.getWeight();
        if (weight == null) {
            model.setWeight(DEFAULT_NAME.equals(model.getName()) ? StylePackageModel.DEFAULT_INITIAL_WEIGHT : StylePackageModel.DEFAULT_WEIGHT);
        } else if (weight < 0 || weight > 100 || Math.abs(weight * 10 - Math.floor(weight * 10)) > 0) {
            throw new IllegalArgumentException("Weight must be between 0 and 100 and have only one digit after the decimal point");
        }
    }

    @VisibleForTesting
    void validateMatchingQuery(StylePackageModel model) {
        if (DEFAULT_NAME.equals(model.getName()) && !StringUtils.isEmpty(model.getMatchingQuery())) {
            throw new IllegalArgumentException("Matching query cannot be specified for a default style package");
        }
    }
}
