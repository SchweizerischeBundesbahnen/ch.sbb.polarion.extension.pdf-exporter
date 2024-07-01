package ch.sbb.polarion.extension.pdf.exporter.util.exporter;

import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.api.utils.html.impl.HtmlBuilder;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import com.polarion.alm.shared.rt.document.PartIdGeneratorImpl;
import com.polarion.alm.shared.rt.parts.impl.readonly.PageBreakPart;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static ch.sbb.polarion.extension.pdf.exporter.util.exporter.Constants.*;

/**
 * Extended version of {@link com.polarion.alm.shared.dle.document.DocumentRenderer}.
 * Used to implement 'page break' feature using polarion native markers.
 */
public class CustomPageBreakPart extends PageBreakPart {

    public CustomPageBreakPart(PageBreakPart object) {
        super(object.getElement(), new PartIdGeneratorImpl());
    }

    /**
     * Initially polarion inserts &lt;pd4ml:page.break&gt; loosing orientation selected by user.
     * So instead of &lt;pd4ml:page.break&gt; we put specific markers which later will help us to generate proper html.
     */
    @Override
    @SneakyThrows
    @SuppressWarnings("java:S3011")
    public void render(@NotNull HtmlBuilder builder, @NotNull RichTextRenderingContext context, int index) {
        RichTextRenderTarget target = context.getRenderTarget();
        if (target.isPdf()) {
            boolean landscape = Boolean.parseBoolean(this.element.getAttribute().byName("data-is-landscape"));

            Method appendFragmentMethod = HtmlBuilder.class.getDeclaredMethod("appendFragment");
            appendFragmentMethod.setAccessible(true);
            HtmlFragmentBuilder fragment = (HtmlFragmentBuilder) appendFragmentMethod.invoke(builder);

            Method htmlMethod = HtmlContentBuilder.class.getDeclaredMethod("html", String.class);
            htmlMethod.setAccessible(true);
            htmlMethod.invoke(fragment, PAGE_BREAK_MARK + (landscape ? LANDSCAPE_ABOVE_MARK : PORTRAIT_ABOVE_MARK));

            fragment.finished();
        } else {
            super.render(builder, context, index);
        }
    }
}
