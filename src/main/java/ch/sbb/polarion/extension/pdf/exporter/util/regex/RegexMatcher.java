package ch.sbb.polarion.extension.pdf.exporter.util.regex;

import lombok.SneakyThrows;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Provides simplified way for string replacements/operations using regex.
 * Also allows to easily switch underlined regex engine: by default it uses RE2J implementation which is a preferable way because
 * in theory it works faster and guarantees linear time execution, but user can switch implementation to the java.util.regex-based
 * in case if there is need to use some features which is unsupported by RE2J - e.g. <code>(?=</code>, <code>(?></code> etc.,
 * full list of (un)supported features may be found <a href="https://github.com/google/re2/wiki/Syntax">here</a>).
 */
@SuppressWarnings("unused")
public class RegexMatcher {

    public static final int CASE_INSENSITIVE = 1;
    public static final int DOTALL = 2;

    private final String regex;
    private final int flags;
    private String impl = RegexEngineRE2JImpl.NAME;

    private RegexMatcher(String regex, int flags) {
        this.regex = regex;
        this.flags = flags;
    }

    public static RegexMatcher get(String regex) {
        return get(regex, 0);
    }

    public static RegexMatcher get(String regex, int flags) {
        return new RegexMatcher(regex, flags);
    }

    public static String quote(String s) {
        return Pattern.quote(s);
    }

    /**
     * Forces to use java.util.regex-based engine.
     * NOTE: this isn't a preferable way, please consider rewriting regex to not use
     * unsupported syntax and switch back to RE2J implementation.
     */
    public RegexMatcher useJavaUtil() {
        this.impl = RegexEngineJavaUtilImpl.NAME;
        return this;
    }

    public String replace(String input, IReplacementCalculator replacementCalculator) {
        return replace(input, true, replacementCalculator);
    }

    public String replace(String input, boolean quoteReplacement, IReplacementCalculator replacementCalculator) {
        IRegexEngine regexEngine = createEngine(input);
        StringBuilder resultBuilder = new StringBuilder();
        while (regexEngine.find()) {
            String replacement = replacementCalculator.calculateReplacement(regexEngine);
            if (replacement != null) {
                regexEngine.appendReplacement(resultBuilder, quoteReplacement ? regexEngine.quoteReplacement(replacement) : replacement);
            }
        }
        regexEngine.appendTail(resultBuilder);
        return resultBuilder.toString();
    }

    public String replaceAll(String input, String replacementString) {
        return replace(input, regexEngine -> replacementString);
    }

    public void processEntry(String input, IEntryProcessor entryProcessor) {
        IRegexEngine regexEngine = createEngine(input);
        while (regexEngine.find()) {
            entryProcessor.processEntry(regexEngine);
        }
    }

    public Optional<String> findFirst(String input) {
        return findFirst(input, IRegexEngine::group);
    }

    public Optional<String> findFirst(String input, Function<IRegexEngine, String> entryCalculator) {
        IRegexEngine regexEngine = createEngine(input);
        return regexEngine.find() ? Optional.of(entryCalculator.apply(regexEngine)) : Optional.empty();
    }

    public boolean anyMatch(String input) {
        return createEngine(input).find();
    }

    public boolean noneMatch(String input) {
        return !createEngine(input).find();
    }

    public String removeAll(String input) {
        return replace(input, regexEngine -> "");
    }

    @SneakyThrows
    public IRegexEngine createEngine(String input) {
        return IRegexEngine.IMPLEMENTATIONS.get(impl)
                .getDeclaredConstructor(String.class, int.class, String.class)
                .newInstance(regex, flags, input);
    }

    public interface IReplacementCalculator {
        String calculateReplacement(IRegexEngine regexEngine);
    }

    public interface IEntryProcessor {
        void processEntry(IRegexEngine regexEngine);
    }

}
