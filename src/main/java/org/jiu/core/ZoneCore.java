package org.jiu.core;

import cn.hutool.http.Header;
import org.jiu.utils.HttpProxyUtil;
import org.jiu.utils.Utils;

public class ZoneCore {
    public static String getData(String data,String query_type, int page, int pagesize) {
        String url = Utils.zoneUrl+"/api/data/";
        String key = Utils.zoneKey;
        return HttpProxyUtil.post(url)
                .header(Header.USER_AGENT, "Hutool http")//头信息，多个头信息多次调用此方法即可
                .timeout(20000)//超时，毫秒
                .body("{\"zone_key_id\":\"" + key + "\",\"query\":\"" + data + "\",\"query_type\":\"" + query_type + "\",\"page\":\"" + page + "\",\"pagesize\":\"" + pagesize + "\"}")
                .contentType("application/json")
                .execute().body();
    }
}
