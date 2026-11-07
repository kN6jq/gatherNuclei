package org.jiu.core;

import cn.hutool.http.Header;
import org.jiu.utils.HttpProxyUtil;
import org.jiu.utils.Utils;

/**
 * Daydaymap 资产查询核心，封装其搜索接口的分页调用。
 */
public class DaydaymapCore {

    /**
     * 调用 Daydaymap 搜索接口，超时 20 秒，网络异常由 Hutool 直接抛出。
     *
     * @param qbase64 Base64 编码后的查询语句
     * @return 接口原始响应体（JSON 文本）
     */
    public static String getData(String qbase64, int page, int size) {
        String apiUrl = Utils.daydaymapUrl + "/api/v1/raymap/search/all";
        String key = Utils.daydaymapKey;
        return HttpProxyUtil.post(apiUrl)
                .header(Header.USER_AGENT, "Hutool http")
                .header("api-key", key)
                .body("{\"keyword\":\"" + qbase64 + "\",\"page\":" + page + ",\"page_size\":" + size + "}")
                .timeout(20000)
                .execute().body();

    }
}
