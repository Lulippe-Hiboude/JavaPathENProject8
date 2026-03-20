package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.config.RewardProperties;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.persistences.user.User;
import com.openclassrooms.tourguide.persistences.user.UserReward;
import com.openclassrooms.tourguide.service.GpsService;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.service.TripPricerService;
import com.openclassrooms.tourguide.tracker.Tracker;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import rewardCentral.RewardCentral;
import tripPricer.TripPricer;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class TestRewardsService {

    @Value("${trip.pricer.api.key}")
    private String apiKey;

    @Test
    public void userGetRewards() {
        //GIVEN
        final GpsUtil gpsUtil = new GpsUtil();
        final GpsService gpsService = new GpsService(gpsUtil);
        final RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), new RewardProperties());
        final TripPricerService tripPricerService = new TripPricerService(new TripPricer(), apiKey);
        InternalTestHelper.setInternalUserNumber(0);
        final TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, tripPricerService);
        final Tracker tracker = new Tracker(tourGuideService);

        final User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        final Attraction attraction = gpsService.getAttractions().getFirst();
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));

        //WHEN & THEN
        tourGuideService.trackUserLocation(user);
        final List<UserReward> userRewards = user.getUserRewards();
        tracker.stopTracking();
        assertThat(userRewards).hasSize(1);
    }

    @Test
    public void isWithinAttractionProximity() {
        //GIVEN
        final GpsUtil gpsUtil = new GpsUtil();
        final GpsService gpsService = new GpsService(gpsUtil);
        final RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), new RewardProperties());
        final Attraction attraction = gpsService.getAttractions().getFirst();

        //WHEN & THEN
        assertThat(rewardsService.isWithinAttractionProximity(attraction, attraction)).isTrue();
    }

    @RepeatedTest(5)
    public void nearAllAttractions() {
        //GIVEN
        final RewardProperties rewardProperties = getTestRewardProperties();
        final GpsUtil gpsUtil = new GpsUtil();
        final GpsService gpsService = new GpsService(gpsUtil);
        final RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), rewardProperties);
        final TripPricerService tripPricerService = new TripPricerService(new TripPricer(), apiKey);
        InternalTestHelper.setInternalUserNumber(1);
        final TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, tripPricerService);
        final Tracker tracker = new Tracker(tourGuideService);

        //WHEN & THEN
        rewardsService.calculateRewards(tourGuideService.getAllUsers().getFirst());
        final List<UserReward> userRewards = tourGuideService.getUserRewards(tourGuideService.getAllUsers().getFirst());
        tracker.stopTracking();
        final List<String> rewards = userRewards.stream()
                .map(r -> r.attraction().attractionName)
                .toList();
        System.out.println("Rewards: " + rewards);
        assertThat(userRewards).hasSameSizeAs(gpsService.getAttractions());
    }

    @Test
    @DisplayName("should return rewardPoint")
    void shouldReturnRewardPoint() {
        //GIVEN
        final User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        final GpsUtil gpsUtil = new GpsUtil();
        final GpsService gpsService = new GpsService(gpsUtil);
        final RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), new RewardProperties());
        final Attraction attraction = gpsService.getAttractions().getFirst();

        //WHEN & THEN
        final int rewardPoints = rewardsService.getRewardPoints(attraction, user);
        assertTrue(rewardPoints > 0);
    }

    @Test
    @DisplayName("should return 0 rewardPoint if user already has reward for attraction")
    void shouldReturnZeroRewardPointIfUserAlreadyHasRewardForAttraction() {
        //Given
        final User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        final GpsUtil gpsUtil = new GpsUtil();
        final GpsService gpsService = new GpsService(gpsUtil);
        final RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), new RewardProperties());
        final Attraction attraction = gpsService.getAttractions().getFirst();

        final VisitedLocation visitedLocation = new VisitedLocation(
                user.getUserId(),
                attraction,
                new Date()
        );

        final UserReward existingReward = new UserReward(
                visitedLocation,
                attraction,
                100
        );

        user.addUserReward(existingReward);

        // WHEN
        final int rewardPoints = rewardsService.getRewardPoints(attraction, user);

        // THEN
        assertThat(rewardPoints).isEqualTo(0);
    }

    @Test
    @DisplayName("should handle exception in CompletableFuture and not add reward")
    void shouldHandleExceptionInCompletableFutureAndNotAddReward() {
        // GIVEN
        final GpsUtil gpsUtil = new GpsUtil();
        final GpsService gpsService = new GpsService(gpsUtil);
        final RewardCentral rewardCentral = mock(RewardCentral.class);
        when(rewardCentral.getAttractionRewardPoints(any(), any()))
                .thenThrow(new RuntimeException("Boom"));
        final RewardsService rewardsService = new RewardsService(
                gpsService,
                rewardCentral,
                new RewardProperties()
        );
        final User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

        final Attraction attraction = gpsService.getAttractions().getFirst();

        user.addToVisitedLocations(new VisitedLocation(
                user.getUserId(),
                attraction,
                new Date()
        ));

        // WHEN
        rewardsService.calculateRewards(user);

        // THEN
        assertThat(user.getUserRewards()).isEmpty();
    }

    @Test
    @DisplayName("should continue processing rewards when some CompletableFutures fail")
    void shouldContinueWhenSomeFuturesFail() {
        // GIVEN
        final GpsUtil gpsUtil = new GpsUtil();
        final GpsService gpsService = new GpsService(gpsUtil);

        final RewardCentral rewardCentral = mock(RewardCentral.class);

        final List<Attraction> attractions = gpsService.getAttractions();

        final Attraction failingAttraction = attractions.get(0);
        final Attraction successAttraction = attractions.get(1);

        final RewardProperties rewardProperties = getTestRewardProperties();

        when(rewardCentral.getAttractionRewardPoints(
                eq(failingAttraction.attractionId), any()))
                .thenThrow(new RuntimeException("Boom"));

        when(rewardCentral.getAttractionRewardPoints(
                eq(successAttraction.attractionId), any()))
                .thenReturn(100);
        final RewardsService rewardsService = new RewardsService(
                gpsService,
                rewardCentral,
                rewardProperties
        );
        final User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        user.addToVisitedLocations(new VisitedLocation(
                user.getUserId(),
                failingAttraction,
                new Date()
        ));

        // WHEN
        rewardsService.calculateRewards(user);

        // THEN
        final List<UserReward> rewards = user.getUserRewards();

        assertThat(rewards).isNotEmpty();
        assertThat(rewards)
                .allMatch(r -> !r.attraction().attractionId.equals(failingAttraction.attractionId));

    }

    private RewardProperties getTestRewardProperties() {
        final RewardProperties rewardProperties = new RewardProperties();
        rewardProperties.setDefaultProximityBuffer(Integer.MAX_VALUE);
        return rewardProperties;
    }
}
