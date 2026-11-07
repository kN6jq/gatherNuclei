package org.jiu.core;

import cn.hutool.http.Header;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.jiu.utils.HttpProxyUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RapidDNS 子域名查询核心类
 * 从 https://rapiddns.io/subdomain/{domain} 爬取子域名数据
 */
public class RapidDnsCore {

    /**
     * 爬取 RapidDNS 页面并解析子域名记录；请求或解析异常时不抛出，
     * 错误信息写入结果的 error 字段。
     */
    public static RapidDnsResult getSubdomains(String domain) {
        RapidDnsResult result = new RapidDnsResult();
        result.domain = domain;

        String url = "https://rapiddns.io/subdomain/" + domain;

        try {
            String html = HttpProxyUtil.get(url)
                    .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header(Header.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header(Header.ACCEPT_LANGUAGE, "en-US,en;q=0.5")
                    .timeout(20000)
                    .execute().body();

            // 从 "Total: <span>数量</span>" 结构中提取页面标注的总数
            String totalPattern = "Total:\\s*<span[^>]*>\\s*(\\d+)\\s*</span>";
            Matcher totalMatcher = Pattern.compile(totalPattern).matcher(html);
            if (totalMatcher.find()) {
                result.total = Integer.parseInt(totalMatcher.group(1).trim());
            }

            // 匹配模式：<tr><th scope="row">1</th><td>domain</td><td><a...>address</a></td><td>type</td><td>date</td></tr>
            String rowPattern = "<tr>\\s*<th[^>]*>\\s*(\\d+)\\s*</th>\\s*<td>([^<]+)</td>\\s*<td>.*?<a[^>]*>([^<]+)</a>.*?</td>\\s*<td>([A-Z]+)</td>\\s*<td>([^<]+)</td>\\s*</tr>";
            Matcher rowMatcher = Pattern.compile(rowPattern, Pattern.DOTALL).matcher(html);

            while (rowMatcher.find()) {
                String index = rowMatcher.group(1);
                String subdomain = rowMatcher.group(2).trim();
                String address = rowMatcher.group(3).trim();
                String type = rowMatcher.group(4);
                String date = rowMatcher.group(5).trim();

                result.records.add(new SubdomainRecord(index, subdomain, address, type, date));
            }

            // 页面结构变化导致表格解析无结果时，退化为逐单元格提取，把域名与其后出现的 IP 配对
            if (result.records.isEmpty() && !html.contains("No results found")) {
                String simplePattern = "<td>([^<]+)</td>";
                Matcher simpleMatcher = Pattern.compile(simplePattern).matcher(html);

                int index = 1;
                String lastDomain = null;
                while (simpleMatcher.find()) {
                    String value = simpleMatcher.group(1).trim();
                    if (value.contains(".") && !value.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        lastDomain = value;
                    } else if (value.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || value.matches("[0-9a-f:]+")) {
                        if (lastDomain != null) {
                            result.records.add(new SubdomainRecord(
                                    String.valueOf(index++),
                                    lastDomain,
                                    value,
                                    value.contains(":") ? "AAAA" : "A",
                                    ""
                            ));
                        }
                    }
                }
            }

            // 以实际解析出的记录数为准，覆盖页面标注的总数
            result.total = result.records.size();

        } catch (Exception e) {
            result.error = "查询失败: " + e.getMessage();
        }

        return result;
    }

    /**
     * 将查询结果序列化为格式化 JSON 字符串。
     */
    public static String toJson(RapidDnsResult result) {
        JSONObject json = new JSONObject();
        json.set("total", result.total);
        json.set("error", result.error);
        json.set("domain", result.domain);

        JSONArray recordsArray = new JSONArray();
        for (SubdomainRecord record : result.records) {
            JSONObject recordJson = new JSONObject();
            recordJson.set("index", record.index);
            recordJson.set("domain", record.domain);
            recordJson.set("address", record.address);
            recordJson.set("type", record.type);
            recordJson.set("date", record.date);
            recordsArray.add(recordJson);
        }
        json.set("records", recordsArray);

        return JSONUtil.toJsonPrettyStr(json);
    }

    /**
     * 子域名查询结果对象
     */
    public static class RapidDnsResult {
        public int total;
        public List<SubdomainRecord> records;
        public String error;
        public String domain;

        public RapidDnsResult() {
            this.records = new ArrayList<>();
        }

        public boolean hasError() {
            return error != null && !error.isEmpty();
        }

        public boolean hasData() {
            return records != null && !records.isEmpty();
        }
    }

    /**
     * 子域名记录
     */
    public static class SubdomainRecord {
        public String index;
        public String domain;
        public String address;
        public String type;
        public String date;

        public SubdomainRecord(String index, String domain, String address, String type, String date) {
            this.index = index;
            this.domain = domain;
            this.address = address;
            this.type = type;
            this.date = date;
        }
    }
}
