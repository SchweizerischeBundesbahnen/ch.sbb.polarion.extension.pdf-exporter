package ch.sbb.polarion.extension.pdf.exporter.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkItemRefData {
    private String id;
    private String project;
    private String layout;
    private String revision;

    /**
     * Finds tags like <div id="polarion_wiki macro name=module-workitem;params=id=DP-477|layout=1|external=true|project=drivepilot|revision=7871|anchor=babb7126-c0a80111-651e5b30-4062292e"></div>
     * and extracts WorkItem references data.
     */
    @SuppressWarnings({"java:S5843", "java:S5855"}) //this regex is necessary according to logic
    public static List<WorkItemRefData> extractListFromHtml(String html, String defaultProject) {
        Set<WorkItemRefData> refs = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("name=module-workitem;params=(id=(?<id>[\\w\\-]+)|[|]|layout=(?<layout>[\\w\\-]+)|project=(?<project>[\\w\\-]+)|revision=(?<revision>[\\w\\-]+)|[^\"])+");
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            WorkItemRefData ref = new WorkItemRefData();
            ref.setId(matcher.group("id"));
            ref.setProject(Optional.ofNullable(matcher.group("project")).orElse(defaultProject));
            ref.setLayout(matcher.group("layout"));
            ref.setRevision(matcher.group("revision"));
            refs.add(ref);
        }
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
