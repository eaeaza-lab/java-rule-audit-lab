package lab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes a compact, standalone HTML representation of one completed audit run. */
public final class HtmlAuditReport {
    private HtmlAuditReport() {}

    /** Writes an escaped report with the scenario summary, rule trace, decisions, and violations. */
    public static void write(Path path, Scenario scenario, List<TraceEntry> traces,
            List<Decision> decisions, List<InvariantViolation> violations) throws IOException {
        if (path == null || scenario == null || traces == null || decisions == null || violations == null) {
            throw new IllegalArgumentException("report inputs required");
        }
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<title>Rule audit report</title><style>")
                .append("body{font-family:system-ui,sans-serif;margin:2rem;max-width:72rem}")
                .append("table{border-collapse:collapse;width:100%}th,td{border:1px solid #777;padding:.4rem;text-align:left}")
                .append("code{white-space:pre-wrap}.ok{color:#176b2c}.fail{color:#a01919}")
                .append("</style></head><body><h1>Rule audit report</h1>");
        html.append("<p>Stock lines: ").append(scenario.skus().size()).append("; orders: ")
                .append(scenario.orders().size()).append("; tax rate: ").append(scenario.taxRateBps())
                .append(" bps.</p>");
        html.append("<h2>Invariant violations</h2>");
        if (violations.isEmpty()) {
            html.append("<p class=\"ok\">None.</p>");
        } else {
            html.append("<ul class=\"fail\">");
            for (InvariantViolation violation : violations) {
                html.append("<li><strong>").append(escape(violation.code())).append("</strong>: ")
                        .append(escape(violation.detail())).append("</li>");
            }
            html.append("</ul>");
        }
        html.append("<h2>Decision trace</h2><ol>");
        for (TraceEntry trace : traces) {
            html.append("<li><code>").append(escape(trace.format())).append("</code></li>");
        }
        html.append("</ol><h2>Decisions</h2><table><thead><tr><th>Order</th><th>Allocated units</th>")
                .append("<th>Net cents</th><th>Tax cents</th></tr></thead><tbody>");
        for (Decision decision : decisions) {
            html.append("<tr><td>").append(escape(decision.orderId())).append("</td><td>")
                    .append(decision.allocatedQty()).append("</td><td>").append(decision.netCents())
                    .append("</td><td>").append(decision.taxCents()).append("</td></tr>");
        }
        html.append("</tbody></table></body></html>\n");
        Files.writeString(path, html.toString(), StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
