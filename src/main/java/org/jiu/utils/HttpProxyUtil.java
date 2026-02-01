package org.jiu.utils;

import cn.hutool.http.HttpRequest;
import org.jiu.ui.dialog.ProxyConfigDialog;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;

/**
 * HTTP代理工具类
 * 用于统一处理HttpRequest的代理设置
 */
public class HttpProxyUtil {

    /**
     * 为HttpRequest应用代理配置
     *
     * @param request HttpRequest对象
     * @return 应用代理后的HttpRequest对象
     */
    public static HttpRequest applyProxy(HttpRequest request) {
        if (!ProxyConfigDialog.isProxyEnabled()) {
            return request;
        }

        Properties props = ProxyConfigDialog.getProxyConfig();
        String type = props.getProperty("proxy.type", "HTTP");
        String host = props.getProperty("proxy.host", "");
        String portStr = props.getProperty("proxy.port", "0");
        String username = props.getProperty("proxy.username", "");
        String password = props.getProperty("proxy.password", "");

        if (host.isEmpty() || portStr.isEmpty()) {
            return request;
        }

        try {
            int port = Integer.parseInt(portStr);
            Proxy proxy;

            if ("SOCKS5".equals(type)) {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
            } else {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            }

            // 设置代理
            request.setProxy(proxy);

            // 如果有认证信息，设置代理认证
            if (!username.isEmpty()) {
                request.basicProxyAuth(username, password);
            }

        } catch (NumberFormatException e) {
            System.err.println("代理端口格式错误: " + portStr);
        }

        return request;
    }

    /**
     * 创建并配置代理的HttpRequest GET请求
     *
     * @param url 请求URL
     * @return 配置好代理的HttpRequest对象
     */
    public static HttpRequest get(String url) {
        HttpRequest request = HttpRequest.get(url);
        return applyProxy(request);
    }

    /**
     * 创建并配置代理的HttpRequest POST请求
     *
     * @param url 请求URL
     * @return 配置好代理的HttpRequest对象
     */
    public static HttpRequest post(String url) {
        HttpRequest request = HttpRequest.post(url);
        return applyProxy(request);
    }
}
