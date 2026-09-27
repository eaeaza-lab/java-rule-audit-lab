package lab;

import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length == 3 && args[0].equals("run") && args[1].equals("--input")) {
            try {
                Scenario scenario = CsvScenarioReader.read(Path.of(args[2]));
                System.out.println("Loaded scenario: " + scenario.skus().size() + " stock lines, "
                        + scenario.orders().size() + " orders, tax rate " + scenario.taxRateBps() + " bps");
            } catch (CsvInputException exception) {
                System.err.println("Input error: " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        System.out.println("java-rule-audit-lab (work in progress), rule " + TaxRule.VERSION);
    }
}
