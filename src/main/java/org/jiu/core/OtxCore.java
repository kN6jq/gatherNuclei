package org.jiu.core;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;

public class OtxCore {
    public static String getData(String domain, int page, int size) {
        String url = "https://otx.alienvault.com/api/v1/indicators/domain/" + domain + "/url_list?limit=" + size + "&page=" + page + "";
        return HttpRequest.get(url)
                .header(Header.USER_AGENT, "Hutool http")//头信息，多个头信息多次调用此方法即可
                .timeout(20000)//超时，毫秒
                .execute().body();

    }

}
