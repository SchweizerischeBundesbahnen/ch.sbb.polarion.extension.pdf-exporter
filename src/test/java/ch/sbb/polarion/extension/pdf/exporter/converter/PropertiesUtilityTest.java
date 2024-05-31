package ch.sbb.polarion.extension.pdf.exporter.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesUtilityTest {
    @Test
    void shouldLoadCorrectProperties() {
        PropertiesUtility propertiesUtility = new PropertiesUtility();
        assertThat(propertiesUtility.getFinishedJobTimeout()).isEqualTo(30);
        assertThat(propertiesUtility.getInProgressJobTimeout()).isEqualTo(60);
    }
}