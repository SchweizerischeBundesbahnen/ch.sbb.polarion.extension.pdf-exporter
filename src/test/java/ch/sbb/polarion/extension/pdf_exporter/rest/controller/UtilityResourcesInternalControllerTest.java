package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.generic.service.PolarionCollectionExecutor;
import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.platform.persistence.IEnumOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, TransactionalExecutorExtension.class})
class UtilityResourcesInternalControllerTest {

    private static final String PROJECT = "elibrary";
    private static final String SPACE = "Testing";
    private static final String DOCUMENT = "Module A";

    @CustomExtensionMock
    private InternalReadOnlyTransaction transaction;

    @Mock
    private PdfExporterPolarionService pdfExporterPolarionService;

    private UtilityResourcesInternalController controller;

    @BeforeEach
    void setUp() {
        controller = new UtilityResourcesInternalController(pdfExporterPolarionService);

        IEnumOption german = mock(IEnumOption.class);
        lenient().when(german.getId()).thenReturn("de");
        IModule atCollectionRevision = mock(IModule.class);
        lenient().when(atCollectionRevision.getCustomField("docLanguage")).thenReturn(german);
        lenient().when(pdfExporterPolarionService.getModule(PROJECT, SPACE, DOCUMENT, "100")).thenReturn(atCollectionRevision);

        // The language of an older revision asked for, and none at HEAD
        IEnumOption french = mock(IEnumOption.class);
        lenient().when(french.getId()).thenReturn("fr");
        IModule atRevisionAskedFor = mock(IModule.class);
        lenient().when(atRevisionAskedFor.getCustomField("docLanguage")).thenReturn(french);
        lenient().when(pdfExporterPolarionService.getModule(PROJECT, SPACE, DOCUMENT, "77")).thenReturn(atRevisionAskedFor);
        IModule atHead = mock(IModule.class);
        lenient().when(pdfExporterPolarionService.getModule(PROJECT, SPACE, DOCUMENT, null)).thenReturn(atHead);
    }

    @Test
    void theLanguageOfADocumentOfACollectionIsTheOneOfTheRevisionTheCollectionHoldsItAt() {
        DocumentReference held = mock(DocumentReference.class);
        when(held.requestedRevision()).thenReturn("100");

        try (MockedStatic<PolarionCollectionExecutor> collectionExecutor = mockStatic(PolarionCollectionExecutor.class)) {
            ArgumentCaptor<DocumentReference> asked = ArgumentCaptor.forClass(DocumentReference.class);
            collectionExecutor.when(() -> PolarionCollectionExecutor.resolveDocument(eq(PROJECT), eq("drivepilot/3"), asked.capture(), eq(transaction))).thenReturn(held);

            assertEquals("de", controller.getDocumentLanguage(PROJECT, SPACE, DOCUMENT, null, "drivepilot/3"));
            assertEquals(PROJECT, asked.getValue().projectId());
        }
    }

    @Test
    void aRevisionAskedForGoesBeforeTheOneOfTheCollection() {
        try (MockedStatic<PolarionCollectionExecutor> collectionExecutor = mockStatic(PolarionCollectionExecutor.class)) {
            assertEquals("fr", controller.getDocumentLanguage(PROJECT, SPACE, DOCUMENT, "77", "3"));
            collectionExecutor.verifyNoInteractions();
        }
    }

    @Test
    void aDocumentOnItsOwnIsReadAtHead() {
        try (MockedStatic<PolarionCollectionExecutor> collectionExecutor = mockStatic(PolarionCollectionExecutor.class)) {
            assertNull(controller.getDocumentLanguage(PROJECT, SPACE, DOCUMENT, null, null));
            assertNull(controller.getDocumentLanguage(PROJECT, SPACE, DOCUMENT, null, ""));
            collectionExecutor.verifyNoInteractions();
        }
        verify(pdfExporterPolarionService, times(2)).getModule(PROJECT, SPACE, DOCUMENT, null);
    }

    @Test
    void aCollectionWhichDoesNotExistIsRefused() {
        try (MockedStatic<PolarionCollectionExecutor> collectionExecutor = mockStatic(PolarionCollectionExecutor.class)) {
            collectionExecutor.when(() -> PolarionCollectionExecutor.resolveDocument(any(), any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Collection '999' does not exist"));

            assertThrows(IllegalArgumentException.class, () -> controller.getDocumentLanguage(PROJECT, SPACE, DOCUMENT, null, "999"));
        }
    }
}
