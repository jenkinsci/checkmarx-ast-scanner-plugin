package com.checkmarx.jenkins.tools;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import com.checkmarx.jenkins.exception.CheckmarxException;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;


public class ProxyHttpClient {

    public OkHttpClient getHttpClient(String proxyString, int connectionTimeoutMillis, int readTimeoutMillis) throws URISyntaxException, CheckmarxException {
        OkHttpClient.Builder okClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(connectionTimeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS);
        if (proxyString != null) {
            URI proxy = new URI(proxyString);
            if (isValidProxy(proxy.getHost(), proxy.getPort())) {
                Proxy _httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort()));
                String proxyUserInfo = proxy.getUserInfo();
                if (StringUtils.isNotEmpty(proxyUserInfo)) {
                    String basicAuth = new String(
                            Base64.getEncoder() // get the base64 encoder
                                    .encode(proxyUserInfo.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
                    Authenticator _httpProxyAuth = new Authenticator() {
                        @Nullable
                        @Override
                        public Request authenticate(Route route, Response response) throws IOException {
                            return response.request().newBuilder()
                                    .addHeader("Proxy-Authorization", "Basic " + basicAuth) // add auth
                                    .build();
                        }
                    } ;
                    return okClientBuilder.proxyAuthenticator(_httpProxyAuth).proxy(_httpProxy).build();
                } else {
                    return okClientBuilder.proxy(_httpProxy).build();
                }
            } else {
                throw new CheckmarxException("Invalid proxy configuration");
            }
        }
        return okClientBuilder.build();
    }

    private static boolean isValidProxy(String proxyHost, int proxyPort) {
        return StringUtils.isNotEmpty(proxyHost) && (proxyPort >= 10) && (proxyPort <= 65535);
    }
}
