package lab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads the three UTF-8 CSV files that make up an offline scenario directory. */
public final class CsvScenarioReader {
    private static final String STOCK_FILE = "stock.csv";
    private static final String ORDERS_FILE = "orders.csv";
    private static final String TAX_RATE_FILE = "tax-rate.csv";

    private CsvScenarioReader() {}

    /**
     * Reads {@code stock.csv}, {@code orders.csv}, and {@code tax-rate.csv} from {@code inputDir}.
     * The reader accepts quoted CSV fields (with doubled quotes for a literal quote).
     */
    public static Scenario read(Path inputDir) {
        if (inputDir == null) {
            throw new IllegalArgumentException("input directory required");
        }
        List<Sku> skus = readStock(inputDir.resolve(STOCK_FILE));
        List<Order> orders = readOrders(inputDir.resolve(ORDERS_FILE));
        int taxRateBps = readTaxRate(inputDir.resolve(TAX_RATE_FILE));
        return new Scenario(skus, orders, taxRateBps);
    }

    private static List<Sku> readStock(Path path) {
        List<List<String>> rows = rows(path, STOCK_FILE, List.of("sku_id", "warehouse", "on_hand", "unit_price_cents"));
        List<Sku> result = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            int line = index + 2;
            requireColumns(STOCK_FILE, line, row, 4);
            try {
                result.add(new Sku(required(STOCK_FILE, line, row.get(0), "sku_id"),
                        required(STOCK_FILE, line, row.get(1), "warehouse"),
                        integer(STOCK_FILE, line, row.get(2), "on_hand"),
                        longInteger(STOCK_FILE, line, row.get(3), "unit_price_cents")));
            } catch (IllegalArgumentException exception) {
                throw modelError(STOCK_FILE, line, exception);
            }
        }
        return result;
    }

    private static List<Order> readOrders(Path path) {
        List<List<String>> rows = rows(path, ORDERS_FILE, List.of("order_id", "sku_id", "quantity", "priority"));
        List<Order> result = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            int line = index + 2;
            requireColumns(ORDERS_FILE, line, row, 4);
            try {
                result.add(new Order(required(ORDERS_FILE, line, row.get(0), "order_id"),
                        required(ORDERS_FILE, line, row.get(1), "sku_id"),
                        integer(ORDERS_FILE, line, row.get(2), "quantity"),
                        integer(ORDERS_FILE, line, row.get(3), "priority")));
            } catch (IllegalArgumentException exception) {
                throw modelError(ORDERS_FILE, line, exception);
            }
        }
        return result;
    }

    private static int readTaxRate(Path path) {
        List<List<String>> rows = rows(path, TAX_RATE_FILE, List.of("tax_rate_bps"));
        if (rows.size() != 1) {
            throw new CsvInputException(TAX_RATE_FILE, "expected exactly one tax rate row");
        }
        List<String> row = rows.get(0);
        requireColumns(TAX_RATE_FILE, 2, row, 1);
        int rate = integer(TAX_RATE_FILE, 2, row.get(0), "tax_rate_bps");
        if (rate < 0) {
            throw new CsvInputException(TAX_RATE_FILE, 2, "tax_rate_bps must not be negative");
        }
        return rate;
    }

    private static List<List<String>> rows(Path path, String source, List<String> expectedHeader) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new CsvInputException(source, "cannot read file");
        }
        if (lines.isEmpty()) {
            throw new CsvInputException(source, "missing header row");
        }
        List<String> header = parse(source, 1, lines.get(0));
        if (!header.equals(expectedHeader)) {
            throw new CsvInputException(source, 1, "expected header " + String.join(",", expectedHeader));
        }
        List<List<String>> parsed = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).isEmpty()) {
                throw new CsvInputException(source, index + 1, "blank rows are not allowed");
            }
            parsed.add(parse(source, index + 1, lines.get(index)));
        }
        return parsed;
    }

    private static List<String> parse(String source, int line, String value) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (quoted) {
                if (character == '"') {
                    if (index + 1 < value.length() && value.charAt(index + 1) == '"') {
                        field.append(character);
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(character);
                }
            } else if (character == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (character == '"' && field.isEmpty()) {
                quoted = true;
            } else {
                field.append(character);
            }
        }
        if (quoted) {
            throw new CsvInputException(source, line, "unterminated quoted field");
        }
        fields.add(field.toString());
        return fields;
    }

    private static void requireColumns(String source, int line, List<String> row, int expected) {
        if (row.size() != expected) {
            throw new CsvInputException(source, line, "expected " + expected + " columns but found " + row.size());
        }
    }

    private static String required(String source, int line, String value, String column) {
        if (value.isEmpty()) {
            throw new CsvInputException(source, line, column + " is required");
        }
        return value;
    }

    private static int integer(String source, int line, String value, String column) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CsvInputException(source, line, "invalid integer for " + column);
        }
    }

    private static long longInteger(String source, int line, String value, String column) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new CsvInputException(source, line, "invalid integer for " + column);
        }
    }

    private static CsvInputException modelError(String source, int line, IllegalArgumentException exception) {
        if (exception instanceof CsvInputException) {
            return (CsvInputException) exception;
        }
        return new CsvInputException(source, line, exception.getMessage());
    }
}
