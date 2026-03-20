package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.config.RewardProperties;
import com.openclassrooms.tourguide.persistences.user.User;
import com.openclassrooms.tourguide.persistences.user.UserReward;
import com.openclassrooms.tourguide.utils.LocationUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import lombok.extern.slf4j.Slf4j;
import rewardCentral.RewardCentral;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
public class RewardsService {

    private final RewardProperties rewardProperties;
    private final GpsService gpsService;
    private final RewardCentral rewardsCentral;

    public RewardsService(GpsService gpsService, RewardCentral rewardCentral, RewardProperties rewardProperties) {
        this.rewardProperties = rewardProperties;
        this.gpsService = gpsService;
        this.rewardsCentral = rewardCentral;
    }

    public void calculateRewards(final User user) {
        final List<Attraction> attractions = gpsService.getAttractions();
        final List<VisitedLocation> userLocations = user.getVisitedLocations();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<UserReward>> userRewards = new LinkedList<>();

            for (VisitedLocation visitedLocation : userLocations) {
                for (Attraction attraction : attractions) {
                    if (!user.hasRewardForAttraction(attraction) && isWithinProximity(attraction, visitedLocation.location, rewardProperties.getDefaultProximityBuffer())) {
                        userRewards.add(CompletableFuture.supplyAsync(() -> new UserReward(visitedLocation, attraction,
                                getRewardPoints(attraction, user)), executor)
                                .exceptionally(ex -> {
                                    log.error("Error calculating reward for user {} and attraction {}: {}",
                                            user.getUserName(),
                                            attraction.attractionName,
                                            ex.getMessage(),
                                            ex);
                                    return null;
                                })
                        );
                    }
                }
            }
            userRewards.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .forEach(user::addUserReward);
        }
    }

    public boolean isWithinAttractionProximity(final Attraction attraction, final Location location) {
        return isWithinProximity(attraction, location, rewardProperties.getAttractionProximityRange());
    }

    private boolean isWithinProximity(final Attraction attraction,
                                      final Location location,
                                      final int range) {
        return (LocationUtil.getDistanceInMiles(attraction, location) <= range);
    }

    public int getRewardPoints(final Attraction attraction, final User user) {
        if (!user.hasRewardForAttraction(attraction)) {
            return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
        }
        return 0;
    }
}
