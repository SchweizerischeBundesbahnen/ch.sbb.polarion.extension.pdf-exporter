package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItems;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItemsRequest;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportWidgetDescriptor;
import ch.sbb.polarion.extension.pdf_exporter.util.BulkExportWidgetHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.WidgetDescriptorSigner;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, TransactionalExecutorExtension.class})
class BulkExportWidgetInternalControllerTest {

    @CustomExtensionMock
    private InternalReadOnlyTransaction transactionMock;

    @Mock
    private BulkExportWidgetHelper helper;

    @InjectMocks
    private BulkExportWidgetInternalController controller;

    @Test
    void aSignedDescriptorIsExecuted() {
        BulkExportWidgetDescriptor descriptor = BulkExportWidgetDescriptor.builder()
                .prototype("TestRun")
                .documentType(DocumentType.TEST_RUN)
                .query("status:passed")
                .top(50)
                .build();
        String encoded = WidgetDescriptorSigner.getInstance().encode(descriptor);
        BulkExportItems items = BulkExportItems.builder().totalCount(3).build();
        when(helper.getItems(any(), any())).thenReturn(items);

        BulkExportItems result = controller.getItems(new BulkExportItemsRequest(encoded, WidgetDescriptorSigner.getInstance().sign(encoded)));

        assertSame(items, result);
        ArgumentCaptor<BulkExportWidgetDescriptor> captor = ArgumentCaptor.forClass(BulkExportWidgetDescriptor.class);
        verify(helper).getItems(captor.capture(), any());
        assertEquals(descriptor, captor.getValue());
    }

    @Test
    void anUnsignedDescriptorIsRejected() {
        String encoded = WidgetDescriptorSigner.getInstance().encode(BulkExportWidgetDescriptor.builder().query("status:passed").build());
        BulkExportItemsRequest unsigned = new BulkExportItemsRequest(encoded, null);
        BulkExportItemsRequest wronglySigned = new BulkExportItemsRequest(encoded, "0000");

        assertThrows(BadRequestException.class, () -> controller.getItems(unsigned));
        assertThrows(BadRequestException.class, () -> controller.getItems(wronglySigned));
        verify(helper, never()).getItems(any(), any());
    }

    @Test
    void aDescriptorSignedForAnotherPayloadIsRejected() {
        // What an attacker would send: the signature of the descriptor the page carries, plus a query of their own
        String own = WidgetDescriptorSigner.getInstance().encode(BulkExportWidgetDescriptor.builder().query("status:passed").build());
        String forged = WidgetDescriptorSigner.getInstance().encode(
                BulkExportWidgetDescriptor.builder().sqlQuery(true).query("SELECT * FROM WORKITEM").build());

        BulkExportItemsRequest request = new BulkExportItemsRequest(forged, WidgetDescriptorSigner.getInstance().sign(own));

        assertThrows(BadRequestException.class, () -> controller.getItems(request));
        verify(helper, never()).getItems(any(), any());
    }

    @Test
    void anEmptyRequestIsRejected() {
        BulkExportItemsRequest empty = new BulkExportItemsRequest(null, null);

        assertThrows(BadRequestException.class, () -> controller.getItems(null));
        assertThrows(BadRequestException.class, () -> controller.getItems(empty));
        verify(helper, never()).getItems(any(), any());
    }

    @Test
    void bothControllersAreConstructibleTheWayJerseyDoesIt() {
        // Jersey instantiates the registered classes through their no-argument constructor
        assertNotNull(new BulkExportWidgetInternalController());
        assertNotNull(new BulkExportWidgetApiController());
    }

    @Test
    void theApiControllerRunsAsTheCallingUser() {
        // Not privileged on purpose: a widget must not show items the calling user cannot read
        BulkExportItems items = BulkExportItems.builder().totalCount(1).build();
        BulkExportWidgetHelper apiHelper = mock(BulkExportWidgetHelper.class);
        when(apiHelper.getItems(any(), any())).thenReturn(items);
        String encoded = WidgetDescriptorSigner.getInstance().encode(BulkExportWidgetDescriptor.builder().query("id:x").build());

        BulkExportWidgetApiController apiController = new BulkExportWidgetApiController(apiHelper);

        assertSame(items, apiController.getItems(new BulkExportItemsRequest(encoded, WidgetDescriptorSigner.getInstance().sign(encoded))));
    }
}
