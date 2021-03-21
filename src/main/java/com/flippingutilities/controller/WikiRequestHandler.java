package com.flippingutilities.controller;

import com.flippingutilities.utilities.WikiRequest;
import com.google.gson.Gson;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Responsible for handling all of the requests for wiki realtime data and ensuring too many requests aren't being made.
 */
@Slf4j
public class WikiRequestHandler {

    OkHttpClient httpClient;
    private Map<Integer, Pair<Instant, WikiRequest>> pastRequests = new HashMap<>();

    WikiRequestHandler(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }


    private boolean requestedLessThanGivenSecondsAgo(int itemId, int seconds) {
        if (pastRequests.containsKey(itemId)) {
            long oldRequestTime = pastRequests.get(itemId).getKey().getEpochSecond();
            long currentTime = Instant.now().getEpochSecond();
            return (Math.abs(currentTime - oldRequestTime) < seconds);
        }
        return false;
    }

    public void fetchWikiData(int itemId, Consumer<WikiRequest> callback) {
        if (requestedLessThanGivenSecondsAgo(itemId, 60)) {
            WikiRequest oldRequest = pastRequests.get(itemId).getValue();
            callback.accept(oldRequest);
        }

        Request request = new Request.Builder().
                header("User-Agent", "FlippingUtilities").
                url("https://prices.runescape.wiki/api/v1/osrs/latest?id=" + itemId).
                build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                    Gson gson = new Gson();
                    WikiRequest wikiRequest = gson.fromJson(responseBody.string(), WikiRequest.class);
                    callback.accept(wikiRequest);
                }
            }
        });
    }
}

