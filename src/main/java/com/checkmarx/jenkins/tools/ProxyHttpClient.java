package com.checkmarx.jenkins.tools;


import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import okhttp3.*;
import org.apache.commons.lang.StringUtils;


public class ProxyHttpClient {

    public OkHttpClient getHttpClient(String proxyString, int connectionTimeoutMillis, int readTimeoutMillis) throws URISyntaxException {
        OkHttpClient.Builder okClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(connectionTimeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS);
        if (proxyString != null) {
            URI proxy = new URI(proxyString);
            if (isValidProxy(proxy.getHost(), proxy.getPort())) {
                Proxy _httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort()));
                if (StringUtils.isNotEmpty(proxy.getUserInfo())) {
                    String[] userPass = proxy.getUserInfo().split(":");
                    Authenticator _httpProxyAuth = (route, response) -> {
                        String credential = Credentials.basic(userPass[0], userPass[1]);
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential).build();
                    };
                    return okClientBuilder.proxyAuthenticator(_httpProxyAuth).proxy(_httpProxy).build();
                } else {
                    return okClientBuilder.proxy(_httpProxy).build();
                }
            }
        }
        return okClientBuilder.build();
    }

    private static boolean isValidProxy(String proxyHost, int proxyPort) {
        return (!proxyHost.isEmpty()) && (proxyPort >= 10) && (proxyPort <= 65535);
    }
}
