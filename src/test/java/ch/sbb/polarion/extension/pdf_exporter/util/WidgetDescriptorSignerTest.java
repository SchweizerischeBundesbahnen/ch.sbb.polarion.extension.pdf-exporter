package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportColumn;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportWidgetDescriptor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.crypto.Mac;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class WidgetDescriptorSignerTest {

    private final WidgetDescriptorSigner signer = WidgetDescriptorSigner.getInstance();

    @Test
    void encodedDescriptorSurvivesTheRoundTrip() {
        BulkExportWidgetDescriptor descriptor = BulkExportWidgetDescriptor.builder()
                .prototype("TestRun")
                .documentType(DocumentType.TEST_RUN)
                .query("project.id:elibrary")
                .sort("id")
                .top(50)
                .columns(List.of(new BulkExportColumn("id", "ID")))
                .build();

        BulkExportWidgetDescriptor decoded = signer.decode(signer.encode(descriptor), BulkExportWidgetDescriptor.class);

        assertEquals(descriptor, decoded);
    }

    @Test
    void encodedDescriptorIsSafeInAnHtmlAttributeAndAUrl() {
        String encoded = signer.encode(BulkExportWidgetDescriptor.builder().query("title:\"a b\" AND id:<x>").build());

        // base64url, unpadded: nothing that would have to be escaped where the descriptor is carried
        assertTrue(encoded.matches("[A-Za-z0-9_-]+"), encoded);
    }

    @Test
    void ownSignatureVerifies() {
        String encoded = signer.encode(BulkExportWidgetDescriptor.builder().query("id:test").build());

        assertTrue(signer.verify(encoded, signer.sign(encoded)));
    }

    @Test
    void tamperedDescriptorIsRejected() {
        String encoded = signer.encode(BulkExportWidgetDescriptor.builder().query("id:test").build());
        String signature = signer.sign(encoded);
        String tampered = signer.encode(BulkExportWidgetDescriptor.builder().query("id:somethingElse").build());

        assertFalse(signer.verify(tampered, signature));
    }

    @Test
    void foreignAndMissingSignaturesAreRejected() {
        String encoded = signer.encode(BulkExportWidgetDescriptor.builder().query("id:test").build());

        assertFalse(signer.verify(encoded, "deadbeef"));
        assertFalse(signer.verify(encoded, null));
        assertFalse(signer.verify(null, signer.sign(encoded)));
        // A truncated signature must not pass either: the comparison is over the whole value
        assertFalse(signer.verify(encoded, signer.sign(encoded).substring(0, 32)));
    }

    @Test
    void signatureDependsOnTheDescriptor() {
        String one = signer.encode(BulkExportWidgetDescriptor.builder().query("id:one").build());
        String other = signer.encode(BulkExportWidgetDescriptor.builder().query("id:other").build());

        assertEquals(signer.sign(one), signer.sign(one));
        assertNotEquals(signer.sign(one), signer.sign(other));
    }

    @Test
    void aDescriptorThatCannotBeSerializedFailsLoudly() {
        // Not a descriptor at all: Jackson has nothing to write for it. Loud is the point - a silently
        // empty descriptor would reach the browser and come back as a query the endpoint cannot run.
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> signer.encode(new Object()));

        assertTrue(failure.getMessage().contains("encode"), failure.getMessage());
    }

    @Test
    void aBrokenMacFailsLoudly() {
        try (MockedStatic<Mac> macs = mockStatic(Mac.class)) {
            macs.when(() -> Mac.getInstance(anyString())).thenThrow(new NoSuchAlgorithmException("no HmacSHA256 here"));

            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> signer.sign("anything"));

            assertTrue(failure.getMessage().contains("sign"), failure.getMessage());
        }
    }

    @Test
    void garbageIsNotDecoded() {
        assertThrows(IllegalArgumentException.class, () -> signer.decode("not base64 at all!", BulkExportWidgetDescriptor.class));
        assertThrows(IllegalArgumentException.class, () -> signer.decode("bm90SnNvbg", BulkExportWidgetDescriptor.class));
    }
}
