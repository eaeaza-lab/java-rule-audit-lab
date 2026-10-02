package lab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Creates small, repeatable, fictional CSV scenarios for local rule exploration. */
public final class SyntheticScenarioGenerator {
    private static final int SKU_COUNT = 3;
    private static final int ORDER_COUNT = 6;

    private SyntheticScenarioGenerator() {}

    /** Writes a complete CSV scenario. The same seed always produces identical file bytes. */
    public static void generate(long seed, Path outputDir) {
        if (outputDir == null) {
            throw new IllegalArgumentException("output directory required");
        }
        Random random = new Random(seed);
        StringBuilder stock = new StringBuilder("sku_id,warehouse,on_hand,unit_price_cents\n");
        for (int number = 1; number <= SKU_COUNT; number++) {
            stock.append(skuId(number)).append(',')
                    .append("Warehouse-").append((char) ('A' + random.nextInt(2))).append(',')
                    .append(10 + random.nextInt(41)).append(',')
                    .append(100 + random.nextInt(901)).append('\n');
        }
        StringBuilder orders = new StringBuilder("order_id,sku_id,quantity,priority\n");
        for (int number = 1; number <= ORDER_COUNT; number++) {
            orders.append(String.format(java.util.Locale.ROOT, "ORD-%04d", number)).append(',')
                    .append(skuId(1 + random.nextInt(SKU_COUNT))).append(',')
                    .append(1 + random.nextInt(15)).append(',')
                    .append(random.nextInt(4)).append('\n');
        }
        String taxRate = "tax_rate_bps\n" + (250 + random.nextInt(1_751)) + "\n";
        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve("stock.csv"), stock.toString(), StandardCharsets.UTF_8);
            Files.writeString(outputDir.resolve("orders.csv"), orders.toString(), StandardCharsets.UTF_8);
            Files.writeString(outputDir.resolve("tax-rate.csv"), taxRate, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("cannot write generated scenario", exception);
        }
    }

    private static String skuId(int number) {
        return String.format(java.util.Locale.ROOT, "SKU-%04d", number);
    }
}
