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
    public static Set<String> getData(Set<String> ips){
        Set<String> ipports = new HashSet<>();
        ips.forEach(ip->{
            String url = "https://internetdb.shodan.io/";
            JSONArray jsonArray = JSONUtil.parseObj(HttpRequest.get(url + ip)
                    .header(Header.USER_AGENT, "Hutool http")//头信息，多个头信息多次调用此方法即可
                    .timeout(20000)//超时，毫秒
                    .execute().body()).getJSONArray("ports");
            jsonArray.forEach(port->{
                ipports.add(ip+":"+port);
            });
        });
        return ipports;
    }


}
