package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.reader.CSSReaderDeclarationList;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CssUtilsTest {

    @Test
    void doesntFailOnGettingNotExistingValueTest() {
        CSSDeclarationList cssStyles = new CSSDeclarationList();
        assertDoesNotThrow(() -> {
            String width = CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH);
            assertEquals("", width);
        });
    }

    @Test
    void getPropertyValueTest() {
        CSSDeclarationList cssStyles = Optional.ofNullable(CSSReaderDeclarationList.readFromString("width: 100px")).orElse(new CSSDeclarationList());
        String width = CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH);
        assertEquals("100px", width);
    }

    @Test
    void setPropertyValueTest() {
        CSSDeclarationList cssStyles = new CSSDeclarationList();
        CssUtils.setPropertyValue(cssStyles, CssProp.WIDTH, "auto");
        assertEquals("auto", CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH));
    }

    @Test
    void overwritePropertyValueTest() {
        CSSDeclarationList cssStyles = Optional.ofNullable(CSSReaderDeclarationList.readFromString("width: auto")).orElse(new CSSDeclarationList());
        CssUtils.setPropertyValue(cssStyles, CssProp.WIDTH, "100px");
        assertEquals("100px", CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH));
    }
}
