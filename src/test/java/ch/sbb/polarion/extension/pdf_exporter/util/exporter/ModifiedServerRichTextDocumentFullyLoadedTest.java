package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.shared.api.utils.collections.StrictListImpl;
import com.polarion.alm.shared.rt.parts.impl.readonly.PageBreakPart;
import com.polarion.alm.shared.rt.parts.impl.readonly.WikiBlockPart;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

@SuppressWarnings("rawtypes")
class ModifiedServerRichTextDocumentFullyLoadedTest {

    @Test
    @SneakyThrows
    void testAddPart() {
        ModifiedServerRichTextDocumentFullyLoaded doc = mock(ModifiedServerRichTextDocumentFullyLoaded.class);
        doCallRealMethod().when(doc).addPart(any());
        FieldUtils.writeField(doc, "parts", new StrictListImpl(), true);

        PageBreakPart pageBreakPart = mock(PageBreakPart.class);
        assertDoesNotThrow(() -> doc.addPart(pageBreakPart));
        WikiBlockPart wikiBlockPart = mock(WikiBlockPart.class);
        assertDoesNotThrow(() -> doc.addPart(wikiBlockPart));
    }

}
