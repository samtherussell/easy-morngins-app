package com.example.easymornings.light;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.easymornings.preference.AppPreferenceValues;
import com.example.easymornings.preference.PreferencesConnector;
import com.example.easymornings.preference.SharedPreferencesConnector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    Retryer retryer = new Retryer();

    public static LightConnector Create(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppPreferenceValues.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        PreferencesConnector preferencesConnector = new SharedPreferencesConnector(sharedPreferences);
        return new LightConnector(() -> preferencesConnector.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));
    }

    public LightConnector(Supplier<String> getIP) {
        executor = Executors.newSingleThreadExecutor();
        this.getIP = getIP;
    }

    public enum LightState {UNDEFINED, CONSTANT, FADING, TIMER, NOT_CONNECTED}

    private LightState parseLightState(String lightState) {
        switch (lightState) {
            case "LIGHT_STATE_CONSTANT":
                return LightState.CONSTANT;
            case "LIGHT_STATE_FADING":
                return LightState.FADING;
            case "LIGHT_STATE_TIMER":
                return LightState.TIMER;
            default:
                throw new RuntimeException();
        }
    }

    @Value
    @Builder
    public static class LightStatus {
        LightState lightState;
        double lightLevel;
        int timeLeft;
        static LightStatus NotConnected()  {
            return new LightStatus(LightState.NOT_CONNECTED, 0, -1);
        }
    }

    public CompletableFuture<LightStatus> getLightStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = statusURI();
                Log.println(Log.DEBUG, "request", uri.toASCIIString());
                String response = get(uri);
                Log.println(Log.DEBUG, "response", response);
                JSONObject json = new JSONObject(response);
                LightState state = parseLightState(json.getString("state"));
                double level = json.getDouble("level");
                int timeLeft = (int) json.getDouble("time_left");
                return LightStatus.builder().lightState(state).lightLevel(level).timeLeft(timeLeft).build();
            } catch (Exception e) {
                Log.println(Log.DEBUG, "exception", e.toString());
                return LightStatus.NotConnected();
            }
        }, executor);
    }

    public CompletableFuture<Boolean> setNow(float level) {
        return retryer.retry(() -> rateLimiter.limit(() ->
                CompletableFuture.supplyAsync(() -> postAndCheck(setNowURI(level)), executor)
        ));
    }

    public CompletableFuture<Boolean> fade(float level, int period) {
        return retryer.retry(() -> rateLimiter.limit(() ->
                CompletableFuture.supplyAsync(() -> postAndCheck(fadeURI(level, period)), executor)
        ));
    }

    public CompletableFuture<Boolean> timer(float level, int period) {
        return retryer.retry(() -> rateLimiter.limit(() ->
                CompletableFuture.supplyAsync(() -> postAndCheck(timerURI(level, period)), executor)
        ));
    }

    boolean postAndCheck(URI path) {
        try {
            return checkResponse(post(path));
        } catch (Exception e) {
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
            throw new RuntimeException(e);
        }
    }

    URI timerURI(float level, int seconds) {
        try {
            return new URIBuilder()
                    .setScheme("http")
                    .setHost(getIP.get())
                    .setPort(PORT)
                    .setPath("/timer")
                    .addParameter("level", String.valueOf(level))
                    .addParameter("seconds", String.valueOf(seconds))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    String execute(HttpRequestBase request) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        request.setConfig(RequestConfig.custom().setConnectTimeout(1500).build());
        CloseableHttpResponse response = httpClient.execute(request);
        InputStream content = response.getEntity().getContent();
        InputStreamReader inputStreamReader = new InputStreamReader(content, StandardCharsets.US_ASCII);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        return bufferedReader.lines().collect(Collectors.joining());
    }

    boolean checkResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            return json.getBoolean("success");
        } catch (JSONException e) {
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

    static class Retryer {
        private static final int RETIRES = 5;
        CompletableFuture<Boolean> retry(Supplier<CompletableFuture<Boolean>> supplier) {
            return supplier.get().thenCompose(success -> {
                if (success)
                    return CompletableFuture.completedFuture(success);
                else
                    return retry(supplier, RETIRES - 1);
            });
        }

        CompletableFuture<Boolean> retry(Supplier<CompletableFuture<Boolean>> supplier, int n) {
            return supplier.get().thenCompose(success -> {
                if (success)
                    return CompletableFuture.completedFuture(success);
                else if (n > 1)
                    return retry(supplier, n - 1);
                else
                    return CompletableFuture.completedFuture(false);
            });
        }
    }

}
