package com.checkmarx.jenkins.tools;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import com.checkmarx.jenkins.exception.CheckmarxException;
import okhttp3.*;
import okio.ByteString;
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
                if (StringUtils.isNotEmpty(proxy.getUserInfo())) {
                    Authenticator _httpProxyAuth = new Authenticator() {
                        @Nullable
                        @Override
                        public Request authenticate(Route route, Response response) throws IOException {
                            byte[] bytes = proxy.getUserInfo().getBytes("ISO-8859-1");
                            String encoded = ByteString.of(bytes).base64();
                            String credential = "Basic " + encoded;
                            return response.request().newBuilder()
                                    .header("Proxy-Authorization", credential).build();
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
