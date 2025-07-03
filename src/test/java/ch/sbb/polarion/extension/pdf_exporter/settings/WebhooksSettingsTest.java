package ch.sbb.polarion.extension.pdf_exporter.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebhooksSettingsTest {

    @Test
    void testDefaultValues() {
        WebhooksSettings settings = mock(WebhooksSettings.class);
        when(settings.defaultValues()).thenCallRealMethod();
        assertTrue(settings.defaultValues().getWebhookConfigs().isEmpty(), "Default webhook configs should be empty");
    }

}
