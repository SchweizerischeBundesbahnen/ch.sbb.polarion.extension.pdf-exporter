package ch.sbb.polarion.extension.pdf_exporter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhooksStatus {
    private Boolean enabled;
}
