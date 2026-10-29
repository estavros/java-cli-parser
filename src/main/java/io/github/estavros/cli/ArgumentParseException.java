package io.github.estavros.cli;

/** Invalid command-line input, suitable for displaying to the caller's user. */
public final class ArgumentParseException extends IllegalArgumentException {
    public ArgumentParseException(String message) {
        super(message);
    }
}
