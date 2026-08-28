package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageVisibilityModel;
import com.polarion.subterra.base.location.ILocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which style packages a scope offers: one always-present "Default" document per scope, the global one
 * being what every project uses unless it saves its own. Kept apart from the style packages themselves
 * because it applies to all of them at once; the Style Packages administration page edits it.
 */
@SuppressWarnings("java:S2160") // the added field is a cache, not state: equality stays the feature name of the parent
public class StylePackageVisibilitySettings extends GenericNamedSettings<StylePackageVisibilityModel> {
    public static final String FEATURE_NAME = "style-package-visibility";

    /**
     * The flag is read on the export path - once per {@link #readNames(String)} of the style packages,
     * which itself runs several times per exported document - so its value is kept per scope until either
     * the project's or the global settings folder gets a new revision.
     */
    private final Map<String, CachedFlag> hiddenGlobalStylePackages = new ConcurrentHashMap<>();

    public StylePackageVisibilitySettings() {
        super(FEATURE_NAME);
    }

    public StylePackageVisibilitySettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    /**
     * These settings as registered for this extension, or null when nothing is registered. Nothing being
     * registered means nothing can be configured either, so every caller reads that as "no scope hides
     * anything"; in a running Polarion the bundle activator always registers them.
     */
    @SuppressWarnings("rawtypes")
    public static @Nullable StylePackageVisibilitySettings registered() {
        for (GenericNamedSettings settings : NamedSettingsRegistry.INSTANCE.getAll()) {
            if (settings instanceof StylePackageVisibilitySettings visibility) {
                return visibility;
            }
        }
        return null;
    }

    @Override
    public @NotNull StylePackageVisibilityModel defaultValues() {
        // Global style packages stay visible in projects: this is what installations did before the flag existed.
        return StylePackageVisibilityModel.builder()
                .hideGlobalStylePackages(false)
                .build();
    }

    /**
     * Tells whether the given scope must ignore the style packages defined on the global level. The global
     * scope itself never hides anything.
     */
    public boolean isGlobalStylePackagesHidden(@NotNull String scope) {
        if (DEFAULT_SCOPE.equals(scope)) {
            return false;
        }

        String projectRevision = lastRevision(scope);
        String globalRevision = lastRevision(DEFAULT_SCOPE);
        if (projectRevision == null && globalRevision == null) {
            return false; // nothing configured anywhere, so nothing to read
        }

        String revisions = projectRevision + "@" + globalRevision;
        CachedFlag cached = hiddenGlobalStylePackages.get(scope);
        if (cached != null && cached.revisions().equals(revisions)) {
            return cached.hidden();
        }

        boolean hidden = read(scope, SettingId.fromName(DEFAULT_NAME), null).isHideGlobalStylePackages();
        hiddenGlobalStylePackages.put(scope, new CachedFlag(revisions, hidden));
        return hidden;
    }

    /**
     * Persists the settings and forgets what was cached for the scope, so the style packages answer to the
     * new value at once rather than after the next revision of the settings folder.
     */
    @Override
    public @NotNull StylePackageVisibilityModel save(@NotNull String scope, @NotNull SettingId id, @NotNull StylePackageVisibilityModel what) {
        StylePackageVisibilityModel saved = super.save(scope, id, what);
        if (DEFAULT_SCOPE.equals(scope)) {
            // The global document is the default of every project that stores none, so all of them have to
            // read it again - their cache key holds the global folder revision, which a content-only change
            // of that document does not move.
            hiddenGlobalStylePackages.clear();
        } else {
            hiddenGlobalStylePackages.remove(scope);
        }
        return saved;
    }

    private @Nullable String lastRevision(@NotNull String scope) {
        ILocation location = ScopeUtils.getContextLocation(scope).append(getSettingsFolder());
        return getSettingsService().getLastRevision(location);
    }

    /** The flag as read for a scope, together with the folder revisions it was read at. */
    private record CachedFlag(@NotNull String revisions, boolean hidden) {
    }
}
