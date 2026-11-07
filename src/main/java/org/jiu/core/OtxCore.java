package org.jiu.core;

import cn.hutool.http.Header;
import org.jiu.utils.HttpProxyUtil;

/**
 * AlienVault OTX 威胁情报查询核心，获取域名关联的 URL 列表。
 */
public class OtxCore {
    /**
     * 查询指定域名在 OTX 上关联的 URL 列表，超时 20 秒，网络异常由 Hutool 直接抛出。
     *
     * @return 接口原始响应体（JSON 文本）
     */
    public static String getData(String domain, int page, int size) {
        String url = "https://otx.alienvault.com/api/v1/indicators/domain/" + domain + "/url_list?limit=" + size + "&page=" + page + "";
        return HttpProxyUtil.get(url)
                .header(Header.USER_AGENT, "Hutool http")
                .timeout(20000)
                .execute().body();

    }

}
