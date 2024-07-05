package ch.sbb.polarion.extension.pdf.exporter.util.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link IRegexEngine} implementation based on standard Java regex engine.
 */
public class RegexEngineJavaUtilImpl implements IRegexEngine {

    public static final String NAME = "java.util.regex";
    private final Matcher matcher;

    public RegexEngineJavaUtilImpl(String regex, int flags, String input) {
        matcher = Pattern.compile(regex, flags).matcher(input);
    }

    @Override
    public boolean find() {
        return matcher.find();
    }

    @Override
    public String group() {
        return matcher.group();
    }

    @Override
    public String group(int group) {
        return matcher.group(group);
    }

    @Override
    public String group(String name) {
        return matcher.group(name);
    }

    @Override
    public void appendReplacement(StringBuilder sb, String replacement) {
        matcher.appendReplacement(sb, replacement);
    }

    @Override
    public void appendTail(StringBuilder sb) {
        matcher.appendTail(sb);
    }

    @Override
    public String quoteReplacement(String input) {
        return Matcher.quoteReplacement(input);
    }
}
