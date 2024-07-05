package ch.sbb.polarion.extension.pdf.exporter.util.regex;

import com.google.re2j.PatternSyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RegexMatcherTest {

    private static final String REGEX_TAG = "<(?<tagName>[a-z]+)>";

    @Test
    void flagsTest() {
        String input = "Text with <SPAN>tag</SPAN> inside";
        assertNull(RegexMatcher.get(REGEX_TAG).findFirst(input).orElse(null));
        assertEquals("<SPAN>", RegexMatcher.get(REGEX_TAG, RegexMatcher.CASE_INSENSITIVE).findFirst(input).orElse(null));
    }

    @Test
    void unsupportedRE2JSyntaxTest() {
        RegexMatcher simpleBackreferenceMatcher = RegexMatcher.get("(')[a-zA-Z]+\\1");

        // backreferences not supported in RE2J
        PatternSyntaxException exception = (PatternSyntaxException) assertThrows(InvocationTargetException.class,
                () -> simpleBackreferenceMatcher.findFirst("Text with 'quotedText' inside")).getCause();
        assertEquals("error parsing regexp: invalid escape sequence: `\\1`", exception.getMessage());
        // but standard java regex does support them
        assertEquals("'quotedText'", simpleBackreferenceMatcher.useJavaUtil().findFirst("Text with 'quotedText' inside").orElse(null));

        // same for positive lookaheads (?=
        RegexMatcher positiveLookaheadMatcher = RegexMatcher.get("[a-z]+(?=\\d)");
        exception = (PatternSyntaxException) assertThrows(InvocationTargetException.class,
                () -> positiveLookaheadMatcher.replaceAll("a1 b2 c d4", "beep")).getCause();
        assertEquals("error parsing regexp: invalid or unsupported Perl syntax: `(?=`", exception.getMessage());
        assertEquals("beep1 beep2 c beep4", positiveLookaheadMatcher.useJavaUtil().replaceAll("a1 b2 c d4", "beep"));

        // and atomic groups
        RegexMatcher atomicGroupsMatcher = RegexMatcher.get("a(?>bc|b)c");
        exception = (PatternSyntaxException) assertThrows(InvocationTargetException.class,
                () -> atomicGroupsMatcher.anyMatch("abc")).getCause();
        assertEquals("error parsing regexp: invalid or unsupported Perl syntax: `(?>`", exception.getMessage());
        assertFalse(atomicGroupsMatcher.useJavaUtil().anyMatch("abc"));
    }

    @Test
    void findFirstTest() {
        RegexMatcher matcher = RegexMatcher.get(REGEX_TAG);
        String twoEntriesInput = "Text with <i>two</i> <b>tag</b> inside";
        assertEquals("<i>", matcher.findFirst(twoEntriesInput).orElse(null));
        assertEquals("i", matcher.findFirst(twoEntriesInput,
                regexEngine -> regexEngine.group("tagName")).orElse(null));
        assertEquals(Optional.empty(), matcher.findFirst("Text without tags inside"));
    }

    @Test
    void anyAndNoneMatchTest() {
        RegexMatcher matcher = RegexMatcher.get(REGEX_TAG);
        assertTrue(matcher.anyMatch("Text with <br> tags inside"));
        assertFalse(matcher.anyMatch("Text without tags inside"));
        assertFalse(matcher.noneMatch("Text with <br> tags inside"));
        assertTrue(matcher.noneMatch("Text without tags inside"));
    }

    @Test
    void removeAllTest() {
        assertEquals("Text with tags inside </br>", RegexMatcher.get(REGEX_TAG).removeAll("Text with <some><random>tags inside </br>"));
    }

    @Test
    void processEntryTest() {
        List<String> tags = new ArrayList<>();
        RegexMatcher.get(REGEX_TAG).processEntry("Text with <a><few><random> tags inside",
                regexEngine -> tags.add(regexEngine.group("tagName")));
        assertEquals("a", tags.get(0));
        assertEquals("few", tags.get(1));
        assertEquals("random", tags.get(2));
    }

    @Test
    void replaceTest() {
        assertEquals("Text with {two}{random} tags inside plus one more at the {end}", RegexMatcher.get(REGEX_TAG)
                .replace("Text with <two><random> tags inside plus one more at the <end>",
                        regexEngine -> "{%s}".formatted(regexEngine.group("tagName"))));

        // by default quoteReplacement = true
        assertEquals("Text with $100 tag inside", RegexMatcher.get(REGEX_TAG)
                .replace("Text with <br> tag inside",
                        regexEngine -> "$100"));

        assertEquals("Text with br00 tag inside", RegexMatcher.get(REGEX_TAG)
                .replace("Text with <br> tag inside", false,
                        regexEngine -> "$100"));

        assertEquals("Text with 4242 tags inside plus one more at the 42", RegexMatcher.get(REGEX_TAG)
                .replaceAll("Text with <two><random> tags inside plus one more at the <end>", "42"));
    }

}
