package com.diemlife.utils;

import com.typesafe.config.Config;
import play.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public abstract class AsyncUtils {

    private AsyncUtils() {
        super();
    }

    public static void processIdsAsync(final Config config, final List<Integer> ids, final Consumer<Integer> idConsumer) {
        final CompletableFuture<List<Integer>> future = CompletableFuture.supplyAsync(() -> {
            final int poolSize = config.getInt("application.workers.poolSize");
            final ForkJoinPool forkJoinPool = new ForkJoinPool(poolSize);
            final ForkJoinTask<List<Integer>> task = forkJoinPool.submit(() -> ids
                    .parallelStream()
                    .peek(idConsumer)
                    .collect(toList()));
            try {
                return task.get();
            } catch (final Exception e) {
                Logger.error("Error processing IDs", e);

                return emptyList();
            }
        });
        future.thenAccept(resultList -> Logger.debug("Run " + resultList.size() + " async tasks"));
    }


}
