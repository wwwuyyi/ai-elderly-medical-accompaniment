package com.example.escort.common;

import com.example.escort.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;

@Slf4j @Component
public class AiHttpClient {
    private final OkHttpClient ocrClient;
    private final OkHttpClient llmClient;
    private final AiProperties props;

    public AiHttpClient(AiProperties props) {
        this.props = props;
        this.ocrClient = build(props.getConnectTimeoutMs(), props.getOcr().getReadTimeoutMs());
        this.llmClient = build(props.getConnectTimeoutMs(), props.getLlm().getReadTimeoutMs());
    }
    private OkHttpClient build(int c, int r) {
        return new OkHttpClient.Builder().connectTimeout(Duration.ofMillis(c)).readTimeout(Duration.ofMillis(r)).build();
    }
    public String ocrPost(String path, String json) throws IOException {
        return doPost(ocrClient, props.getOcr().getBaseUrl() + path, json, props.getOcr().getApiKey());
    }
    public String llmPost(String path, String json) throws IOException {
        return doPost(llmClient, props.getLlm().getBaseUrl() + path, json, props.getLlm().getApiKey());
    }
    private String doPost(OkHttpClient cl, String url, String json, String key) throws IOException {
        RequestBody b = RequestBody.create(json, MediaType.parse("application/json"));
        Request req = new Request.Builder().url(url).post(b).header("Authorization", "Bearer " + key).build();
        try (Response r = cl.newCall(req).execute()) {
            if (!r.isSuccessful()) throw new AiApiException(r.code(), "HTTP " + r.code());
            return r.body() != null ? r.body().string() : "";
        }
    }
    @PreDestroy public void destroy() {
        ocrClient.dispatcher().executorService().shutdown();
        llmClient.dispatcher().executorService().shutdown();
    }
}