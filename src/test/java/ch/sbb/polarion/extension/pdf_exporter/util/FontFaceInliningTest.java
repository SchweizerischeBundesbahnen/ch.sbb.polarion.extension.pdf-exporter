package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** The fonts of {@code @font-face} rules: one source of each, and none of a family nothing names. */
@ExtendWith(PdfExporterExtensionConfigurationExtension.class)
class FontFaceInliningTest {

    private FileResourceProvider provider;

    @BeforeEach
    void setUp() {
        provider = mock(FileResourceProvider.class);
        lenient().when(provider.getResourceAsBase64String(anyString())).thenAnswer(invocation -> "data:font/x;base64," + fileOf(invocation.getArgument(0)));
    }

    private static String fileOf(String url) {
        return url.substring(url.lastIndexOf('/') + 1).replace('.', '_');
    }

    @Test
    void keepsTheSourceOfAFormatWeasyPrintReads() {
        String css = MediaUtils.inlineCssResources("""
                @font-face { font-family: "Icons"; src: url('/fonts/icons.woff2') format('woff2'), url('/fonts/icons.ttf') format('truetype'); }""", provider);

        assertThat(css).contains("src: url(data:font/x;base64,icons_ttf) format('truetype')").doesNotContain("woff2");
        verify(provider, never()).getResourceAsBase64String(contains("woff2"));
    }

    @Test
    void tellsTheFormatByTheNameOfTheFileWhereNoneIsStated() {
        String css = MediaUtils.inlineCssResources("""
                @font-face { font-family: "Icons"; src: url(/fonts/icons.eot), url(/fonts/icons.woff2), url(/fonts/icons.woff); }""", provider);

        assertThat(css).contains("src: url(data:font/x;base64,icons_woff)").doesNotContain("icons_eot").doesNotContain("icons_woff2");
    }

    @Test
    void keepsALocalSource() {
        String css = MediaUtils.inlineCssResources("""
                @font-face { font-family: "Icons"; src: local("Icons"), url(/fonts/icons.woff2) format("woff2"), url(/fonts/icons.ttf) format("truetype"); }""", provider);

        assertThat(css).contains("src: local(\"Icons\"), url(data:font/x;base64,icons_ttf) format(\"truetype\")");
    }

    @Test
    void leavesASingleSourceAndAListWithNoReadableOneAsTheyWere() {
        String single = MediaUtils.inlineCssResources("@font-face { font-family: A; src: url(/fonts/a.ttf); }", provider);
        String unreadable = MediaUtils.inlineCssResources("@font-face { font-family: B; src: url(/fonts/b.woff2) format('woff2'), url(/fonts/b.svg) format('svg'); }", provider);

        assertThat(single).isEqualTo("@font-face { font-family: A; src: url(data:font/x;base64,a_ttf); }");
        assertThat(unreadable).contains("b_woff2").contains("b_svg");
    }

    @Test
    void leavesOutTheFontOfAFamilyNothingNames() {
        String css = MediaUtils.inlineCssResources("""
                @font-face { font-family: "Font Awesome 6 Free"; src: url(/fonts/solid.ttf) format("truetype"); }
                @font-face { font-family: "FontAwesome"; src: url(/fonts/solid.ttf) format("truetype"); }
                .fa { font-family: "Font Awesome 6 Free"; }""", provider, null, FontFamilyUse.in(".fa { font-family: \"Font Awesome 6 Free\"; }"));

        assertThat(css).contains("Font Awesome 6 Free").doesNotContain("\"FontAwesome\"");
        assertThat(css.split("solid_ttf", -1)).as("the font of the named family, once").hasSize(2);
    }

    /** A font of its own may be named in a stylesheet this one cannot see, as a sibling link of the same document. */
    @Test
    void keepsTheFontOfItsOwnOfAFamilyNothingHereNames() {
        String css = MediaUtils.inlineCssResources("""
                @font-face { font-family: "Corporate"; src: url(/fonts/corporate.ttf) format("truetype"); }""", provider, null, FontFamilyUse.in(".x { color: red; }"));

        assertThat(css).contains("corporate_ttf");
    }
}
