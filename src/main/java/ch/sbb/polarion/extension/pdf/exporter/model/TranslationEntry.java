package ch.sbb.polarion.extension.pdf.exporter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationEntry {
    private String language;
    private String value;
}
