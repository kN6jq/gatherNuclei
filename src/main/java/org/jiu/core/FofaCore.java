package org.jiu.core;

import cn.hutool.http.Header;
import org.jiu.utils.HttpProxyUtil;
import org.jiu.utils.Utils;


public class FofaCore {

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
}
