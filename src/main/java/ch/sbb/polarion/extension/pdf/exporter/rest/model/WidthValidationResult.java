package ch.sbb.polarion.extension.pdf.exporter.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class WidthValidationResult {

    private List<PageInfo> invalidPages = new LinkedList<>();
    private List<WorkItemRefData> suspiciousWorkItems = new LinkedList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageInfo {
        private int number;
        private String content;
    }
}
