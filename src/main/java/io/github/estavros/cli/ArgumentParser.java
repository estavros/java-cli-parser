package io.github.estavros.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** An immutable, reusable parser built from explicitly declared options and flags. */
public final class ArgumentParser {
    private final Map<String, Option> options;

    private record Option(String name, boolean flag) {}

    private ArgumentParser(Map<String, Option> options) {
        this.options = Map.copyOf(options);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses tokens without modifying the input. A standalone {@code --} ends option
     * parsing. Values beginning with a dash must use {@code --name=value}.
     *
     * @throws ArgumentParseException for unknown options, duplicates or missing values
     * @throws NullPointerException if the array or any token is null
     */
    public ParsedArguments parse(String... arguments) {
        Objects.requireNonNull(arguments, "arguments");
        for (String argument : arguments) {
            Objects.requireNonNull(argument, "argument token");
        }
        var values = new HashMap<String, String>();
        var flags = new HashSet<String>();
        var seen = new HashSet<String>();
        var positionals = new ArrayList<String>();
        boolean optionsEnded = false;
        for (int i = 0; i < arguments.length; i++) {
            String token = arguments[i];
            if (optionsEnded || !token.startsWith("-") || token.equals("-")) {
                positionals.add(token);
                continue;
            }
            if (token.equals("--")) {
                optionsEnded = true;
                continue;
            }
            int equals = token.startsWith("--") ? token.indexOf('=') : -1;
            String spelling = equals < 0 ? token : token.substring(0, equals);
            Option option = options.get(spelling);
            if (option == null) {
                throw new ArgumentParseException("Unknown option: " + spelling);
            }
            if (!seen.add(option.name())) {
                throw new ArgumentParseException("Option supplied more than once: --" + option.name());
            }
            if (option.flag()) {
                if (equals >= 0) {
                    throw new ArgumentParseException("Flag does not accept a value: " + spelling);
                }
                flags.add(option.name());
            } else if (equals >= 0) {
                values.put(option.name(), token.substring(equals + 1));
            } else {
                if (i + 1 == arguments.length || arguments[i + 1].startsWith("-")
                        && !arguments[i + 1].equals("-")) {
                    throw new ArgumentParseException("Missing value for " + spelling
                            + "; use --" + option.name() + "=VALUE for values starting with '-'");
                }
                values.put(option.name(), arguments[++i]);
            }
        }
        return new ParsedArguments(values, flags, positionals);
    }

    /** Mutable configuration; built parsers are independent snapshots. */
    public static final class Builder {
        private final Map<String, Option> options = new LinkedHashMap<>();

        private Builder() {}

        public Builder option(String name) {
            return add(name, null, false);
        }

        public Builder option(String name, char shortName) {
            return add(name, shortName, false);
        }

        public Builder flag(String name) {
            return add(name, null, true);
        }

        public Builder flag(String name, char shortName) {
            return add(name, shortName, true);
        }

        private Builder add(String name, Character shortName, boolean flag) {
            if (name == null || !name.matches("[A-Za-z][A-Za-z0-9-]*")) {
                throw new IllegalArgumentException("Long option name must match [A-Za-z][A-Za-z0-9-]*: " + name);
            }
            if (shortName != null && !shortName.toString().matches("[A-Za-z0-9]")) {
                throw new IllegalArgumentException("Short option name must be an ASCII letter or digit: " + shortName);
            }
            String longKey = "--" + name;
            String shortKey = shortName == null ? null : "-" + shortName;
            if (options.containsKey(longKey) || shortKey != null && options.containsKey(shortKey)) {
                throw new IllegalArgumentException("Duplicate option name or alias: " + longKey);
            }
            var option = new Option(name, flag);
            options.put(longKey, option);
            if (shortKey != null) {
                options.put(shortKey, option);
            }
            return this;
        }

        public ArgumentParser build() {
            return new ArgumentParser(options);
        }
    }
}
