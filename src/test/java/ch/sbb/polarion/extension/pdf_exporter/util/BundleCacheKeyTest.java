package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.rest.model.Version;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

class BundleCacheKeyTest {

    private static String keyFor(Version version) {
        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(VersionUtils::getVersion).thenReturn(version);
            return BundleCacheKey.get();
        }
    }

    @Test
    void changesWithEveryBuildOfOneVersion() {
        // Two builds of one SNAPSHOT, or a rebuilt release: the version alone would give both the same URL
        String first = keyFor(Version.builder().bundleVersion("13.7.1").bundleBuildTimestamp("2026-09-22 14:23").build());
        String second = keyFor(Version.builder().bundleVersion("13.7.1").bundleBuildTimestamp("2026-09-22 16:05").build());

        assertEquals("13.7.1-202609221423", first);
        assertEquals("13.7.1-202609221605", second);
    }

    @Test
    void isTheVersionAloneWhereTheManifestCarriesNoBuildTimestamp() {
        assertEquals("13.7.1", keyFor(Version.builder().bundleVersion("13.7.1").build()));
    }

    @Test
    void fallsBackToZeroWhereTheManifestCarriesNoVersion() {
        // A unit test, or a deployment that lost its metadata. The key must still be something: left out, the
        // placeholder would be requested literally
        assertEquals("0", keyFor(Version.builder().build()));
        assertEquals("0-202609221423", keyFor(Version.builder().bundleBuildTimestamp("2026-09-22 14:23").build()));
    }
}
