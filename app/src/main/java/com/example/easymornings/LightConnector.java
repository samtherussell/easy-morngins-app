package com.example.easymornings;

import android.util.JsonReader;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class LightConnector {

    final Executor executor;
    final String ip;
    final int port;

    LightConnector(String ip, int port) {
        executor = Executors.newSingleThreadExecutor();
        this.ip = ip;
        this.port = port;
    }

    enum LightState {OFF, FADING_ON, ON, FADING_OFF, TIMED_ON}

    private LightState parseLightState(String lightState) {
        switch (lightState) {
            case "LIGHT_STATE_ON_INDEFINITELY":
                return LightState.ON;
            case "LIGHT_STATE_OFF_INDEFINITELY":
                return LightState.OFF;
            case "LIGHT_STATE_FADING_ON":
                return LightState.FADING_ON;
            case "LIGHT_STATE_FADING_OFF":
                return LightState.FADING_OFF;
            case "LIGHT_STATE_ON_TIMED":
                return LightState.TIMED_ON;
            default:
                throw new RuntimeException();
        }
    }

    public CompletableFuture<LightState> getLightState() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = get(simpleUri("status"));
                return parseLightState(getString(response, "state"));
            } catch (IOException e) {
                e.printStackTrace();
                return LightState.OFF;
            }
        }, executor);
    }

    void onNow() {
        CompletableFuture.runAsync(() -> postAndCheck(simpleUri("on")), executor);
    }

    void onTimer(int period) {
        CompletableFuture.runAsync(() -> postAndCheck(uriWithSeconds("on-timer", period)), executor);
    }

    void fadeOnNow(int period) {
        CompletableFuture.runAsync(() -> postAndCheck(uriWithSeconds("fade-on", period)), executor);
    }

    void offNow() {
        CompletableFuture.runAsync(() -> postAndCheck(simpleUri("off")), executor);
    }

    void fadeOffNow(int period) {
        CompletableFuture.runAsync(() -> postAndCheck(uriWithSeconds("fade-off", period)), executor);
    }

    boolean postAndCheck(String path) {
        try {
            return checkResponse(post(path));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    String post(String uri) throws IOException {
        return execute(new HttpPost(uri));
    }

    String get(String uri) throws IOException {
        return execute(new HttpGet(uri));
    }

    String simpleUri(String path) {
        return String.format("http://%s:%d/%s", ip, port, path);
    }

    String uriWithSeconds(String path, int seconds) {
        return String.format("%s?seconds=%d", simpleUri(path), seconds);
    }

    String execute(HttpUriRequest request) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(request);
        InputStream content = response.getEntity().getContent();
        byte[] bytes = new byte[content.available()];
        content.read(bytes);
        return new String(bytes);
    }

    boolean checkResponse(String response) {
        return getBool(response, "success");

    }

    boolean getBool(String json, String name) {
        JsonReader jsonReader = new JsonReader(new StringReader(json));
        try {
            jsonReader.beginObject();
            while (jsonReader.hasNext())
                if (jsonReader.nextName().equals(name))
                    return jsonReader.nextBoolean();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    String getString(String response, String name) {
        JsonReader jsonReader = new JsonReader(new StringReader(response));
        try {
            jsonReader.beginObject();
            while (jsonReader.hasNext())
                if (jsonReader.nextName().equals(name))
                    return jsonReader.nextString();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static long now() {
        return System.currentTimeMillis()/1000;
    }

}
