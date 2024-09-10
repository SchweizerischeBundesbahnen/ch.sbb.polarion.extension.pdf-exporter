package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.DefaultXWikiRenderingEngine;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * Renders wiki page using internal polarion XWiki renderer.
 */
public class WikiRenderer {

    @SneakyThrows
    public String render(@NotNull String projectId, @NotNull String locationPath, String revision) {
        XWikiContext context = new XWikiContext();

        URL url = new URL(System.getProperty("base.url")); //is there any better way to get polarion url?
        context.setURL(url);
        context.setURLFactory(new XWikiServletURLFactory(url, null, null));

        //it would be great to find a less hacky solution
        XWiki xwiki = new XWiki(new XWikiConfig(XWiki.class.getProtectionDomain().getCodeSource().getLocation().getPath()
                .replace("sidecar.jar", "src/main/webapp/WEB-INF/xwiki.cfg")), context);
        context.setWiki(xwiki);

        XWikiDocument doc = new XWikiDocument();
        doc.setFullName("%s/%s".formatted(projectId, locationPath), context);
        XWikiDocument document = xwiki.getDocument(doc, revision, context);

        //taken from com.xpn.xwiki.pdf.impl.PdfExportImpl#createPD4ML
        context.setDoc(document);
        context.setAction("view");
        context.put("pdf_generate", "1");
        context.getWiki().prepareResources(context);

        return new DefaultXWikiRenderingEngine(xwiki, context).renderDocument(document, context);
    }
}
