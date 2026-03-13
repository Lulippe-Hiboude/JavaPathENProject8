package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.config.RewardProperties;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.persistences.user.User;
import com.openclassrooms.tourguide.persistences.user.UserReward;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.tracker.Tracker;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import rewardCentral.RewardCentral;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRewardsService {

    @Test
    public void userGetRewards() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), new RewardProperties());

        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        Tracker tracker = new Tracker(tourGuideService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        Attraction attraction = gpsUtil.getAttractions().getFirst();
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
        tourGuideService.trackUserLocation(user);
        List<UserReward> userRewards = user.getUserRewards();
        tracker.stopTracking();
        assertThat(userRewards).hasSize(1);

    }

    @Test
    public void isWithinAttractionProximity() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), new RewardProperties());
        Attraction attraction = gpsUtil.getAttractions().getFirst();
        assertThat(rewardsService.isWithinAttractionProximity(attraction, attraction)).isTrue();
    }

    @RepeatedTest(5)
    public void nearAllAttractions() {
        final RewardProperties rewardProperties = getTestRewardProperties();
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), rewardProperties);

        InternalTestHelper.setInternalUserNumber(1);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        Tracker tracker = new Tracker(tourGuideService);
        rewardsService.calculateRewards(tourGuideService.getAllUsers().get(0));
        List<UserReward> userRewards = tourGuideService.getUserRewards(tourGuideService.getAllUsers().get(0));
        tracker.stopTracking();
        final List<String> rewards = userRewards.stream()
                .map(r -> r.attraction.attractionName)
                .toList();
        System.out.println("Rewards: " + rewards);
        assertThat(userRewards).hasSameSizeAs(gpsUtil.getAttractions());

    }

    @Test
    @DisplayName("should return rewardPoint")
    void shouldReturnRewardPoint() {
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), new RewardProperties());
        Attraction attraction = gpsUtil.getAttractions().getFirst();
        int rewardPoints = rewardsService.getRewardPoints(attraction, user);
        assertTrue(rewardPoints > 0);
    }

    private RewardProperties getTestRewardProperties() {
        final RewardProperties rewardProperties = new RewardProperties();
        rewardProperties.setDefaultProximityBuffer(Integer.MAX_VALUE);
        return rewardProperties;
    }
}
