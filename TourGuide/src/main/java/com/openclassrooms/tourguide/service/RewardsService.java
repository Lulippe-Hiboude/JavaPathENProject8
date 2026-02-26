package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.config.RewardProperties;
import com.openclassrooms.tourguide.persistences.user.User;
import com.openclassrooms.tourguide.persistences.user.UserReward;
import com.openclassrooms.tourguide.utils.LocationUtil;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class RewardsService {

    private final RewardProperties rewardProperties;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;
    private final List<Attraction> attractions;
    private final ConcurrentHashMap<UUID, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral, RewardProperties rewardProperties) {
        this.rewardProperties = rewardProperties;
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
        this.attractions = gpsUtil.getAttractions();
    }

    public void calculateRewards(final User user) {
        ReentrantLock lock = userLocks.computeIfAbsent(
                user.getUserId(),
                id -> new ReentrantLock()
        );
        lock.lock();
        try {
            final List<VisitedLocation> userLocations = user.getVisitedLocations();

            final Set<UUID> rewardedAttractionIds = new HashSet<>();
            user.getUserRewards()
                    .forEach(userReward -> rewardedAttractionIds.add(userReward.attraction.attractionId));

            final List<UserReward> newRewards = userLocations.stream()
                    .flatMap(visitedLocation -> getRewardStream(user, visitedLocation, attractions, rewardedAttractionIds))
                    .toList();

            user.getUserRewards().addAll(newRewards);

        } finally {
            lock.unlock();
        }
    }

    private Stream<UserReward> getRewardStream(final User user, final VisitedLocation visitedLocation,
                                               final List<Attraction> attractions, final Set<UUID> rewardedAttractionIds) {
        return attractions.stream()
                .filter(attraction -> isWithinProximity(attraction, visitedLocation.location, rewardProperties.getDefaultProximityBuffer()))
                .filter(attraction -> rewardedAttractionIds.add(attraction.attractionId))
                .map(attraction -> new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
    }

    public boolean isWithinAttractionProximity(final Attraction attraction, final Location location) {
        return isWithinProximity(attraction, location, rewardProperties.getAttractionProximityRange());
    }

    private boolean isWithinProximity(final Attraction attraction, final Location location, final int range) {
        return (LocationUtil.getDistanceInMiles(attraction, location) <= range);
    }

    public int getRewardPoints(final Attraction attraction, final User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }
}
