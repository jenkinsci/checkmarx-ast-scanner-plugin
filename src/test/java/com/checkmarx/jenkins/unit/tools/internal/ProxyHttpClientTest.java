package com.checkmarx.jenkins.unit.tools.internal;

import com.checkmarx.jenkins.exception.CheckmarxException;
import com.checkmarx.jenkins.tools.ProxyHttpClient;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class ProxyHttpClientTest {

    private ProxyHttpClient proxyHttpClient;

    @BeforeEach
    public void setUp() {
        proxyHttpClient = new ProxyHttpClient();
    }

    @Test
    public void testGetHttpClientWithoutProxy() throws URISyntaxException, CheckmarxException {
        OkHttpClient client = proxyHttpClient.getHttpClient(null, 1000, 1000);
        assertNotNull(client);
        assertNull(client.proxy());
    }

    @Test
    public void testGetHttpClientWithValidProxy() throws URISyntaxException, CheckmarxException {
        OkHttpClient client = proxyHttpClient.getHttpClient("http://proxy.example.com:8080", 1000, 1000);
        assertNotNull(client);
        assertNotNull(client.proxy());
    }

    @Test
    public void testGetHttpClientWithProxyAuth() throws URISyntaxException, CheckmarxException {
        OkHttpClient client = proxyHttpClient.getHttpClient("http://user:pass@proxy.example.com:8080", 1000, 1000);
        assertNotNull(client);
        assertNotNull(client.proxy());
        assertNotNull(client.proxyAuthenticator());
    }

    @Test
    public void testGetHttpClientWithInvalidProxyPort() {
        assertThrows(CheckmarxException.class,
                () -> proxyHttpClient.getHttpClient("http://proxy.example.com:5", 1000, 1000));
    }

    @Test
    public void testGetHttpClientWithTooHighProxyPort() {
        assertThrows(CheckmarxException.class,
                () -> proxyHttpClient.getHttpClient("http://proxy.example.com:65536", 1000, 1000));
    }

    @Test
    public void testGetHttpClientWithInvalidProxyUrl() {
        assertThrows(Exception.class,
                () -> proxyHttpClient.getHttpClient("not-a-valid-url", 1000, 1000));
    }

    @Test
    public void testGetHttpClientWithMinimumValidPort() throws URISyntaxException, CheckmarxException {
        OkHttpClient client = proxyHttpClient.getHttpClient("http://proxy.example.com:10", 1000, 1000);
        assertNotNull(client);
        assertNotNull(client.proxy());
    }

    @Test
    public void testGetHttpClientWithMaximumValidPort() throws URISyntaxException, CheckmarxException {
        OkHttpClient client = proxyHttpClient.getHttpClient("http://proxy.example.com:65535", 1000, 1000);
        assertNotNull(client);
        assertNotNull(client.proxy());
    }

    @Test
    public void testGetHttpClientTimeouts() throws URISyntaxException, CheckmarxException {
        int connectTimeout = 5000;
        int readTimeout = 3000;
        OkHttpClient client = proxyHttpClient.getHttpClient(null, connectTimeout, readTimeout);
        
        assertEquals(connectTimeout, client.connectTimeoutMillis());
        assertEquals(readTimeout, client.readTimeoutMillis());
    }

    @Test
    public void testGetHttpClientWithEmptyProxyHost() {
        assertThrows(CheckmarxException.class,
                () -> proxyHttpClient.getHttpClient("http://:8080", 1000, 1000));
    }

    @Test
    public void testGetHttpClientWithMalformedProxyUrl() {
        assertThrows(Exception.class,
                () -> proxyHttpClient.getHttpClient("http://proxy.example.com:80:80/invalid-///path", 1000, 1000));
    }
}