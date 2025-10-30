package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WebhooksModelTest {

    private static Stream<Arguments> testValuesForWebhooks() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of("some badly formatted string", null),
                Arguments.of(String.format("ok file" +
                                        "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                        "12345%1$s" +
                                        "-----END BUNDLE TIMESTAMP-----%1$s" +
                                        "-----BEGIN WEBHOOKS-----%1$s" +
                                        "[{\"url\":\"https://example.com/webhook\",\"authType\":\"BEARER_TOKEN\",\"authTokenName\":\"token1\"}]%1$s" +
                                        "-----END WEBHOOKS-----%1$s",
                                System.lineSeparator()),
                        List.of(new WebhookConfig("https://example.com/webhook", AuthType.BEARER_TOKEN, "token1"))),
                Arguments.of(String.format("multiple webhooks" +
                                        "-----BEGIN WEBHOOKS-----%1$s" +
                                        "[{\"url\":\"https://example.com/webhook1\",\"authType\":\"BASIC_AUTH\",\"authTokenName\":\"token1\"}," +
                                        "{\"url\":\"https://example.com/webhook2\",\"authType\":\"BEARER_TOKEN\",\"authTokenName\":\"token2\"}]%1$s" +
                                        "-----END WEBHOOKS-----%1$s",
                                System.lineSeparator()),
                        List.of(
                                new WebhookConfig("https://example.com/webhook1", AuthType.BASIC_AUTH, "token1"),
                                new WebhookConfig("https://example.com/webhook2", AuthType.BEARER_TOKEN, "token2")
                        )),
                Arguments.of(String.format("empty array" +
                                "-----BEGIN WEBHOOKS-----%1$s" +
                                "[]%1$s" +
                                "-----END WEBHOOKS-----%1$s",
                        System.lineSeparator()), Collections.emptyList()),
                Arguments.of(String.format("keep first duplicated entry" +
                                        "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                        "ts1%1$s" +
                                        "-----END BUNDLE TIMESTAMP-----%1$s" +
                                        "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                        "ts2%1$s" +
                                        "-----END BUNDLE TIMESTAMP-----%1$s" +
                                        "-----BEGIN WEBHOOKS-----%1$s" +
                                        "[{\"url\":\"https://example.com/webhook1\",\"authType\":\"BEARER_TOKEN\",\"authTokenName\":\"token1\"}]%1$s" +
                                        "-----END WEBHOOKS-----%1$s" +
                                        "-----BEGIN WEBHOOKS-----%1$s" +
                                        "[{\"url\":\"https://example.com/webhook2\",\"authType\":\"BASIC_AUTH\",\"authTokenName\":\"token2\"}]%1$s" +
                                        "-----END WEBHOOKS-----%1$s",
                                System.lineSeparator()),
                        List.of(new WebhookConfig("https://example.com/webhook1", AuthType.BEARER_TOKEN, "token1")))
        );
    }

    @ParameterizedTest
    @MethodSource("testValuesForWebhooks")
    void getProperExpectedResults(String locationContent, List<WebhookConfig> expectedWebhooks) {

        final WebhooksModel model = new WebhooksModel();
        model.deserialize(locationContent);

        if (expectedWebhooks == null) {
            assertNotNull(model.getWebhookConfigs());
            assertTrue(model.getWebhookConfigs().isEmpty());
        } else {
            assertEquals(expectedWebhooks, model.getWebhookConfigs());
        }
    }
}
