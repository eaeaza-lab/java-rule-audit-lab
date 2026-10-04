package lab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads the deliberately small JSON-like configuration format used by the offline CLI. */
public final class RuleConfigReader {
    private RuleConfigReader() {}

    public static RuleConfig read(Path path) {
        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            Parser parser = new Parser(source);
            Map<String, Object> root = parser.object();
            if (parser.hasTrailingContent()) {
                throw invalid(path, "unexpected trailing content");
            }
            return new RuleConfig(version(root, "tax", path), version(root, "inventory-reservation", path),
                    version(root, "allocation", path), rate(root, path));
        } catch (IOException exception) {
            throw new IllegalArgumentException("cannot read rule config " + path, exception);
        } catch (ParseException exception) {
            throw invalid(path, exception.getMessage());
        }
    }

    private static String version(Map<String, Object> root, String ruleId, Path path) {
        Object value = root.get(ruleId);
        if (!(value instanceof Map<?, ?> rawRule)) {
            throw invalid(path, "missing object for " + ruleId);
        }
        Object version = rawRule.get("version");
        if (!(version instanceof String text) || text.isBlank()) {
            throw invalid(path, "missing string " + ruleId + ".version");
        }
        return text;
    }

    private static int rate(Map<String, Object> root, Path path) {
        Object tax = root.get("tax");
        if (!(tax instanceof Map<?, ?> rawTax)) {
            throw invalid(path, "missing object for tax");
        }
        Object rate = rawTax.get("rateBps");
        if (!(rate instanceof Integer value) || value < 0) {
            throw invalid(path, "missing non-negative integer tax.rateBps");
        }
        return value;
    }

    private static IllegalArgumentException invalid(Path path, String detail) {
        return new IllegalArgumentException("invalid rule config " + path + ": " + detail);
    }

    private static final class Parser {
        private final String source;
        private int index;

        Parser(String source) {
            this.source = source;
        }

        Map<String, Object> object() {
            skipSpace();
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipSpace();
            if (consume('}')) {
                return result;
            }
            do {
                String key = string();
                skipSpace();
                expect(':');
                Object previous = result.put(key, value());
                if (previous != null) {
                    throw new ParseException("duplicate key " + key);
                }
                skipSpace();
            } while (consume(','));
            expect('}');
            return result;
        }

        boolean hasTrailingContent() {
            skipSpace();
            return index < source.length();
        }

        private Object value() {
            skipSpace();
            if (peek('{')) {
                return object();
            }
            if (peek('"')) {
                return string();
            }
            int start = index;
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw new ParseException("expected object, string, or integer");
            }
            try {
                return Integer.valueOf(source.substring(start, index));
            } catch (NumberFormatException exception) {
                throw new ParseException("integer is out of range");
            }
        }

        private String string() {
            skipSpace();
            expect('"');
            int start = index;
            while (index < source.length() && source.charAt(index) != '"') {
                char character = source.charAt(index++);
                if (character == '\\' || character < ' ') {
                    throw new ParseException("unsupported character in string");
                }
            }
            if (index == source.length()) {
                throw new ParseException("unterminated string");
            }
            String value = source.substring(start, index);
            index++;
            return value;
        }

        private void expect(char expected) {
            skipSpace();
            if (!consume(expected)) {
                throw new ParseException("expected '" + expected + "'");
            }
        }

        private boolean consume(char expected) {
            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private boolean peek(char expected) {
            return index < source.length() && source.charAt(index) == expected;
        }

        private void skipSpace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }
    }

    private static final class ParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ParseException(String message) {
            super(message);
        }
    }
}
