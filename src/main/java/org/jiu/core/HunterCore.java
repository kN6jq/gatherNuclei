package org.jiu.core;

import cn.hutool.http.Header;
import org.jiu.utils.HttpProxyUtil;
import org.jiu.utils.Utils;

/**
 * Hunter 资产查询核心，封装其开放搜索接口的分页调用。
 */
public class HunterCore {
    /**
     * 调用 Hunter 搜索接口，超时 20 秒，网络异常由 Hutool 直接抛出。
     *
     * @param data       URL 安全的 Base64 编码查询语句（无填充符）
     * @param type       资产类型：1 web 资产，2 非 web 资产，3 全部
     * @param start_time 查询起始日期，格式 yyyy-MM-dd
     * @param end_time   查询结束日期，格式 yyyy-MM-dd
     * @return 接口原始响应体（JSON 文本）
     */
    public static String getData(String data, int page, int size, int type, String start_time, String end_time) {
        String apiUrl = Utils.hunterUrl + "/openApi/search";
        String key = Utils.hunterKey;
        String url = apiUrl +
                "?api-key=" + key +
                "&search=" + data +
                "&page=" + page +
                "&page_size=" + size +
                "&is_web=" + type +        
                "&start_time=" + start_time +
                "&end_time=" + end_time;
        return HttpProxyUtil.get(url)
                .header(Header.USER_AGENT, "Hutool http")
                .timeout(20000)
                .execute().body();
    }
}
