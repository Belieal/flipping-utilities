package com.flippingutilities.jobs;

import com.flippingutilities.utilities.WikiRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Responsible for handling all of the requests for wiki realtime data and ensuring too many requests aren't being made.
 */
@Slf4j
public class WikiDataFetcherJob {

    static final String API = "https://prices.runescape.wiki/api/v1/osrs/latest";
    ScheduledExecutorService executor;
    OkHttpClient httpClient;
    List<BiConsumer<WikiRequest, Instant>> subscribers = new ArrayList<>();
    Future wikiDataFetchTask;

    public WikiDataFetcherJob(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void subscribe(BiConsumer<WikiRequest, Instant> subscriber) {
        subscribers.add(subscriber);
    }

    public void start() {
        wikiDataFetchTask = executor.scheduleAtFixedRate(this::fetchWikiData, 5,60, TimeUnit.SECONDS);
        log.info("started wiki fetching job");
    }

    public void stop() {
        if (!wikiDataFetchTask.isCancelled() && !wikiDataFetchTask.isCancelled()) {
            wikiDataFetchTask.cancel(true);
            log.info("shut down wiki fetching job");
        }
    }

    public void fetchWikiData() {
        Request request = new Request.Builder().header("User-Agent", "FlippingUtilities").url(API).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //log.info("request for wiki data failed:", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        return;
                    };
                    try {
                        Gson gson = new Gson();
                        WikiRequest wikiRequest = gson.fromJson(responseBody.string(), WikiRequest.class);
                        Instant requestCompletionTime = Instant.now();
                        subscribers.forEach(subscriber -> subscriber.accept(wikiRequest, requestCompletionTime));
                    }
                    catch (JsonSyntaxException e) {
                    }
                }
            }
        });
    }
}

