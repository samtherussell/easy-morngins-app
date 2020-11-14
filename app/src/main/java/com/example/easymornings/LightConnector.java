package com.example.easymornings;

import android.util.JsonReader;

import cz.msebera.android.httpclient.client.config.RequestConfig;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpRequestBase;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;


public class LightConnector {

    final Executor executor;
    final Supplier<String> getIP;
    final static int PORT = 8080;

    LightConnector(Supplier<String> getIP) {
        executor = Executors.newSingleThreadExecutor();
        this.getIP = getIP;
    }

    enum LightState {UNDEFINED, OFF, FADING_ON, ON, FADING_OFF, TIMED_ON, NOT_CONNECTED}

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
                String response = get(simpleUri("/status"));
                return parseLightState(getStringFromJson(response, "state"));
            } catch (IOException e) {
                e.printStackTrace();
                return LightState.NOT_CONNECTED;
            }
        }, executor);
    }

    CompletableFuture<Boolean> onNow() {
        return CompletableFuture.supplyAsync(() -> postAndCheck(simpleUri("/on")), executor);
    }

    CompletableFuture<Boolean> onTimer(int period) {
        return CompletableFuture.supplyAsync(() -> postAndCheck(uriWithSeconds("/on-timer", period)), executor);
    }

    CompletableFuture<Boolean> fadeOnNow(int period) {
        return CompletableFuture.supplyAsync(() -> postAndCheck(uriWithSeconds("/fade-on", period)), executor);
    }

    CompletableFuture<Boolean> offNow() {
        return CompletableFuture.supplyAsync(() -> postAndCheck(simpleUri("/off")), executor);
    }

    CompletableFuture<Boolean> fadeOffNow(int period) {
        return CompletableFuture.supplyAsync(() -> postAndCheck(uriWithSeconds("/fade-off", period)), executor);
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

    URI simpleUri(String path) {
        try {
            return new URIBuilder()
                    .setScheme("http")
                    .setHost(getIP.get())
                    .setPort(PORT)
                    .setPath(path)
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    URI uriWithSeconds(String path, int seconds) {
        try {
            return new URIBuilder()
                    .setScheme("http")
                    .setHost(getIP.get())
                    .setPort(PORT)
                    .setPath(path)
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
        return getBoolFromJson(response, "success");

    }

    boolean getBoolFromJson(String json, String name) {
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

    String getStringFromJson(String response, String name) {
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

}
