package ch.sbb.polarion.extension.pdf_exporter.util;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows to check which data was logged by the Polarion logger.
 * WARNING: in order to be properly registered this appender must be configured in the log4j2.xml in the classpath
 */
@SuppressWarnings("unused")
@Plugin(name = "InMemoryAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class InMemoryAppender extends AbstractAppender {

    private static final List<String> logMessages = new ArrayList<>();

    protected InMemoryAppender(String name, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, null, layout, ignoreExceptions, null);
    }


    @PluginFactory
    public static InMemoryAppender createAppender(@PluginAttribute("name") String name,
                                                  @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
                                                  Layout<? extends Serializable> layout) {
        return new InMemoryAppender(name, layout != null ? layout : PatternLayout.createDefaultLayout(), ignoreExceptions);
    }

    public static boolean anyMessageContains(String messagePart) {
        return logMessages.stream().anyMatch(m -> m.contains(messagePart));
    }

    @Override
    public void append(LogEvent event) {
        logMessages.add(getLayout() != null ? new String(getLayout().toByteArray(event)) : event.getMessage().getFormattedMessage());
    }
}
