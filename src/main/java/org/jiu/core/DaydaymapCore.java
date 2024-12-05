package org.jiu.core;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import org.jiu.utils.Utils;

/**
 * @Author Xm17
 * @Date 2024-06-02 21:52
 */
public class DaydaymapCore {

    public static String getData(String qbase64, int page, int size) {
        String apiUrl = Utils.daydaymapUrl + "/api/v1/raymap/search/all";
        String key = Utils.daydaymapKey;
        return HttpRequest.post(apiUrl)
                .header(Header.USER_AGENT, "Hutool http")//头信息，多个头信息多次调用此方法即可
                .header("api-key", key)
                .body("{\"keyword\":\""+qbase64+"\",\"page\":"+page+",\"page_size\":"+size+"}")
                .timeout(20000)//超时，毫秒
                .execute().body();

    }
}
