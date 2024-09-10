package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.Language.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizationModelTest {

    private static Stream<Arguments> testValuesForGetLocalizationMap() {
        return Stream.of(
                Arguments.of(null, 0, 0, 0, null, Arrays.asList(
                        new ExpectedLocalization(DE, null, null),
                        new ExpectedLocalization(FR, null, null),
                        new ExpectedLocalization(IT, null, null)
                )),
                Arguments.of("", 0, 0, 0, null, Arrays.asList(
                        new ExpectedLocalization(DE, null, null),
                        new ExpectedLocalization(FR, null, null),
                        new ExpectedLocalization(IT, null, null)
                )),
                Arguments.of("some badly formatted string", 0, 0, 0, null, Arrays.asList(
                        new ExpectedLocalization(DE, null, null),
                        new ExpectedLocalization(FR, null, null),
                        new ExpectedLocalization(IT, null, null)
                )),
                Arguments.of(String.format("ok file" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "12345%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN DE LOCALIZATION-----" +
                                "<?xml version=\"1.0\"?>%1$s" +
                                "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"de\">%1$s" +
                                "    <file id=\"1\" original=\"de.xlf\">%1$s" +
                                "        <unit id=\"1\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Must Have</source>%1$s" +
                                "                <target>Haben müssen</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"2\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Should Have</source>%1$s" +
                                "                <target>Sollte haben</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"3\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Nice to Have</source>%1$s" +
                                "                <target>Schön zu haben</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"4\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Will not Have</source>%1$s" +
                                "                <target>Werde nicht haben</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"5\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Open</source>%1$s" +
                                "                <target>Offen</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"6\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>In Progress</source>%1$s" +
                                "                <target>Im Gange</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"7\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Reopened</source>%1$s" +
                                "                <target>Wiedereröffnet</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"8\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Done</source>%1$s" +
                                "                <target>Erledigt</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"9\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Verified</source>%1$s" +
                                "                <target>Verifiziert</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"10\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Rejected</source>%1$s" +
                                "                <target>Abgelehnt</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "    </file>%1$s" +
                                "</xliff>%1$s" +
                                "-----END DE LOCALIZATION-----%1$s" +
                                "any accident garbage%1$s" +
                                "-----BEGIN IT LOCALIZATION-----%1$s" +
                                "<?xml version=\"1.0\"?>%1$s" +
                                "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\">%1$s" +
                                "    <file id=\"1\" original=\"fr.xlf\">%1$s" +
                                "        <unit id=\"1\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Verified</source>%1$s" +
                                "                <target>Verificato</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "    </file>%1$s" +
                                "</xliff>%1$s" +
                                "-----END IT LOCALIZATION-----%1$s" +
                                "-----BEGIN FR LOCALIZATION-----%1$s" +
                                "<?xml version=\"1.0\"?>%1$s" +
                                "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"it\">%1$s" +
                                "    <file id=\"1\" original=\"it.xlf\">%1$s" +
                                "        <unit id=\"1\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Must Have</source>%1$s" +
                                "                <target>Doit avoir</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"2\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Should Have</source>%1$s" +
                                "                <target>Avoir dû</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"3\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Nice to Have</source>%1$s" +
                                "                <target>Agréable d'avoir</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"4\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Will not Have</source>%1$s" +
                                "                <target>N'aura pas</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"5\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Open</source>%1$s" +
                                "                <target>Ouvrir</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"6\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>In Progress</source>%1$s" +
                                "                <target>En cours</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"7\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Reopened</source>%1$s" +
                                "                <target>Rouvert</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"8\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Done</source>%1$s" +
                                "                <target>Fait</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"9\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Verified</source>%1$s" +
                                "                <target>Vérifié</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "        <unit id=\"10\">%1$s" +
                                "            <segment>%1$s" +
                                "                <source>Rejected</source>%1$s" +
                                "                <target>Rejeté</target>%1$s" +
                                "            </segment>%1$s" +
                                "        </unit>%1$s" +
                                "    </file>%1$s" +
                                "</xliff>%1$s" +
                                "-----END FR LOCALIZATION-----%1$s",
                        System.lineSeparator()), 10, 10, 1, "12345", Arrays.asList(
                        new ExpectedLocalization(DE, "Sollte haben", "Verifiziert"),
                        new ExpectedLocalization(FR, "Avoir dû", "Vérifié"),
                        new ExpectedLocalization(IT, null, "Verificato"))),
                Arguments.of(String.format("corrupted example 1" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN DE LOCALIZATION-----" +
                                "Should Have = Sollte haben%1$s" +
                                "-----END FR LOCALIZATION-----%1$s",
                        System.lineSeparator()), 0, 0, 0, "", null),
                Arguments.of(String.format("corrupted example 2" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN DE LOCALIZATION-----" +
                                "Should Have = Sollte haben%1$s",
                        System.lineSeparator()), 0, 0, 0, "", null)
        );
    }

    @ParameterizedTest
    @MethodSource("testValuesForGetLocalizationMap")
    void getProperExpectedResults(String locationContent, int deSize, int frSize, int itSize, String expectedBundleTimestamp, List<ExpectedLocalization> expectedLocalizations) {
        final LocalizationModel localizationModel = new LocalizationModel();
        localizationModel.deserialize(locationContent);

        assertEquals(deSize, localizationModel.getLocalizationMap(DE.name()).size());
        assertEquals(frSize, localizationModel.getLocalizationMap(FR.name()).size());
        assertEquals(itSize, localizationModel.getLocalizationMap(IT.name()).size());

        assertEquals(expectedBundleTimestamp, localizationModel.getBundleTimestamp());

        if (expectedLocalizations != null) {
            for (ExpectedLocalization expectedLocalization : expectedLocalizations) {
                Map<String, String> localizationMap = localizationModel.getLocalizationMap(expectedLocalization.getLanguage().name());
                assertEquals(expectedLocalization.getShouldHaveLocalization(), localizationMap.get("Should Have"));
                assertEquals(expectedLocalization.getVerifiedLocalization(), localizationMap.get("Verified"));
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class ExpectedLocalization {
        private Language language;
        private String shouldHaveLocalization;
        private String verifiedLocalization;
    }
}