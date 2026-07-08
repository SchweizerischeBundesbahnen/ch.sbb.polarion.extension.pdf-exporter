package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

public final class Constants {

    /**
     * Heading tag minimum priority (&lt;h6&gt;).
     */
    public static final int H_TAG_MIN_PRIORITY = 6;

    /**
     * Maximum heading level supported by Polarion.
     * Levels 1-6 use standard &lt;h1&gt;-&lt;h6&gt; tags, levels 7-30 use &lt;div class="heading-N"&gt;.
     */
    public static final int H_TAG_MAX_LEVEL = 30;

    public static final String PAGE_BREAK = "PAGE_BREAK";
    public static final String PORTRAIT_ABOVE = "PORTRAIT_ABOVE";
    public static final String LANDSCAPE_ABOVE = "LANDSCAPE_ABOVE";
    public static final String ROTATE_BELOW = "ROTATE_BELOW";
    public static final String RESET_BELOW = "RESET_BELOW";
    public static final String BREAK_BELOW = "BREAK_BELOW";

    public static final String PAGE_BREAK_MARK = "<!--PAGE_BREAK-->";
    public static final String PORTRAIT_ABOVE_MARK = "<!--PORTRAIT_ABOVE-->";
    public static final String LANDSCAPE_ABOVE_MARK = "<!--LANDSCAPE_ABOVE-->";
    public static final String ROTATE_BELOW_MARK = "<!--ROTATE_BELOW-->";
    public static final String RESET_BELOW_MARK = "<!--RESET_BELOW-->";
    public static final String BREAK_BELOW_MARK = "<!--BREAK_BELOW-->";
    public static final String MIME_TYPE_SVG = "image/svg+xml";

    /**
     * Marker class emitted by the {@code PageBreakWidget} report widget and the contract between it and
     * {@code HtmlProcessor}. The widget output lands inside a {@code polarion-rp-column-layout} table cell, where the CSS
     * {@code page} property cannot switch the page orientation. The widget itself only forces a renderer-independent page
     * break; when exporting through this extension, {@code HtmlProcessor} additionally lifts the content following the
     * marker out of the cell into a body-level orientation section.
     */
    public static final String PAGE_BREAK_WIDGET_CLASS = "pdf-exporter-page-break";

    /**
     * Modifier class added next to {@link #PAGE_BREAK_WIDGET_CLASS} when the widget's {@code landscape} parameter is set,
     * telling {@code HtmlProcessor} to switch the lifted section to landscape; otherwise it is a plain portrait break.
     */
    public static final String PAGE_BREAK_WIDGET_LANDSCAPE_CLASS = "pdf-exporter-page-break-landscape";

    public static final String VERSION_FILE = "versions.properties";

    private Constants() {
        // Constants class, not to be instantiated
    }
}
