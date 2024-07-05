package ch.sbb.polarion.extension.pdf.exporter.util.regex;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

/**
 * {@link IRegexEngine} implementation based on RE2J.
 * RE2J is a regular expression engine that runs in time linear in the size of the input. RE2J is a port
 * of C++ library RE2 to pure Java.
 * Java's standard regular expression package, java.util.regex, and many other widely used regular expression
 * packages such as PCRE, Perl and Python use a backtracking implementation strategy: when a pattern presents two
 * alternatives such as a|b, the engine will try to match subpattern a first, and if that yields no match,
 * it will reset the input stream and try to match b instead.
 */
public class RegexEngineRE2JImpl implements IRegexEngine {

    public static final String NAME = "re2j";
    private final Matcher matcher;

    public RegexEngineRE2JImpl(String regex, int flags, String input) {

        // RE2J uses slightly different syntax for named groups
        regex = regex.replace("(?<", "(?P<");

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
