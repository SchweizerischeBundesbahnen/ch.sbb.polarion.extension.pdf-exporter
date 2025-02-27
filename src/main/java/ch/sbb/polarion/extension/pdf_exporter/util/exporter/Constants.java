package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

public final class Constants {

    /**
     * Heading tag minimum priority (&lt;h6&gt;).
     */
    public static final int H_TAG_MIN_PRIORITY = 6;

    public static final String PAGE_BREAK_MARK = "<!--PAGE_BREAK-->";
    public static final String PORTRAIT_ABOVE_MARK = "<!--PORTRAIT_ABOVE-->";
    public static final String LANDSCAPE_ABOVE_MARK = "<!--LANDSCAPE_ABOVE-->";
    public static final String PAGE_BREAK_PORTRAIT_ABOVE = PAGE_BREAK_MARK + PORTRAIT_ABOVE_MARK;
    public static final String PAGE_BREAK_LANDSCAPE_ABOVE = PAGE_BREAK_MARK + LANDSCAPE_ABOVE_MARK;
    public static final String MIME_TYPE_SVG = "image/svg+xml";

    public static final String VERSION_FILE = "versions.properties";

    private Constants() {
        // Constants class, not to be instantiated
    }
}
