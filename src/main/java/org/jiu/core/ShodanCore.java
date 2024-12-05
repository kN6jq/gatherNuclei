package org.jiu.core;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ShodanCore {
    public static Set<String> getData(String ip){
        String url = "https://internetdb.shodan.io/";
        JSONArray jsonArray = JSONUtil.parseObj(HttpRequest.get(url + ip)
                .header(Header.USER_AGENT, "Hutool http")//头信息，多个头信息多次调用此方法即可
                .timeout(20000)//超时，毫秒
                .execute().body()).getJSONArray("ports");
        Set<String> ipports = new HashSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            ipports.add(ip+":"+jsonArray.get(i));
        }
        return ipports;
    }


}
