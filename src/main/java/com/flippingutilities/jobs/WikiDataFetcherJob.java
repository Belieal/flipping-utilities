package com.flippingutilities.jobs;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.utilities.WikiRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    FlippingPlugin plugin;
    ScheduledExecutorService executor;
    OkHttpClient httpClient;
    List<BiConsumer<WikiRequest, Instant>> subscribers = new ArrayList<>();
    Future wikiDataFetchTask;
    Instant timeOfLastRequestCompletion;
    boolean inFlightRequest = false;


    public WikiDataFetcherJob(FlippingPlugin plugin, OkHttpClient httpClient) {
        this.plugin = plugin;
        this.httpClient = httpClient;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void subscribe(BiConsumer<WikiRequest, Instant> subscriber) {
        subscribers.add(subscriber);
    }

    public void start() {
        wikiDataFetchTask = executor.scheduleAtFixedRate(this::attemptToFetchWikiData, 5,1, TimeUnit.SECONDS);
        log.info("started wiki fetching job");
    }

    public void stop() {
        if (!wikiDataFetchTask.isCancelled() && !wikiDataFetchTask.isCancelled()) {
            wikiDataFetchTask.cancel(true);
            log.info("shut down wiki fetching job");
        }
    }

    //only problem with this is that then master panel will be visible even if they have opened and then closed flipping utils
    //as long as they haven't opened another plugin. But if they have another plugin open or they haven't opened flipping utils
    //then masterpanel.isVisible() will correctly return false.
    private boolean shouldFetch() {
        boolean lastRequestMoreThan60SecondsAgo = timeOfLastRequestCompletion == null || Instant.now().minus(60, ChronoUnit.SECONDS).isAfter(timeOfLastRequestCompletion);
        return plugin.getMasterPanel().isVisible() && !inFlightRequest && lastRequestMoreThan60SecondsAgo;
    }

    public void attemptToFetchWikiData() {
        if (!shouldFetch()) {
            return;
        }
        inFlightRequest = true;
        Request request = new Request.Builder().header("User-Agent", "FlippingUtilities").url(API).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                timeOfLastRequestCompletion = Instant.now();
                inFlightRequest = false;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        timeOfLastRequestCompletion = Instant.now();
                        inFlightRequest = false;
                        return;
                    }
                    try {
                        timeOfLastRequestCompletion = Instant.now();
                        inFlightRequest = false;
                        Gson gson = new Gson();
                        WikiRequest wikiRequest = gson.fromJson(responseBody.string(), WikiRequest.class);
                        subscribers.forEach(subscriber -> subscriber.accept(wikiRequest, timeOfLastRequestCompletion));
                    }
                    catch (JsonSyntaxException e) { }
                }
            }
        });
    }
}

