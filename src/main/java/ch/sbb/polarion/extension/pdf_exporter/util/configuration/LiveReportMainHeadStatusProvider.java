package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.subterra.base.location.ILocation;
import org.jetbrains.annotations.NotNull;

@Discoverable
public class LiveReportMainHeadStatusProvider extends ConfigurationStatusProvider {

    public static final String LIVE_REPORT_BUTTON = "LiveReport Button";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        ILocation location = ScopeUtils.getDefaultLocation().append(".polarion/context.properties");
        String content = new SettingsService().read(location, null);

        return getConfigurationStatus(LIVE_REPORT_BUTTON, content, "scriptInjection.mainHead=<script src=\"/polarion/pdf-exporter/js/starter.js\">");
    }
}
