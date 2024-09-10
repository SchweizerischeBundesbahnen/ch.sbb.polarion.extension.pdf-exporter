package ch.sbb.polarion.extension.pdf_exporter.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of the width validation process")
public class WidthValidationResult {

    @Schema(description = "List of pages that failed the width validation",
            implementation = PageInfo.class)
    private List<PageInfo> invalidPages = new LinkedList<>();

    @Schema(description = "List of work items that are considered suspicious based on the validation",
            implementation = WorkItemRefData.class)
    private List<WorkItemRefData> suspiciousWorkItems = new LinkedList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Information about a specific page that failed validation, including its number and content.")
    public static class PageInfo {

        @Schema(description = "The number of the page that failed validation")
        private int number;

        @Schema(description = "The encoded content of the page that failed validation")
        private String content;
    }
}
