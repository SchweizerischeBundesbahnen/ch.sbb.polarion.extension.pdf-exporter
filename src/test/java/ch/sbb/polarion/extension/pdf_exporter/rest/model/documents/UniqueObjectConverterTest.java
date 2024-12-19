package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents;

import com.polarion.alm.shared.api.model.document.Document;
import com.polarion.alm.shared.api.model.rp.RichPage;
import com.polarion.alm.shared.api.model.tr.TestRun;
import com.polarion.alm.shared.api.model.wiki.WikiPage;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWikiPage;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class UniqueObjectConverterTest {

    @Test
    void testDocument() {
        Document document = mock(Document.class);
        when(document.getOldApi()).thenReturn(mock(IModule.class));
        new UniqueObjectConverter(document);
        verify(document, times(1)).getOldApi();
    }

    @Test
    void testRichPage() {
        RichPage richPage = mock(RichPage.class);
        when(richPage.getOldApi()).thenReturn(mock(IRichPage.class));
        new UniqueObjectConverter(richPage);
        verify(richPage, times(1)).getOldApi();
    }

    @Test
    void testTestRun() {
        TestRun testRun = mock(TestRun.class);
        when(testRun.getOldApi()).thenReturn(mock(ITestRun.class));
        new UniqueObjectConverter(testRun);
        verify(testRun, times(1)).getOldApi();
    }

    @Test
    void testWikiPage() {
        WikiPage wikiPage = mock(WikiPage.class);
        when(wikiPage.getOldApi()).thenReturn(mock(IWikiPage.class));
        new UniqueObjectConverter(wikiPage);
        verify(wikiPage, times(1)).getOldApi();
    }
}
