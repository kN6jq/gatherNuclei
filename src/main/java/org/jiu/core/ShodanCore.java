package org.jiu.core;

import cn.hutool.http.Header;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.jiu.utils.HttpProxyUtil;

/**
 * Shodan InternetDB 查询结果
 */
public class ShodanCore {

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

        public boolean hasData() {
            return ports != null && !ports.isEmpty();
        }
    }

    /**
     * 获取Shodan数据
     * @param ip IP地址
     * @return ShodanResult对象
     */
    public static ShodanResult getData(String ip){
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
     * Format Shodan result for display
     */
    public static String formatResult(ShodanResult result) {
        return formatResult(result, 0, 0);
    }

    /**
     * Format Shodan result with index info
     * @param result Shodan result
     * @param current Current index
     * @param total Total count
     */
    public static String formatResult(ShodanResult result, int current, int total) {
        if (result == null || !result.hasData()) {
            String header = (total > 0) ? "\n#" + current + "/" + total + " " : "";
            return header + "[No Data] " + (result != null ? result.ip : "") + "\n\n";
        }

        StringBuilder sb = new StringBuilder();

        // Header with index
        if (total > 0) {
            sb.append("\n");
            sb.append("╔════════════════════════════════════════════════════════════╗\n");
            sb.append("║  #").append(current).append(" / ").append(total).append("\n");
        } else {
            sb.append("╔════════════════════════════════════════════════════════════╗\n");
        }

        sb.append("╠════════════════════════════════════════════════════════════╣\n");
        sb.append("║  IP: ").append(result.ip).append("\n");

        // Hostnames
        if (result.hostnames != null && !result.hostnames.isEmpty()) {
            sb.append("║  Hostnames: ").append(String.join(", ", result.hostnames)).append("\n");
        }

        // Tags
        if (result.tags != null && !result.tags.isEmpty()) {
            sb.append("║  Tags: ").append(String.join(", ", result.tags)).append("\n");
        }

        sb.append("╠════════════════════════════════════════════════════════════╣\n");

        // IP:Port list
        if (result.ports != null && !result.ports.isEmpty()) {
            sb.append("║  Open Ports (").append(result.ports.size()).append("):\n");
            for (Integer port : result.ports) {
                sb.append("║     ").append(result.ip).append(":").append(port).append("\n");
            }
        }

        // CPE info
        if (result.cpes != null && !result.cpes.isEmpty()) {
            sb.append("║\n");
            sb.append("║  Products (").append(result.cpes.size()).append("):\n");
            for (String cpe : result.cpes) {
                String simplified = simplifyCpe(cpe);
                sb.append("║     - ").append(simplified).append("\n");
            }
        }

        // Vulnerabilities (show all)
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
     * Simplify CPE display
     */
    private static String simplifyCpe(String cpe) {
        // cpe:/a:oracle:mysql:5.7.44
        String[] parts = cpe.split(":");
        if (parts.length >= 5) {
            String vendor = parts[2];
            String product = parts[3];
            String version = parts[4];
            return vendor + " " + product + " " + version;
        }
        return cpe;
    }
}
