package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StylePackageModelFirstPageTest {

    @Test
    void storesTheHeaderAndFooterOfTheFirstPage() {
        StylePackageModel model = StylePackageModel.builder().headerFooter("Running").firstPageHeaderFooter("Title page").build();

        StylePackageModel read = new StylePackageModel();
        read.deserialize(model.serialize());

        assertEquals("Running", read.getHeaderFooter());
        assertEquals("Title page", read.getFirstPageHeaderFooter());
    }

    @Test
    void readsAStylePackageStoredWithoutItAsHavingNone() {
        StylePackageModel model = StylePackageModel.builder().headerFooter("Running").build();

        StylePackageModel read = new StylePackageModel();
        read.deserialize(model.serialize().replaceAll("-----BEGIN FIRST PAGE HEADER FOOTER-----[\\s\\S]*?-----END FIRST PAGE HEADER FOOTER-----\\R?", ""));

        assertEquals("Running", read.getHeaderFooter());
        assertNull(read.getFirstPageHeaderFooter());
    }
}
