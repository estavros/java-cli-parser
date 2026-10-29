package io.github.estavros.cli;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable results, queried by canonical long name (without dashes). */
public final class ParsedArguments {
    private final Map<String, String> values;
    private final Set<String> flags;
    private final List<String> positionals;

    ParsedArguments(Map<String, String> values, Set<String> flags, List<String> positionals) {
        this.values = Map.copyOf(values);
        this.flags = Set.copyOf(flags);
        this.positionals = List.copyOf(positionals);
    }

    /** Returns an option's value, or empty if absent. */
    public Optional<String> value(String name) {
        return Optional.ofNullable(values.get(name));
    }

    /** Returns whether a boolean flag was supplied. */
    public boolean flag(String name) {
        return flags.contains(name);
    }

    /** Returns positional arguments in input order. */
    public List<String> positionals() {
        return positionals;
    }
}
