package ch.sbb.polarion.extension.pdf_exporter.rest.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WidthValidationResultTest {

    @Test
    void startsOutWithNothingFound() {
        // Lists, not null: PdfWidthValidationService adds to what the result already carries, and
        // @Builder ignored the initializers until they were marked @Builder.Default
        WidthValidationResult built = WidthValidationResult.builder().build();
        assertEquals(List.of(), built.getInvalidPages());
        assertEquals(List.of(), built.getSuspiciousWorkItems());

        WidthValidationResult created = new WidthValidationResult();
        assertEquals(List.of(), created.getInvalidPages());
        assertEquals(List.of(), created.getSuspiciousWorkItems());
    }
}
