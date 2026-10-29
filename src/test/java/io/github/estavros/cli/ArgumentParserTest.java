package io.github.estavros.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentParserTest {
    private final ArgumentParser parser = ArgumentParser.builder()
            .option("name", 'n').option("count", 'c').flag("verbose", 'v').build();

    @Test
    void parsesLongShortAndInlineValuesWithFlagsAndPositionals() {
        var result = parser.parse("first", "--name=Ada=Love", "-c", "3", "--verbose", "last");
        assertEquals("Ada=Love", result.value("name").orElseThrow());
        assertEquals("3", result.value("count").orElseThrow());
        assertTrue(result.flag("verbose"));
        assertEquals(List.of("first", "last"), result.positionals());
    }

    @Test
    void parsesSeparatedLongValueAndShortFlag() {
        var result = parser.parse("--name", "Ada Lovelace", "-v");
        assertEquals("Ada Lovelace", result.value("name").orElseThrow());
        assertTrue(result.flag("verbose"));
    }

    @Test
    void emptyInputHasNoValuesOrFlags() {
        var result = parser.parse();
        assertTrue(result.value("name").isEmpty());
        assertFalse(result.flag("verbose"));
        assertTrue(result.positionals().isEmpty());
    }

    @Test
    void terminatorPreservesAllRemainingTokens() {
        assertEquals(List.of("-", "", "--verbose", "--", "-x"),
                parser.parse("-", "", "--", "--verbose", "--", "-x").positionals());
    }

    @Test
    void acceptsEmptyValuesNegativeValuesAndLoneDash() {
        assertEquals("", parser.parse("--name=").value("name").orElseThrow());
        assertEquals("", parser.parse("-n", "").value("name").orElseThrow());
        assertEquals("-3", parser.parse("--count=-3").value("count").orElseThrow());
        assertEquals("-", parser.parse("-n", "-").value("name").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"--unknown", "-x", "-vn", "-nAda", "-n=Ada", "---name", "--=x"})
    void rejectsUnknownOrUnsupportedOptionSpellings(String token) {
        assertEquals("Unknown option: " + (token.equals("--=x") ? "--" : token),
                assertThrows(ArgumentParseException.class, () -> parser.parse(token)).getMessage());
    }

    @Test
    void rejectsMissingValuesWithoutConsumingOptions() {
        for (String[] args : List.of(new String[]{"--name"}, new String[]{"-n", "--verbose"},
                new String[]{"--name", "--"}, new String[]{"--count", "-3"})) {
            assertTrue(assertThrows(ArgumentParseException.class,
                    () -> parser.parse(args)).getMessage().startsWith("Missing value for " + args[0]));
        }
    }

    @Test
    void rejectsFlagValues() {
        assertEquals("Flag does not accept a value: --verbose",
                assertThrows(ArgumentParseException.class,
                        () -> parser.parse("--verbose=false")).getMessage());
    }

    @Test
    void rejectsRepeatedOptionsIncludingAliases() {
        assertEquals("Option supplied more than once: --name",
                assertThrows(ArgumentParseException.class,
                        () -> parser.parse("--name=Ada", "-n", "Grace")).getMessage());
        assertThrows(ArgumentParseException.class, () -> parser.parse("-v", "--verbose"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"--name", "has space", "a=b", "1name", "é"})
    void validatesLongNames(String name) {
        assertThrows(IllegalArgumentException.class, () -> ArgumentParser.builder().option(name));
    }

    @Test
    void validatesAliasesAndRejectsRegistrationCollisionsAtomically() {
        var builder = ArgumentParser.builder().option("name", 'n');
        assertThrows(IllegalArgumentException.class, () -> builder.flag("name"));
        assertThrows(IllegalArgumentException.class, () -> builder.option("other", 'n'));
        assertThrows(IllegalArgumentException.class, () -> builder.flag("bad", '-'));
        builder.flag("other", 'o');
        assertTrue(builder.build().parse("-o").flag("other"));
    }

    @Test
    void parserAndResultsAreIndependentSnapshots() {
        var builder = ArgumentParser.builder().option("name", 'n');
        var first = builder.build();
        builder.flag("verbose");
        assertThrows(ArgumentParseException.class, () -> first.parse("--verbose"));
        var input = new String[]{"-n", "Ada", "file"};
        var result = first.parse(input);
        input[1] = "changed";
        first.parse("--name=Grace");
        assertEquals("Ada", result.value("name").orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> result.positionals().add("x"));
        assertTrue(first.parse().value("name").isEmpty());
    }

    @Test
    void rejectsNullInputTokensEvenAfterTerminator() {
        assertThrows(NullPointerException.class, () -> parser.parse((String[]) null));
        assertThrows(NullPointerException.class, () -> parser.parse("--", null));
    }

    @Test
    void supportsLongOnlyDeclarationsAndCaseSensitiveNames() {
        var longOnly = ArgumentParser.builder().option("output-file").flag("Debug").build();
        assertEquals("out", longOnly.parse("--output-file", "out").value("output-file").orElseThrow());
        assertTrue(longOnly.parse("--Debug").flag("Debug"));
        assertThrows(ArgumentParseException.class, () -> longOnly.parse("--debug"));
    }
}
