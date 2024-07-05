package ch.sbb.polarion.extension.pdf.exporter.util.regex;

import java.util.Map;

public interface IRegexEngine {

    Map<String, Class<? extends IRegexEngine>> IMPLEMENTATIONS = Map.of(
            RegexEngineJavaUtilImpl.NAME, RegexEngineJavaUtilImpl.class,
            RegexEngineRE2JImpl.NAME, RegexEngineRE2JImpl.class
    );

    boolean find();

    String group();

    String group(int group);

    String group(String name);

    void appendReplacement(StringBuilder sb, String replacement);

    void appendTail(StringBuilder sb);

    String quoteReplacement(String input);
}
