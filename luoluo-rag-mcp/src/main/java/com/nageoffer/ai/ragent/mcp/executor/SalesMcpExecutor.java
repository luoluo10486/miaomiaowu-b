package com.nageoffer.ai.ragent.mcp.executor;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class SalesMcpExecutor {

    private static final String TOOL_ID = "sales_query";
    private static final List<String> REGIONS = List.of("华东", "华南", "华北", "西南", "西北");
    private static final List<String> PRODUCTS = List.of("企业版", "专业版", "基础版");
    private static final Map<String, List<String>> SALES_BY_REGION = Map.of(
            "华东", List.of("张三", "李四", "王五"),
            "华南", List.of("赵六", "钱七", "孙八"),
            "华北", List.of("周九", "吴十", "郑冬"),
            "西南", List.of("陈春", "林夏", "黄秋"),
            "西北", List.of("刘一", "杨二", "马三")
    );

    @Bean
    public McpServerFeatures.SyncToolSpecification salesToolSpecification() {
        return new McpServerFeatures.SyncToolSpecification(buildTool(), (exchange, request) -> handleCall(request));
    }

    private Tool buildTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("region", Map.of("type", "string", "description", "地区筛选，不填则查询全部", "enum", REGIONS));
        properties.put("period", Map.of("type", "string", "description", "时间段：本月、上月、本季度、上季度、本年", "enum",
                List.of("本月", "上月", "本季度", "上季度", "本年"), "default", "本月"));
        properties.put("product", Map.of("type", "string", "description", "产品筛选，不填则查询全部", "enum", PRODUCTS));
        properties.put("salesPerson", Map.of("type", "string", "description", "销售姓名，不填则查询全部"));
        properties.put("queryType", Map.of("type", "string", "description", "summary、ranking、detail、trend",
                "enum", List.of("summary", "ranking", "detail", "trend"), "default", "summary"));
        properties.put("limit", Map.of("type", "integer", "description", "返回记录数量限制，默认 10", "default", 10));
        JsonSchema inputSchema = new JsonSchema("object", properties, List.of(), null, null, null);
        return Tool.builder().name(TOOL_ID).description("查询销售数据，支持汇总、排名、明细和趋势分析").inputSchema(inputSchema).build();
    }

    private CallToolResult handleCall(CallToolRequest request) {
        try {
            Map<String, Object> args = request.arguments() == null ? Map.of() : request.arguments();
            String region = stringArg(args, "region");
            String period = defaultIfBlank(stringArg(args, "period"), "本月");
            String product = stringArg(args, "product");
            String salesPerson = stringArg(args, "salesPerson");
            String queryType = defaultIfBlank(stringArg(args, "queryType"), "summary");
            int limit = clamp(intArg(args, "limit"), 10, 1, 50);

            List<SalesRecord> data = filterData(generateData(period), region, product, salesPerson);
            String result = switch (queryType) {
                case "ranking" -> buildRankingResult(data, region, period, limit);
                case "detail" -> buildDetailResult(data, region, period, limit);
                case "trend" -> buildTrendResult(data, region, period);
                default -> buildSummaryResult(data, region, period, product, salesPerson);
            };
            return successResult(result);
        } catch (Exception exception) {
            return errorResult("查询失败: " + messageOf(exception));
        }
    }

    private String buildSummaryResult(List<SalesRecord> data, String region, String period, String product, String salesPerson) {
        double totalAmount = data.stream().mapToDouble(r -> r.amount).sum();
        int orderCount = data.size();
        double avgAmount = orderCount == 0 ? 0 : totalAmount / orderCount;
        Map<String, Double> byProduct = data.stream().collect(Collectors.groupingBy(r -> r.product, Collectors.summingDouble(r -> r.amount)));
        Map<String, Double> byRegion = data.stream().collect(Collectors.groupingBy(r -> r.region, Collectors.summingDouble(r -> r.amount)));

        StringBuilder sb = new StringBuilder();
        sb.append("【").append(period).append(" 销售数据汇总】\n");
        List<String> filters = new ArrayList<>();
        if (region != null) filters.add("地区: " + region);
        if (product != null) filters.add("产品: " + product);
        if (salesPerson != null) filters.add("销售: " + salesPerson);
        if (!filters.isEmpty()) {
            sb.append("筛选条件: ").append(String.join("，", filters)).append("\n\n");
        }
        sb.append("总销售额: ").append(String.format("%.2f", totalAmount)).append("\n");
        sb.append("成交订单数: ").append(orderCount).append("\n");
        sb.append("平均单价: ").append(String.format("%.2f", avgAmount)).append("\n");
        if (product == null && !byProduct.isEmpty()) {
            sb.append("\n【按产品分布】\n");
            byProduct.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .forEach(e -> sb.append(e.getKey()).append(": ").append(String.format("%.2f", e.getValue())).append("\n"));
        }
        if (region == null && !byRegion.isEmpty()) {
            sb.append("\n【按地区分布】\n");
            byRegion.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .forEach(e -> sb.append(e.getKey()).append(": ").append(String.format("%.2f", e.getValue())).append("\n"));
        }
        return sb.toString().trim();
    }

    private String buildRankingResult(List<SalesRecord> data, String region, String period, int limit) {
        Map<String, Double> bySales = data.stream().collect(Collectors.groupingBy(r -> r.salesPerson, Collectors.summingDouble(r -> r.amount)));
        List<Map.Entry<String, Double>> ranking = bySales.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .toList();
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(period);
        if (region != null) {
            sb.append(" ").append(region);
        }
        sb.append(" 销售排名】\n");
        if (ranking.isEmpty()) {
            sb.append("暂无销售数据");
            return sb.toString();
        }
        for (int i = 0; i < ranking.size(); i++) {
            Map.Entry<String, Double> entry = ranking.get(i);
            sb.append("第").append(i + 1).append("名: ").append(entry.getKey()).append(" - ").append(String.format("%.2f", entry.getValue())).append("\n");
        }
        return sb.toString().trim();
    }

    private String buildDetailResult(List<SalesRecord> data, String region, String period, int limit) {
        List<SalesRecord> topRecords = data.stream()
                .sorted((a, b) -> Double.compare(b.amount, a.amount))
                .limit(limit)
                .toList();
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(period);
        if (region != null) {
            sb.append(" ").append(region);
        }
        sb.append(" 销售明细】\n");
        for (int i = 0; i < topRecords.size(); i++) {
            SalesRecord r = topRecords.get(i);
            sb.append(i + 1).append(". ")
                    .append(r.customer).append(" | ")
                    .append(r.product).append(" | ")
                    .append(r.amount).append(" | ")
                    .append(r.salesPerson).append(" | ")
                    .append(r.region).append(" | ")
                    .append(r.date).append("\n");
        }
        return sb.toString().trim();
    }

    private String buildTrendResult(List<SalesRecord> data, String region, String period) {
        Map<String, Double> byWeek = data.stream().collect(Collectors.groupingBy(
                r -> "第" + ((LocalDate.parse(r.date).getDayOfMonth() - 1) / 7 + 1) + "周",
                LinkedHashMap::new,
                Collectors.summingDouble(r -> r.amount)));
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(period);
        if (region != null) {
            sb.append(" ").append(region);
        }
        sb.append(" 销售趋势】\n");
        if (byWeek.isEmpty()) {
            sb.append("暂无数据");
            return sb.toString();
        }
        byWeek.forEach((week, amount) -> sb.append(week).append(": ").append(String.format("%.2f", amount)).append("\n"));
        return sb.toString().trim();
    }

    private List<SalesRecord> filterData(List<SalesRecord> data, String region, String product, String salesPerson) {
        return data.stream()
                .filter(r -> region == null || region.equals(r.region))
                .filter(r -> product == null || product.equals(r.product))
                .filter(r -> salesPerson == null || salesPerson.equals(r.salesPerson))
                .toList();
    }

    private List<SalesRecord> generateData(String period) {
        LocalDate now = LocalDate.now();
        LocalDate start = switch (period) {
            case "上月" -> now.minusMonths(1).withDayOfMonth(1);
            case "本季度" -> currentQuarterStart(now);
            case "上季度" -> previousQuarterStart(now);
            case "本年" -> now.withDayOfYear(1);
            default -> now.withDayOfMonth(1);
        };
        long days = Math.max(1, now.toEpochDay() - start.toEpochDay() + 1);
        Random random = new Random(period.hashCode() * 17L + now.getYear());
        List<SalesRecord> records = new ArrayList<>();
        for (long d = 0; d < days; d++) {
            LocalDate date = start.plusDays(d);
            if (date.isAfter(now) || date.getDayOfWeek().getValue() > 5) {
                continue;
            }
            int ordersPerDay = 3 + random.nextInt(5);
            for (int i = 0; i < ordersPerDay; i++) {
                SalesRecord record = new SalesRecord();
                record.region = REGIONS.get(random.nextInt(REGIONS.size()));
                record.salesPerson = SALES_BY_REGION.get(record.region).get(random.nextInt(SALES_BY_REGION.get(record.region).size()));
                record.product = PRODUCTS.get(random.nextInt(PRODUCTS.size()));
                record.customer = "客户" + (1 + random.nextInt(100));
                record.amount = round(switch (record.product) {
                    case "企业版" -> 500 + random.nextDouble() * 500;
                    case "专业版" -> 100 + random.nextDouble() * 300;
                    default -> 20 + random.nextDouble() * 80;
                });
                record.date = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                records.add(record);
            }
        }
        return records;
    }

    private LocalDate currentQuarterStart(LocalDate now) {
        int month = ((now.getMonthValue() - 1) / 3) * 3 + 1;
        return now.withMonth(month).withDayOfMonth(1);
    }

    private LocalDate previousQuarterStart(LocalDate now) {
        LocalDate currentQuarterStart = currentQuarterStart(now);
        return currentQuarterStart.getMonthValue() == 1
                ? currentQuarterStart.minusYears(1).withMonth(10)
                : currentQuarterStart.minusMonths(3);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String stringArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Integer intArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static int clamp(Integer value, int defaultValue, int min, int max) {
        int actual = value == null ? defaultValue : value;
        return Math.max(min, Math.min(max, actual));
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static CallToolResult successResult(String text) {
        return CallToolResult.builder()
                .content(List.of(new TextContent(text == null ? "" : text)))
                .isError(false)
                .build();
    }

    private static CallToolResult errorResult(String message) {
        return CallToolResult.builder()
                .content(List.of(new TextContent(message == null ? "" : message)))
                .isError(true)
                .build();
    }

    private static String messageOf(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private static final class SalesRecord {
        String region;
        String salesPerson;
        String product;
        String customer;
        double amount;
        String date;
    }
}
