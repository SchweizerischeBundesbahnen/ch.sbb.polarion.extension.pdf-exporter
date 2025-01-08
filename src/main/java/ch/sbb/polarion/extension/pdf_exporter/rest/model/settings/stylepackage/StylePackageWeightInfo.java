package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylePackageWeightInfo {

    @Schema(description = "The name of the setting")
    private String name;

    @Schema(description = "The scope of the setting")
    private String scope;

    @Schema(description = "The weight of the setting")
    private Float weight;

}
