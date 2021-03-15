package com.example.easymornings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import cz.msebera.android.httpclient.client.config.RequestConfig;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpRequestBase;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;


public class LightConnector {

    final Executor executor;
    final Supplier<String> getIP;
    final static int PORT = 8080;
    RateLimiter<CompletableFuture<Boolean>> rateLimiter = new RateLimiter<>(50, CompletableFuture.completedFuture(true));

    LightConnector(Supplier<String> getIP) {
        executor = Executors.newSingleThreadExecutor();
        this.getIP = getIP;
    }

    enum LightState {UNDEFINED, CONSTANT, FADING, NOT_CONNECTED}

    private LightState parseLightState(String lightState) {
        switch (lightState) {
            case "LIGHT_STATE_CONSTANT":
                return LightState.CONSTANT;
            case "LIGHT_STATE_FADING":
                return LightState.FADING;
            default:
                throw new RuntimeException();
        }
    }

    @Value
    @Builder
    static class LightStatus {
        LightState lightState;
        double lightLevel;
        static LightStatus NotConnected()  {
            return new LightStatus(LightState.NOT_CONNECTED, 0);
        }
    }

    public CompletableFuture<LightStatus> getLightStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = get(statusURI());
                JSONObject json = new JSONObject(response);
                LightState state = parseLightState(json.getString("state"));
                double level = json.getDouble("level");
                return LightStatus.builder().lightState(state).lightLevel(level).build();
            } catch (Exception e) {
                e.printStackTrace();
                return LightStatus.NotConnected();
            }
        }, executor);
    }

    CompletableFuture<Boolean> setNow(float level) {
        return rateLimiter.limit(() ->
                CompletableFuture.supplyAsync(() -> postAndCheck(setNowURI(level)), executor)
        );
    }

    CompletableFuture<Boolean> fade(float level, int period) {
        return rateLimiter.limit(() ->
                CompletableFuture.supplyAsync(() -> postAndCheck(fadeURI(level, period)), executor)
        );
    }

    boolean postAndCheck(URI path) {
        try {
            return checkResponse(post(path));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    String post(URI uri) throws IOException {
        return execute(new HttpPost(uri));
    }

    String get(URI uri) throws IOException {
        return execute(new HttpGet(uri));
    }

    URI statusURI() {
        try {
            return new URIBuilder()
                    .setScheme("http")
                    .setHost(getIP.get())
                    .setPort(PORT)
                    .setPath("/status")
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    URI setNowURI(float level) {
        try {
            return new URIBuilder()
                    .setScheme("http")
                    .setHost(getIP.get())
                    .setPort(PORT)
                    .setPath("/now")
                    .addParameter("level", String.valueOf(level))
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    URI fadeURI(float level, int seconds) {
        try {
            return new URIBuilder()
                    .setScheme("http")
                    .setHost(getIP.get())
                    .setPort(PORT)
                    .setPath("/fade")
                    .addParameter("level", String.valueOf(level))
                    .addParameter("seconds", String.valueOf(seconds))
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    String execute(HttpRequestBase request) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        request.setConfig(RequestConfig.custom().setConnectTimeout(1000).build());
        CloseableHttpResponse response = httpClient.execute(request);
        InputStream content = response.getEntity().getContent();
        byte[] bytes = new byte[content.available()];
        content.read(bytes);
        return new String(bytes);
    }

    boolean checkResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            return json.getBoolean("success");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    @RequiredArgsConstructor
    static class RateLimiter <T> {
        final float delay;
        final T defaultValue;
        long lastOnLevelRequest = 0;
        T limit(Supplier<T> supplier) {
            long now = System.currentTimeMillis();
            if (now - lastOnLevelRequest > delay) {
                lastOnLevelRequest = now;
                return supplier.get();
            } else
                return defaultValue;
        }
    }

}
