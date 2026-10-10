package org.jiu.core;

import cn.hutool.http.Header;
import org.jiu.utils.HttpProxyUtil;
import org.jiu.utils.Utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class FofaCore {

    /** 时间范围下拉框展示文案 -> 配置值 */
    public static final LinkedHashMap<String, String> TIME_RANGES = new LinkedHashMap<>();

    /** 默认时间范围（全部） */
    public static final String TIME_RANGE_ALL = "all";

    /** 匹配语句中自带的 after="..." / before="..." */
    private static final Pattern TIME_CONDITION_PATTERN =
            Pattern.compile("(?i)(after|before)\\s*=\\s*\"");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    static {
        TIME_RANGES.put("全部", TIME_RANGE_ALL);
        TIME_RANGES.put("最近一周", "last_week");
        TIME_RANGES.put("最近一个月", "last_month");
        TIME_RANGES.put("最近半年", "half_year");
        TIME_RANGES.put("最近一年", "last_year");
    }

    /**
     * 获取数据
     *
     * @param data
     * @param fields
     * @param page
     * @param size
     * @param full
     * @return
     */
    public static String getData(String data, String fields, int page, int size, Boolean full) {
        String apiUrl = Utils.fofaUrl + "/api/v1/search/all";
        String key = Utils.fofaKey;
        String url = apiUrl +
                "?key=" + key +
                "&qbase64=" + data +
                "&fields=" + fields +
                "&page=" + page +
                "&size=" + size +
                "&full=" + full;
        return HttpProxyUtil.get(url)
                .header(Header.USER_AGENT, "Hutool http")//头信息，多个头信息多次调用此方法即可
                .timeout(20000)//超时，毫秒
                .execute().body();
    }

    /**
     * 根据配置值获取对应的下拉框展示文案
     */
    public static String getTimeRangeLabel(String value) {
        if (value != null) {
            for (Map.Entry<String, String> entry : TIME_RANGES.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(value.trim())) {
                    return entry.getKey();
                }
            }
        }
        return "全部";
    }

    /**
     * 根据下拉框展示文案获取配置值
     */
    public static String getTimeRangeValue(String label) {
        if (label != null && TIME_RANGES.containsKey(label)) {
            return TIME_RANGES.get(label);
        }
        return TIME_RANGE_ALL;
    }

    /**
     * 归一化配置值，非法或空值一律按"全部"处理
     */
    public static String normalizeTimeRange(String value) {
        if (value == null || value.trim().isEmpty()) {
            return TIME_RANGE_ALL;
        }
        String trimmed = value.trim();
        if (TIME_RANGES.containsValue(trimmed)) {
            return trimmed;
        }
        return TIME_RANGE_ALL;
    }

    /**
     * 计算时间范围的起始日期，全部则返回空字符串
     */
    private static String buildStartDate(String timeRange) {
        LocalDate now = LocalDate.now();
        LocalDate start;
        switch (normalizeTimeRange(timeRange)) {
            case "last_week":
                start = now.minusWeeks(1);
                break;
            case "last_month":
                start = now.minusMonths(1);
                break;
            case "half_year":
                start = now.minusMonths(6);
                break;
            case "last_year":
                start = now.minusYears(1);
                break;
            default:
                return "";
        }
        return start.format(DATE_FORMATTER);
    }

    /**
     * 判断查询语句中是否已自带时间条件
     */
    public static boolean hasTimeCondition(String query) {
        return query != null && TIME_CONDITION_PATTERN.matcher(query).find();
    }

    /**
     * 在查询语句上追加时间范围条件。
     * FOFA API v1 不支持网页端的 filter_type 参数（传了会被忽略），只能通过查询语法 after="YYYY-MM-DD" 实现。
     * 若语句中已自带 after/before，则以语句为准，忽略传入的时间范围。
     *
     * @param query     原始查询语句
     * @param timeRange 时间范围配置值
     * @return 追加后的查询语句
     */
    public static String applyTimeRange(String query, String timeRange) {
        String startDate = buildStartDate(timeRange);
        if (startDate.isEmpty()) {
            return query;
        }
        if (query == null || query.trim().isEmpty()) {
            return "after=\"" + startDate + "\"";
        }
        if (hasTimeCondition(query)) {
            return query;
        }
        // 括号包裹原语句，避免 || 与 && 的优先级问题
        return "(" + query.trim() + ") && after=\"" + startDate + "\"";
    }
}
