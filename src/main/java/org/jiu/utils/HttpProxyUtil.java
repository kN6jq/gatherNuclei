package org.jiu.utils;

import cn.hutool.http.HttpRequest;
import org.jiu.ui.dialog.ProxyConfigDialog;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;

/**
 * HTTP 代理工具类，为 Hutool HttpRequest 统一应用代理配置。
 * 对外提供 get/post 请求创建与 applyProxy 代理应用。
 */
public class HttpProxyUtil {

    /**
     * 为请求应用代理配置：代理未启用或主机/端口为空时原样返回；
     * 端口格式非法时仅打印错误，返回未设置代理的请求。
     *
     * @return 传入的请求对象本身（便于链式调用）
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

            request.setProxy(proxy);

            if (!username.isEmpty()) {
                request.basicProxyAuth(username, password);
            }

        } catch (NumberFormatException e) {
            System.err.println("代理端口格式错误: " + portStr);
        }

        return request;
    }

    /**
     * 创建已应用代理配置的 GET 请求。
     */
    public static HttpRequest get(String url) {
        HttpRequest request = HttpRequest.get(url);
        return applyProxy(request);
    }

    /**
     * 创建已应用代理配置的 POST 请求。
     */
    public static HttpRequest post(String url) {
        HttpRequest request = HttpRequest.post(url);
        return applyProxy(request);
    }
}
