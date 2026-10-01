package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Documents of a known shape, written as Polarion writes the content of a LiveDoc. */
@UtilityClass
class Documents {

    private static final String TABLE = "<table class=\"polarion-Document-table\" style=\"width:100%;border:1px solid #CCCCCC;empty-cells:show;border-collapse:collapse;\">";
    private static final String TH = "<th style=\"font-weight:bold;font-size:9pt;line-height:1.5;text-align:left;vertical-align:top;width:auto;height:12px;border:1px solid #CCCCCC;padding:5px;\">";
    private static final String TD = "<td style=\"font-size:9pt;line-height:1.5;text-align:left;vertical-align:top;width:auto;height:12px;border:1px solid #CCCCCC;padding:5px;\">";

    private static final String SENTENCE = "The system shall keep the data of a patron for as long as the law asks for it and delete it afterwards. ";

    /** A document table of the given rows, each of an ID, a title, a description, a URL, a status and a date. */
    static @NotNull String largeTable(int rows) {
        String body = IntStream.range(0, rows).mapToObj(row -> "<tr>"
                + TD + "REQ-" + (1000 + row) + "</td>"
                + TD + "Requirement number " + row + "</td>"
                + TD + SENTENCE.repeat(1 + row % 3) + "</td>"
                + TD + "https://polarion.example.com/polarion/#/project/elibrary/workitem?id=REQ-" + (1000 + row) + "</td>"
                + TD + (row % 2 == 0 ? "Approved" : "In review") + "</td>"
                + TD + "2026-10-" + String.format("%02d", 1 + row % 28) + "</td></tr>")
                .collect(Collectors.joining("\n"));
        return "<h1>A large table</h1>" + TABLE + "<thead><tr>" + TH + "ID</th>" + TH + "Title</th>" + TH + "Description</th>"
                + TH + "Link</th>" + TH + "Status</th>" + TH + "Date</th></tr></thead><tbody>" + body + "</tbody></table>";
    }

    /** A table whose cells hold text running across pages, the shape which made exports take minutes (#1101). */
    static @NotNull String longCells(int cells, int characters) {
        String text = SENTENCE.repeat(characters / SENTENCE.length() + 1).substring(0, characters);
        return "<h1>Long cells</h1>" + TABLE + "<tbody><tr>" + (TD + text + "</td>").repeat(cells) + "</tr></tbody></table>";
    }

    /** Paragraphs and table cells holding raster images of a few sizes. */
    static @NotNull String manyImages(int images) {
        String body = IntStream.range(0, images).mapToObj(index -> index % 2 == 0
                        ? "<p><img src=\"" + png(index) + "\" style=\"width: " + (200 + index % 5 * 100) + "px;\"/></p>"
                        : TABLE + "<tbody><tr>" + TD + "<img src=\"" + png(index) + "\" style=\"width: 900px; height: 300px;\"/></td>" + TD + "A cell beside an image</td></tr></tbody></table>")
                .collect(Collectors.joining("\n"));
        return "<h1>Many images</h1>" + body;
    }

    /** Work items with their fields, as a document renders them, each followed by a table of its attributes. */
    static @NotNull String manyWorkItems(int workItems) {
        String body = IntStream.range(0, workItems).mapToObj(index -> {
            String id = "EL-" + (5000 + index);
            return "<div id=\"polarion_wiki macro name=module-workitem;params=id=" + id + "\" class=\"polarion-dle-workitem-basic-0\" title=\"Requirement: " + id + "\">"
                    + "<a id=\"work-item-anchor-elibrary/" + id + "\"></a>"
                    + "<span id=\"polarion_editor_field=title\" class=\"polarion-dle-workitem-title\"><span id=\"polarion_editor_fields_container_start\" class=\"polarion-dle-workitem-fields-start\">"
                    + "<span id=\"polarion_editor_field=id\">" + id + "</span> -&nbsp;</span>Requirement " + index + "<br/></span>"
                    + "<p>" + SENTENCE.repeat(2) + "</p>"
                    + TABLE + "<tbody><tr>" + TD + "Status</td>" + TD + "Approved</td></tr><tr>" + TD + "Severity</td>" + TD + "Must have</td></tr></tbody></table>"
                    + "</div>";
        }).collect(Collectors.joining("\n"));
        return "<h1>Many work items</h1>" + body;
    }

    /** Sections which page breaks turn landscape and back, each holding a wide table. */
    static @NotNull String pageBreakSections(int sections) {
        String body = IntStream.range(0, sections).mapToObj(index -> "<h2>Section " + index + "</h2>"
                        + largeTable(8).replace("<h1>A large table</h1>", "")
                        + (index % 2 == 0 ? "<!--PAGE_BREAK--><!--LANDSCAPE_ABOVE-->" : "<!--PAGE_BREAK--><!--PORTRAIT_ABOVE-->"))
                .collect(Collectors.joining("\n"));
        return "<h1>Page breaks</h1>" + body;
    }

    /** Tables of nine narrow columns, whose words leave them no room until they break into parts. */
    static @NotNull String crampedTables(int tables) {
        String row = "<tr>" + IntStream.range(0, 9).mapToObj(column -> TD + "Sicherheitsanforderungen TMSPRG-" + (13164 + column) + "</td>").collect(Collectors.joining()) + "</tr>";
        String table = TABLE + "<tbody>" + row.repeat(6) + "</tbody></table>";
        return "<h1>Cramped tables</h1>" + IntStream.range(0, tables).mapToObj(index -> "<h2>Table " + index + "</h2>" + table).collect(Collectors.joining("\n"));
    }

    /** Tables of long German compounds, which a document with a language hyphenates. */
    static @NotNull String hyphenatedTables(int tables) {
        String text = "Die Donaudampfschifffahrtsgesellschaftskapitänsmütze wurde von der&nbsp;Rechtsschutzversicherungsgesellschaft geprüft. "
                + "Selbst die&nbsp;Grundstücksverkehrsgenehmigungszuständigkeitsübertragungsverordnung verlangt eine&nbsp;Arbeiterunfallversicherungsgesetznovelle.";
        String table = TABLE + "<tbody><tr>" + (TD + text + "</td>").repeat(3) + "</tr></tbody></table>";
        return "<h1>Silbentrennung</h1>" + IntStream.range(0, tables).mapToObj(index -> "<h2>Tabelle " + index + "</h2>" + table).collect(Collectors.joining("\n"));
    }

    /** A small raster image of its own color, so that no two are the same. */
    @SneakyThrows
    private static @NotNull String png(int index) {
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 200, 100);
            graphics.setColor(new Color(Color.HSBtoRGB(index / 37f, 0.6f, 0.8f)));
            graphics.fillRect(10, 10, 180, 80);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
