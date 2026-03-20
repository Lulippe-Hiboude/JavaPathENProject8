package com.openclassrooms.tourguide.service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class GpsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GpsService.class);
    private final ReentrantLock lock = new ReentrantLock();

    private final Duration timeToLive = Duration.ofMinutes(5);

    private List<Attraction> lastAttractionsResult = List.of();

    private long lastTimeCacheRefreshed = 0;

    private final GpsUtil gpsUtil;

    public GpsService(GpsUtil gpsUtil) {
        // GpsUtil will only work parsing distances using a default US locale.
        Locale.setDefault(Locale.US);
        this.gpsUtil = gpsUtil;
    }

    public List<Attraction> getAttractions() {
        lock.lock();
        try {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastTimeCacheRefreshed >= timeToLive.toMillis()) {
                LOGGER.debug("Refreshing attractions data from GPSUtil.");
                lastTimeCacheRefreshed = currentTimeMillis;
                lastAttractionsResult = gpsUtil.getAttractions();
            }
            return lastAttractionsResult;
        } finally {
            lock.unlock();
        }
    }

    public VisitedLocation getUserLocation(final UUID userId) {
        return gpsUtil.getUserLocation(userId);
    }
}

