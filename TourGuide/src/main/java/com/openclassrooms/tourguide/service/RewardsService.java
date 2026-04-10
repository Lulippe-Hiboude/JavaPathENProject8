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

    /**
     * Calculates and assigns rewards to a user based on their visited locations
     * and proximity to known attractions.
     *
     * <p>This method compares every location visited by the user with all known
     * attractions retrieved from {@link GpsService}. If the user has been within
     * the configured proximity buffer of an attraction and has not already received
     * a reward for it, a reward calculation task is created.</p>
     *
     * <p>Reward calculations are executed concurrently using virtual threads via
     * {@link Executors#newVirtualThreadPerTaskExecutor()}. Each task retrieves the
     * reward points from {@link RewardCentral} and creates a {@link UserReward}
     * object.</p>
     *
     * <p>Once all asynchronous computations complete, the resulting rewards are
     * collected and added to the user.</p>
     *
     * <p>Error handling is performed per task to avoid interrupting the entire
     * reward calculation process if one attraction fails.</p>
     *
     * @param user the user for whom rewards are being calculated
     */
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
                                .exceptionally(ex -> handleRewardCalculationError(user, attraction, ex))
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

    /**
     * Determines whether a given location is within the configured proximity
     * range of a specific attraction.
     *
     * <p>The distance between the attraction and the provided location is
     * calculated using {@link LocationUtil#getDistanceInMiles}. The result
     * is then compared against the attraction proximity range defined in
     * {@link RewardProperties}.</p>
     *
     * @param attraction the attraction to compare against
     * @param location the location to evaluate
     * @return {@code true} if the location is within the attraction proximity range,
     *         {@code false} otherwise
     */
    public boolean isWithinAttractionProximity(final Attraction attraction, final Location location) {
        return isWithinProximity(attraction, location, rewardProperties.getAttractionProximityRange());
    }

    private boolean isWithinProximity(final Attraction attraction,
                                      final Location location,
                                      final int range) {
        return (LocationUtil.getDistanceInMiles(attraction, location) <= range);
    }

    private static UserReward handleRewardCalculationError(User user, Attraction attraction, Throwable ex) {
        log.error("Error calculating reward for user {} and attraction {}: {}",
                user.getUserName(),
                attraction.attractionName,
                ex.getMessage(),
                ex);
        return null;
    }

    /**
     * Retrieves the reward points associated with a given attraction for a user.
     *
     * <p>If the user has not yet received a reward for the specified attraction,
     * the reward points are requested from {@link RewardCentral}. If the reward
     * has already been granted, the method returns {@code 0} to prevent duplicate
     * rewards.</p>
     *
     * @param attraction the attraction associated with the reward
     * @param user the user eligible for the reward
     * @return the number of reward points granted for the attraction,
     *         or {@code 0} if the user has already received the reward
     */
    public int getRewardPoints(final Attraction attraction, final User user) {
        if (!user.hasRewardForAttraction(attraction)) {
            return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
        }
        return 0;
    }
}
