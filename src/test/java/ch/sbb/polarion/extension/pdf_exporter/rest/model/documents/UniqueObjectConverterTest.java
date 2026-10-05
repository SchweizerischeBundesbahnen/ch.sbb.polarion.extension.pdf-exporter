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
        IModule oldApi = mock(IModule.class);
        when(document.getOldApi()).thenReturn(oldApi);
        new UniqueObjectConverter(document);
        verify(document, times(1)).getOldApi();
    }

    @Test
    void testRichPage() {
        RichPage richPage = mock(RichPage.class);
        IRichPage oldApi = mock(IRichPage.class);
        when(richPage.getOldApi()).thenReturn(oldApi);
        new UniqueObjectConverter(richPage);
        verify(richPage, times(1)).getOldApi();
    }

    @Test
    void testTestRun() {
        TestRun testRun = mock(TestRun.class);
        ITestRun oldApi = mock(ITestRun.class);
        when(testRun.getOldApi()).thenReturn(oldApi);
        new UniqueObjectConverter(testRun);
        verify(testRun, times(1)).getOldApi();
    }

    @Test
    void testWikiPage() {
        WikiPage wikiPage = mock(WikiPage.class);
        IWikiPage oldApi = mock(IWikiPage.class);
        when(wikiPage.getOldApi()).thenReturn(oldApi);
        new UniqueObjectConverter(wikiPage);
        verify(wikiPage, times(1)).getOldApi();
    }
}
