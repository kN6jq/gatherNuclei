package org.jiu.core;

import cn.hutool.http.Header;
import org.jiu.utils.HttpProxyUtil;
import org.jiu.utils.Utils;

/**
 * Zone 资产查询核心，以 POST 方式调用其数据查询接口。
 */
public class ZoneCore {
    /**
     * 调用 Zone 数据查询接口，超时 20 秒，网络异常由 Hutool 直接抛出。
     *
     * @param query_type 查询类型，取值 site 或 apk
     * @return 接口原始响应体（JSON 文本）
     */
    public static String getData(String data, String query_type, int page, int pagesize) {
        String url = Utils.zoneUrl + "/api/data/";
        String key = Utils.zoneKey;
        return HttpProxyUtil.post(url)
                .header(Header.USER_AGENT, "Hutool http")
                .timeout(20000)
                .body("{\"zone_key_id\":\"" + key + "\",\"query\":\"" + data + "\",\"query_type\":\"" + query_type + "\",\"page\":\"" + page + "\",\"pagesize\":\"" + pagesize + "\"}")
                .contentType("application/json")
                .execute().body();
    }
}
