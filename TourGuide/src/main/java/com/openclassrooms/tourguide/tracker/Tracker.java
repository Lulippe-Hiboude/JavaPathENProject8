package com.openclassrooms.tourguide.tracker;

import com.openclassrooms.tourguide.persistences.user.User;
import com.openclassrooms.tourguide.service.TourGuideService;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Background tracker responsible for periodically updating the location of all users.
 *
 * <p>This component runs in its own dedicated thread and continuously performs
 * tracking operations at a fixed interval. For each cycle, it retrieves all users
 * from the {@link TourGuideService} and updates their location concurrently using
 * virtual threads.</p>
 *
 * <p>The tracker is automatically started when the instance is created and runs
 * until it is explicitly stopped or the application shuts down.</p>
 */
public class Tracker implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Tracker.class);
    private static final long TRACKING_POLLING_INTERVAL = TimeUnit.MINUTES.toSeconds(5);
    private final AtomicLong trackedUsersCount = new AtomicLong(0);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

    private final TourGuideService tourGuideService;
    private boolean stop = false;

    public Tracker(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
        executorService.submit(this);
        addShutDownHook();
    }

    /**
     * Assures to shut down the Tracker thread
     */
    public void stopTracking() {
        stop = true;
        executorService.shutdownNow();
    }

    /**
     * Main execution loop of the tracker.
     *
     * <p>This method continuously performs tracking cycles until the tracker
     * is interrupted or {@link #stopTracking()} is called.</p>
     *
     * <p>During each cycle, all users are tracked concurrently using virtual
     * threads and the method waits for all tasks to complete before sleeping
     * for the configured polling interval.</p>
     */
    @Override
    public void run() {
        StopWatch stopWatch = new StopWatch();
        while (true) {
            if (Thread.currentThread().isInterrupted() || stop) {
                LOGGER.debug("Tracker stopping");
                break;
            }

            List<User> users = tourGuideService.getAllUsers();
            LOGGER.debug("Begin Tracker. Tracking {} users.", users.size());
            stopWatch.start();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = new ArrayList<CompletableFuture<User>>(users.size());
                for (User user : users) {
                    futures.add(CompletableFuture.supplyAsync(() -> {
                                tourGuideService.trackUserLocation(user);
                                long count = trackedUsersCount.accumulateAndGet(1L, (previousCount, delta) -> {
                                    if (previousCount == Long.MAX_VALUE) {
                                        LOGGER.warn("Tracker reached max tracked locations count. Resetting counter.");
                                        return 1L;
                                    }
                                    return previousCount + delta;
                                });
                                if (count % 1000 == 0) {
                                    LOGGER.debug("Processed {} users", count);
                                }
                                return user;
                            }, executor)
                            .exceptionally(ex -> {
                                LOGGER.error("Error tracking user {}: {}", user.getUserName(), ex.getMessage(), ex);
                                return null;
                            }));
                }
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            }

            stopWatch.stop();
            LOGGER.debug("Tracker Time Elapsed: {} seconds.", TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
            stopWatch.reset();

            try {
                LOGGER.debug("Tracker sleeping");
                TimeUnit.SECONDS.sleep(TRACKING_POLLING_INTERVAL);
            } catch (InterruptedException e) {
                LOGGER.debug("Tracker interrupted");
                break;
            }
        }
        executorService.shutdownNow();
    }

    public long getTrackedUsersCount() {
        return trackedUsersCount.get();
    }

    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(Tracker.this::stopTracking));
    }
}
