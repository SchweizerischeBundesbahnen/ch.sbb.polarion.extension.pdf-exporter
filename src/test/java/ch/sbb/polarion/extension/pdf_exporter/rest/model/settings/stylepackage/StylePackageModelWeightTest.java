package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StylePackageModelWeightTest {

    @Test
    void storesTheWeight() {
        StylePackageModel model = StylePackageModel.builder().weight(25f).build();

        StylePackageModel read = new StylePackageModel();
        read.deserialize(model.serialize());

        assertEquals(25f, read.getWeight());
    }

    @Test
    void readsAWeightWhichIsNoNumberAsTheDefault() {
        // A style package edited by hand stays readable
        StylePackageModel model = StylePackageModel.builder().weight(25f).build();

        StylePackageModel read = new StylePackageModel();
        read.deserialize(model.serialize().replaceAll("(-----BEGIN WEIGHT-----\\R)[\\s\\S]*?(\\R-----END WEIGHT-----)", "$1heavy$2"));

        assertEquals(StylePackageModel.DEFAULT_WEIGHT, read.getWeight());
    }
}
