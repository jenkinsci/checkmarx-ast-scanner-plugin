package com.checkmarx.jenkins.integration;

import com.checkmarx.jenkins.exception.CheckmarxException;
import com.checkmarx.jenkins.tools.ProxyHttpClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class ProxyHttpClientTest extends CheckmarxTestBase {

    private MockWebServer mockWebServer;
    private ProxyHttpClient proxyHttpClient;
    private static final int TIMEOUT = 10000;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        proxyHttpClient = new ProxyHttpClient();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testGetHttpClientWithoutProxy() throws URISyntaxException, CheckmarxException, IOException {
        // Setup mock server
        mockWebServer.enqueue(new MockResponse().setBody("success"));
        
        // Get client without proxy
        OkHttpClient client = proxyHttpClient.getHttpClient(null, TIMEOUT, TIMEOUT);
        
        // Make a test request
        Request request = new Request.Builder()
                .url(mockWebServer.url("/test").toString())
                .build();
        
        Response response = client.newCall(request).execute();
        
        assertTrue(response.isSuccessful());
        assertEquals("success", response.body().string());
        assertNull(client.proxy());
    }

    @Test
    public void testGetHttpClientWithValidProxy() throws URISyntaxException, CheckmarxException, IOException {
        // Setup mock server
        mockWebServer.enqueue(new MockResponse().setBody("success"));
        
        // Create proxy URL with valid port
        String proxyUrl = "http://proxy.example.com:8080";
        
        OkHttpClient client = proxyHttpClient.getHttpClient(proxyUrl, TIMEOUT, TIMEOUT);
        
        assertNotNull(client.proxy());
        InetSocketAddress address = (InetSocketAddress) client.proxy().address();
        assertEquals(8080, address.getPort());
        assertEquals("proxy.example.com", address.getHostString());
    }

    @Test
    public void testGetHttpClientWithProxyAuth() throws URISyntaxException, CheckmarxException {
        // Create proxy URL with auth credentials
        String proxyUrl = "http://user:pass@proxy.example.com:8080";
        
        OkHttpClient client = proxyHttpClient.getHttpClient(proxyUrl, TIMEOUT, TIMEOUT);
        
        assertNotNull(client.proxy());
        assertNotNull(client.proxyAuthenticator());
        InetSocketAddress address = (InetSocketAddress) client.proxy().address();
        assertEquals(8080, address.getPort());
        assertEquals("proxy.example.com", address.getHostString());
    }

    @Test(expected = CheckmarxException.class)
    public void testGetHttpClientWithInvalidProxyPort() throws URISyntaxException, CheckmarxException {
        // Create proxy URL with invalid port
        String proxyUrl = "http://proxy.example.com:5";
        
        proxyHttpClient.getHttpClient(proxyUrl, TIMEOUT, TIMEOUT);
    }

    @Test(expected = CheckmarxException.class)
    public void testGetHttpClientWithTooHighProxyPort() throws URISyntaxException, CheckmarxException {
        // Create proxy URL with port > 65535
        String proxyUrl = "http://proxy.example.com:70000";
        
        proxyHttpClient.getHttpClient(proxyUrl, TIMEOUT, TIMEOUT);
    }

    @Test
    public void testGetHttpClientWithTimeouts() throws URISyntaxException, CheckmarxException {
        int connectTimeout = 5000;
        int readTimeout = 7000;
        
        OkHttpClient client = proxyHttpClient.getHttpClient(null, connectTimeout, readTimeout);
        
        assertEquals(connectTimeout, client.connectTimeoutMillis());
        assertEquals(readTimeout, client.readTimeoutMillis());
    }

    @Test(expected = CheckmarxException.class)
    public void testGetHttpClientWithEmptyProxyHost() throws URISyntaxException, CheckmarxException {
        String proxyUrl = "http://:8080";
        proxyHttpClient.getHttpClient(proxyUrl, TIMEOUT, TIMEOUT);
    }
} 