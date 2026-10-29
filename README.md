# Java CLI Parser

A small Java 25 library for parsing command-line arguments. No runtime dependencies.

## Usage

```java
import io.github.estavros.cli.ArgumentParser;
import io.github.estavros.cli.ArgumentParseException;

var parser = ArgumentParser.builder()
        .option("name", 'n')
        .option("count", 'c')
        .flag("verbose", 'v')
        .build();

try {
    var parsed = parser.parse("--name=Ada", "-c", "3", "--verbose", "input.txt");
    String name = parsed.value("name").orElse("world");
    boolean verbose = parsed.flag("verbose");
    var files = parsed.positionals(); // [input.txt]
} catch (ArgumentParseException exception) {
    System.err.println(exception.getMessage());
}
```

In a `main(String[] args)` method, pass `args` to `parser.parse(args)`.
Use `.option("name")` or `.flag("verbose")` when no short alias is needed.
Result lookups use long names without dashes; absent values return `Optional.empty()`
and absent flags return `false`. Unknown result lookup names also return empty/false.

Supported forms:

| Input | Meaning |
| --- | --- |
| `--name=Ada` / `--name Ada` | Long option with a value |
| `-n Ada` | Short option with a value |
| `--verbose` / `-v` | Boolean flag |
| `input.txt` | Positional argument |
| `-- --literal -x` | All tokens after `--` are positional |
| `--count=-3` | A value beginning with a dash |
| `--name=` | An explicitly empty value |

## Design decisions

- Options must be declared. Unknown options, missing values, repeated options (including
  aliases), and values attached to flags produce `ArgumentParseException` with a specific
  message. Configuration errors produce `IllegalArgumentException`; null input arrays or
  tokens produce `NullPointerException`.
- Long names match `[A-Za-z][A-Za-z0-9-]*`; short aliases are single ASCII letters or
  digits. Names are case sensitive, and duplicate names or aliases are rejected.
- Separate values starting with a dash are rejected to avoid consuming another option by
  accident. Use `--name=value` for these values. A lone `-` is accepted as a value or
  positional argument. Empty strings are preserved.
- Option parsing continues after positional arguments until `--`. Short option clusters,
  attached short values (`-nAda`), `--verbose=false`, automatic help, required options,
  and type conversion are intentionally outside this minimal API. Applications handle
  defaults, required values, conversion and exit codes.
- Built parsers and results are immutable. A builder can be reused without changing
  existing parsers; parsing stores no shared mutable state, so parsers can be reused
  concurrently. Input arrays are not retained or modified.

## Build and test

Install JDK 25 and run:

```sh
./mvnw clean verify
```

On Windows, use `mvnw.cmd clean verify`. The Maven wrapper downloads Maven 3.9.11
on first use (internet access required). A local Maven installation also works with
`mvn clean verify`. JUnit 5 is the only library dependency and is test scoped.
The JAR is written to `target/java-cli-parser-0.1.0-SNAPSHOT.jar`.

To use from another local Maven project, run `./mvnw install` and add:

```xml
<dependency>
  <groupId>io.github.estavros</groupId>
  <artifactId>java-cli-parser</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

These are local development coordinates; no publishing is configured.

Licensed under the [MIT License](LICENSE).
