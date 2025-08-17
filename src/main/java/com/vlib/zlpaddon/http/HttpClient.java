package com.vlib.zlpaddon.http;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HttpClient {
    private static final HttpClient INSTANCE = new HttpClient();

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final HttpClientBuilder builder = HttpClientBuilder.create();

    private HttpClient(){
    }

    public static HttpClient getInstance(){
        return INSTANCE;
    }

    public Future<String> sendGet(String url) {
        return executorService.submit(() -> {
            try (CloseableHttpClient client = builder.build()) {
                HttpUriRequest request = new HttpGet(url);

                return client.execute(request, new BasicResponseHandler());
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        });
    }

    public Future<String> sendPost(String url, HttpEntity body) {
        return executorService.submit(() -> {
            try (CloseableHttpClient client = builder.build()) {
                HttpPost request = new HttpPost(url);
                request.setEntity(body);

                return client.execute(request, new BasicResponseHandler());
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        });
    }
}
