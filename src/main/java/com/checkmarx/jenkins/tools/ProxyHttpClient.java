package com.checkmarx.jenkins.tools;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import com.checkmarx.jenkins.exception.CheckmarxException;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;


public class ProxyHttpClient {
    private static SecretKey SECRET_KEY;

    public ProxyHttpClient() throws CheckmarxException {
        try {
            SECRET_KEY = EncryptionUtil.generateKey();
        } catch (Exception e) {
            throw new CheckmarxException("Failed to generate encryption key");
        }
    }


    public OkHttpClient getHttpClient(String proxyString, int connectionTimeoutMillis, int readTimeoutMillis) throws Exception {
        OkHttpClient.Builder okClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(connectionTimeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS);
        if (proxyString != null) {
            URI proxy = new URI(proxyString);
            if (!"https".equalsIgnoreCase(proxy.getScheme())) {
                throw new CheckmarxException("Proxy URL must use HTTPS");
            }
            if (isValidProxy(proxy.getHost(), proxy.getPort())) {
                Proxy _httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort()));
//                String proxyUser = System.getenv("PROXY_USER");
//                String proxyPass = System.getenv("PROXY_PASS");
//                if (StringUtils.isNotEmpty(proxyUser) && StringUtils.isNotEmpty(proxyPass)) {
                if (StringUtils.isNotEmpty(proxy.getUserInfo())) {
                    String[] userPass = proxy.getUserInfo().split(":");
                    String encryptedUser = EncryptionUtil.encrypt(userPass[0], SECRET_KEY);
                    String encryptedPass = EncryptionUtil.encrypt(userPass[1], SECRET_KEY);
                    Authenticator _httpProxyAuth = new Authenticator() {
                        @Nullable
                        @Override
                        public Request authenticate(Route route, Response response) throws IOException {
                            try {
                                String decryptedUser = EncryptionUtil.decrypt(encryptedUser, SECRET_KEY);
                                String decryptedPass = EncryptionUtil.decrypt(encryptedPass, SECRET_KEY);
                                String credential = Credentials.basic(decryptedUser, decryptedPass);
                                return response.request().newBuilder()
                                        .header("Proxy-Authorization", credential).build();
                            } catch (Exception e) {
                                throw new IOException("Failed to decrypt credentials", e);
                            }
                        }
                    };
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
