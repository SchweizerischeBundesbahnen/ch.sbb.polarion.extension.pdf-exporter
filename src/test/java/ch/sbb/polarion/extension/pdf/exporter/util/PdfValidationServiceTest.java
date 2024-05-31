package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.pdf.exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.WidthValidationResult;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.WorkItemRefData;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PdfValidationServiceTest {

    @Test
    @SneakyThrows
    void extraWidthTest() {
        try (InputStream is = this.getClass().getResourceAsStream("/test_extra_width_content.pdf")) {
            assertNotNull(is);
            PdfValidationService service = new PdfValidationService(mock(PdfConverter.class));
            List<WidthValidationResult.PageInfo> invalidPages = service.findInvalidPages(IOUtils.toByteArray(is), 5);
            assertEquals(3, invalidPages.size());
            assertTrue(invalidPages.stream().map(WidthValidationResult.PageInfo::getNumber).collect(Collectors.toSet()).containsAll(Arrays.asList(0, 1, 2)));
        }
    }

    @Test
    @SneakyThrows
    void extraWidthMaxResultsTest() {
        try (InputStream is = this.getClass().getResourceAsStream("/test_extra_width_content.pdf")) {
            assertNotNull(is);
            PdfValidationService service = new PdfValidationService(mock(PdfConverter.class));
            List<WidthValidationResult.PageInfo> invalidPages = service.findInvalidPages(IOUtils.toByteArray(is), 2);
            assertEquals(2, invalidPages.size());
        }
    }

    @Test
    void refParseTest() {
        String init = "<div id=\"polarion_wiki macro name=module-workitem;params=id=DP-477|layout=1|external=true|project=drivepilot|revision=7871|anchor=babb7126-c0a80111-651e5b30-4062292e\">" +
                "</div><p id=\"polarion_1\"></p><p id=\"polarion_2\">" +
                "<div id=\"polarion_wiki macro name=module-workitem;params=id=DP-455|some-bad-param\">";
        List<WorkItemRefData> refs = WorkItemRefData.extractListFromHtml(init, "test");
        assertEquals(2, refs.size());
        assertEquals(new WorkItemRefData("DP-477", "drivepilot", "1", "7871"), refs.get(0));
        assertEquals(new WorkItemRefData("DP-455", "test", null, null), refs.get(1));
    }
}
