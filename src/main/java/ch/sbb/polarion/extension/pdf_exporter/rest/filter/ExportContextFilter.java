package ch.sbb.polarion.extension.pdf_exporter.rest.filter;

import ch.sbb.polarion.extension.pdf_exporter.util.ExportContext;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

public class ExportContextFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        ExportContext.clear();
    }
}
