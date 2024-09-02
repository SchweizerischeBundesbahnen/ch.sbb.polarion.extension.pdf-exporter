package ch.sbb.polarion.extension.pdf.exporter.rest.model;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Data class representing a reference to a work item, including ID, project, layout, and revision")
public class WorkItemRefData {
    @Schema(description = "The unique identifier of the Work item")
    private String id;

    @Schema(description = "The unique identifier of the project to which the Work item belongs")
    private String project;

    @Schema(description = "Work item layout")
    private String layout;

    @Schema(description = "The revision number of the Work item")
    private String revision;

    /**
     * Finds tags like &lt;div id="polarion_wiki macro name=module-workitem;params=id=DP-477|layout=1|external=true|project=drivepilot|revision=7871|anchor=babb7126-c0a80111-651e5b30-4062292e"&gt;&lt;/div&gt;
     * and extracts WorkItem references data.
     */
    @SuppressWarnings({"java:S5843", "java:S5855"}) //this regex is necessary according to logic
    public static List<WorkItemRefData> extractListFromHtml(String html, String defaultProject) {
        Set<WorkItemRefData> refs = new LinkedHashSet<>();
        RegexMatcher.get("name=module-workitem;params=(id=(?<id>[\\w\\-]+)|[|]|layout=(?<layout>[\\w\\-]+)|project=(?<project>[\\w\\-]+)|revision=(?<revision>[\\w\\-]+)|[^\"])+")
                .processEntry(html, regexEngine -> {
                    WorkItemRefData ref = new WorkItemRefData();
                    ref.setId(regexEngine.group("id"));
                    ref.setProject(Optional.ofNullable(regexEngine.group("project")).orElse(defaultProject));
                    ref.setLayout(regexEngine.group("layout"));
                    ref.setRevision(regexEngine.group("revision"));
                    refs.add(ref);
                });
        return new ArrayList<>(refs);
    }

    public String toInternalContent() {
        List<String> linkParts = new LinkedList<>();
        linkParts.add("id=" + id);
        if (layout != null) {
            linkParts.add("layout=" + layout);
        }
        linkParts.add("project=" + project);
        if (revision != null) {
            linkParts.add("revision=" + revision);
        }
        return "<div id=\"polarion_wiki macro name=module-workitem;params=" + String.join("|", linkParts) + "\"></div>";
    }

    @JsonProperty("link")
    public String toLink() {
        return String.format("#/project/%s/workitem?id=%s%s", project, id, revision == null ? "" : ("&revision=" + revision));
    }
}
