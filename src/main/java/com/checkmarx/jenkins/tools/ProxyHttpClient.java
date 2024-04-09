package com.checkmarx.jenkins.tools;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import okhttp3.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.apache.commons.lang.StringUtils;


public class ProxyHttpClient {

    public OkHttpClient getHttpClient(String proxyString, int connectionTimeoutMillis, int readTimeoutMillis) throws URISyntaxException {
        if (proxyString != null) {
            URI proxyUrl = new URI(proxyString);
            String proxyHost = proxyUrl.getHost();
            int proxyPort = proxyUrl.getPort();
            String proxyUserPass = proxyUrl.getUserInfo();
            if ((!proxyHost.isEmpty()) && (proxyPort >= 10) && (proxyPort <= 65535)) {
                Proxy _httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                if (StringUtils.isNotEmpty(proxyUserPass)) {
                    String[] userPass = proxyUserPass.split(":");
                    Authenticator _httpProxyAuth = new Authenticator() {
                        @Override
                        public Request authenticate(@NotNull Route route, @NotNull Response response)
                                throws IOException {
                            String credential = Credentials.basic(userPass[0], userPass[1]);
                            return response
                                    .request()
                                    .newBuilder()
                                    .header("Proxy-Authorization", credential)
                                    .build();
                        }
                    };
                    return new OkHttpClient.Builder()
                            .proxy(_httpProxy)
                            .proxyAuthenticator(_httpProxyAuth)
                            .connectTimeout(connectionTimeoutMillis, TimeUnit.MILLISECONDS)
                            .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                            .build();
                } else {
                    return new OkHttpClient.Builder()
                            .proxy(_httpProxy)
                            .connectTimeout(connectionTimeoutMillis, TimeUnit.MILLISECONDS)
                            .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                            .build();
                }
            }
        }
        return new OkHttpClient.Builder()
                .connectTimeout(connectionTimeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                .build();
    }
}
