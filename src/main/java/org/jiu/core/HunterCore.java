package org.jiu.core;

import cn.hutool.http.Header;
import org.jiu.utils.HttpProxyUtil;
import org.jiu.utils.Utils;

public class HunterCore {
    public static String getData(String data, int page, int size, int type, String start_time, String end_time) {
        String apiUrl = Utils.hunterUrl + "/openApi/search";
        String key = Utils.hunterKey;
        String url = apiUrl +
                "?api-key=" + key +
                "&search=" + data +
                "&page=" + page +
                "&page_size=" + size +
                "&is_web=" + type +        // 资产类型，1代表"web资产"，2代表"非web资产"，3代表"全部"
                "&start_time=" + start_time +
                "&end_time=" + end_time;
        return HttpProxyUtil.get(url)
                .header(Header.USER_AGENT, "Hutool http")//头信息，多个头信息多次调用此方法即可
                .timeout(20000)//超时，毫秒
                .execute().body();
    }
}
