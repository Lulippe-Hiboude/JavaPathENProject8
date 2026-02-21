package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.config.RewardProperties;
import com.openclassrooms.tourguide.persistences.user.User;
import com.openclassrooms.tourguide.persistences.user.UserReward;
import com.openclassrooms.tourguide.utils.LocationUtil;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rewardCentral.RewardCentral;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service

public class RewardsService {

    private final RewardProperties rewardProperties;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final List<Attraction> attractions;

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral, RewardProperties rewardProperties) {
        this.rewardProperties = rewardProperties;
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
        this.attractions = gpsUtil.getAttractions();
    }

    public void calculateRewards(final User user) {
        final List<VisitedLocation> userLocations = user.getVisitedLocations();

        final Set<UUID> rewardedAttractionIds = new HashSet<>();
        user.getUserRewards()
                .forEach(userReward -> rewardedAttractionIds.add(userReward.attraction.attractionId));

        userLocations.forEach(visitedLocation -> attractions.stream()
                .filter(attraction -> !rewardedAttractionIds.contains(attraction.attractionId)
                        && isWithinProximity(attraction, visitedLocation.location, rewardProperties.getDefaultProximityBuffer()))
                .forEach(attraction -> {user.addUserReward(new UserReward(visitedLocation,attraction));
                rewardedAttractionIds.add(attraction.attractionId);}));

       /* final List<UserReward> newRewards = userLocations.stream()
                .flatMap(visitedLocation -> getRewardStream(user, visitedLocation, attractions, rewardedAttractionIds))
                .toList();

        user.getUserRewards().addAll(newRewards);*/
    }

    public void calculateRewardTest(final User user) {
        final List<VisitedLocation> userLocations = user.getVisitedLocations();
        Set<UUID> rewardedAttractionIds = user.getUserRewards().stream()
                .map(r -> r.attraction.attractionId)
                .collect(Collectors.toSet());

        for (VisitedLocation visitedLocation : userLocations) {
            for (Attraction attraction : attractions) {
                if (!rewardedAttractionIds.contains(attraction.attractionId)) {
                    if (isWithinProximity(attraction, visitedLocation.location, rewardProperties.getDefaultProximityBuffer())) {
                        user.addUserReward(new UserReward(visitedLocation, attraction));
                        rewardedAttractionIds.add(attraction.attractionId);
                    }
                }
            }
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
