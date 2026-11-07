package org.jiu.core;

import cn.hutool.http.Header;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.jiu.utils.HttpProxyUtil;

/**
 * Shodan InternetDB 查询核心：查询 IP 的开放端口、指纹与漏洞信息，并格式化为展示文本。
 */
public class ShodanCore {

    /**
     * 查询指定 IP 的 Shodan InternetDB 数据，超时 20 秒。
     * 请求或解析失败时不抛异常，返回仅填充 ip 的空结果（各列表为空、rawJson 为 null）。
     */
    public static ShodanResult getData(String ip) {
        String url = "https://internetdb.shodan.io/";
        try {
            String response = HttpProxyUtil.get(url + ip)
                    .header(Header.USER_AGENT, "Hutool http")
                    .timeout(20000)
                    .execute().body();

            JSONObject jsonObj = JSONUtil.parseObj(response);
            ShodanResult result = new ShodanResult();
            result.ip = jsonObj.getStr("ip");
            result.hostnames = jsonObj.getBeanList("hostnames", String.class);
            result.ports = jsonObj.getBeanList("ports", Integer.class);
            result.cpes = jsonObj.getBeanList("cpes", String.class);
            result.tags = jsonObj.getBeanList("tags", String.class);
            result.vulns = jsonObj.getBeanList("vulns", String.class);
            result.rawJson = response;
            return result;
        } catch (Exception e) {
            ShodanResult emptyResult = new ShodanResult();
            emptyResult.ip = ip;
            emptyResult.hostnames = new java.util.ArrayList<>();
            emptyResult.ports = new java.util.ArrayList<>();
            emptyResult.cpes = new java.util.ArrayList<>();
            emptyResult.tags = new java.util.ArrayList<>();
            emptyResult.vulns = new java.util.ArrayList<>();
            return emptyResult;
        }
    }

    /**
     * 将结果格式化为展示文本（不带序号信息）。
     */
    public static String formatResult(ShodanResult result) {
        return formatResult(result, 0, 0);
    }

    /**
     * 将结果格式化为带边框的展示文本，无数据时输出 [No Data] 占位。
     *
     * @param current 当前条目序号，total 大于 0 时显示在头部
     * @param total   条目总数，传 0 表示不显示序号
     */
    public static String formatResult(ShodanResult result, int current, int total) {
        if (result == null || !result.hasData()) {
            String header = (total > 0) ? "\n#" + current + "/" + total + " " : "";
            return header + "[No Data] " + (result != null ? result.ip : "") + "\n\n";
        }

        StringBuilder sb = new StringBuilder();

        if (total > 0) {
            sb.append("\n");
            sb.append("╔════════════════════════════════════════════════════════════╗\n");
            sb.append("║  #").append(current).append(" / ").append(total).append("\n");
        } else {
            sb.append("╔════════════════════════════════════════════════════════════╗\n");
        }

        sb.append("╠════════════════════════════════════════════════════════════╣\n");
        sb.append("║  IP: ").append(result.ip).append("\n");

        if (result.hostnames != null && !result.hostnames.isEmpty()) {
            sb.append("║  Hostnames: ").append(String.join(", ", result.hostnames)).append("\n");
        }

        if (result.tags != null && !result.tags.isEmpty()) {
            sb.append("║  Tags: ").append(String.join(", ", result.tags)).append("\n");
        }

        sb.append("╠════════════════════════════════════════════════════════════╣\n");

        if (result.ports != null && !result.ports.isEmpty()) {
            sb.append("║  Open Ports (").append(result.ports.size()).append("):\n");
            for (Integer port : result.ports) {
                sb.append("║     ").append(result.ip).append(":").append(port).append("\n");
            }
        }

        if (result.cpes != null && !result.cpes.isEmpty()) {
            sb.append("║\n");
            sb.append("║  Products (").append(result.cpes.size()).append("):\n");
            for (String cpe : result.cpes) {
                String simplified = simplifyCpe(cpe);
                sb.append("║     - ").append(simplified).append("\n");
            }
        }

        if (result.vulns != null && !result.vulns.isEmpty()) {
            sb.append("║\n");
            sb.append("║  Vulnerabilities (").append(result.vulns.size()).append("):\n");
            for (String vuln : result.vulns) {
                sb.append("║     - ").append(vuln).append("\n");
            }
        }

        sb.append("╚════════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * 将 CPE 字符串简化为"厂商 产品 版本"，段数不足时原样返回。
     */
    private static String simplifyCpe(String cpe) {
        // 形如 cpe:/a:oracle:mysql:5.7.44，冒号分段后依次取厂商、产品、版本
        String[] parts = cpe.split(":");
        if (parts.length >= 5) {
            String vendor = parts[2];
            String product = parts[3];
            String version = parts[4];
            return vendor + " " + product + " " + version;
        }
        return cpe;
    }

    /**
     * Shodan查询结果对象
     */
    public static class ShodanResult {
        public String ip;
        public java.util.List<String> hostnames;
        public java.util.List<Integer> ports;
        public java.util.List<String> cpes;
        public java.util.List<String> tags;
        public java.util.List<String> vulns;
        public String rawJson;

        /**
         * 以开放端口列表非空作为"有数据"的判据。
         */
        public boolean hasData() {
            return ports != null && !ports.isEmpty();
        }
    }
}
