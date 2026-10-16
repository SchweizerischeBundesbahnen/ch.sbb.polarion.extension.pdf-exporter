package ch.sbb.polarion.extension.pdf_exporter.util;

import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSExpressionMemberTermURI;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.ICSSTopLevelRule;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.decl.visit.DefaultCSSUrlVisitor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltInStylesheetsTest {

    /** The export embeds every url of its stylesheet, so one Polarion does not serve is reported on every export. */
    @ParameterizedTest
    @ValueSource(strings = {"/default/dle-pdf-export.css", "/default/wiki.css"})
    @SneakyThrows
    void nameNoImageOfTheOldWikiSkin(String resource) {
        try (InputStream stream = BuiltInStylesheetsTest.class.getResourceAsStream(resource)) {
            CascadingStyleSheet stylesheet = ExportStylesheet.read(new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8));
            List<String> urls = new ArrayList<>();
            CSSVisitor.visitCSSUrl(Objects.requireNonNull(stylesheet), new DefaultCSSUrlVisitor() {
                @Override
                public void onUrlDeclaration(@Nullable ICSSTopLevelRule topLevelRule, @NotNull CSSDeclaration declaration, @NotNull CSSExpressionMemberTermURI uri) {
                    urls.add(uri.getURIString());
                }
            });

            assertThat(urls).isNotEmpty().doesNotContain("/polarion/wiki/skins/sidecar/msg.png");
        }
    }
}
