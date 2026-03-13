package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.config.RewardProperties;
import com.openclassrooms.tourguide.dto.NearbyAttractionDto;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.persistences.user.User;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.utils.LocationUtil;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import rewardCentral.RewardCentral;
import tripPricer.Provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TestTourGuideService {

    @Test
    public void getUserLocation() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), new RewardProperties());
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        Tracker tracker = new Tracker(tourGuideService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);

        tracker.stopTracking();

        assertThat(visitedLocation.userId).isEqualTo(user.getUserId());
    }

    @Test
    public void addUser() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), new RewardProperties());
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        Tracker tracker = new Tracker(tourGuideService);
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

        tourGuideService.addUser(user);
        tourGuideService.addUser(user2);

        User retrievedUser = tourGuideService.getUser(user.getUserName());
        User retrievedUser2 = tourGuideService.getUser(user2.getUserName());

        tracker.stopTracking();

        assertSoftly(softly -> {
            softly.assertThat(retrievedUser).isEqualTo(user);
            softly.assertThat(retrievedUser2).isEqualTo(user2);
        });
    }

    @Test
    public void getAllUsers() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), new RewardProperties());
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        Tracker tracker = new Tracker(tourGuideService);
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

        tourGuideService.addUser(user);
        tourGuideService.addUser(user2);

        List<User> allUsers = tourGuideService.getAllUsers();

        tracker.stopTracking();

        assertThat(allUsers).contains(user, user2);
    }

    @Test
    public void trackUser() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), new RewardProperties());
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        Tracker tracker = new Tracker(tourGuideService);
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);

        tracker.stopTracking();

        assertThat(visitedLocation.userId).isEqualTo(user.getUserId());
    }

    @Test
    public void getNearbyAttractions() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), new RewardProperties());
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        Tracker tracker = new Tracker(tourGuideService);
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

        List<NearbyAttractionDto> attractions = tourGuideService.getFiveClosestAttractions(user);

        tracker.stopTracking();

        assertThat(attractions).hasSize(5);
    }

    @Test
    public void getTripDeals() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral(), new RewardProperties());
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        Tracker tracker = new Tracker(tourGuideService);
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

        List<Provider> providers = tourGuideService.getTripDeals(user);

        tracker.stopTracking();

        assertThat(providers).hasSize(10);
    }

    @Test
    @DisplayName("should get the 5 nearest attractions to the user")
    void getNearbyAttractionsTest() {
        //given
        final GpsUtil gpsUtilMock = mock(GpsUtil.class);
        final RewardsService rewardsServiceMock = mock(RewardsService.class);
        InternalTestHelper.setInternalUserNumber(0);
        final TourGuideService tourGuideService = Mockito.spy(new TourGuideService(gpsUtilMock, rewardsServiceMock));
        final User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        final Date date = new Date();
        date.setTime(date.getTime());

        final VisitedLocation visitedLocation = new VisitedLocation(
                user.getUserId(),
                new Location(42.3601, -71.0589),
                date
        );

        final Location bostonCommonLocation = new Location(42.355, -71.065);

        final double distanceToBostonCommon = LocationUtil.getDistanceInMiles(visitedLocation.location, bostonCommonLocation);

        final List<Attraction> attractions = getAttractionList();

        given(gpsUtilMock.getAttractions()).willReturn(attractions);
        given(tourGuideService.getUserLocation(user)).willReturn(visitedLocation);
        given(rewardsServiceMock.getRewardPoints(any(), any())).willReturn(100);
        //when
        List<NearbyAttractionDto> expected = tourGuideService.getFiveClosestAttractions(user);

        //then
        assertThat(expected).hasSize(5);
        assertThat(expected.get(0).getAttractionName())
                .isEqualTo("Boston Common");

        assertThat(expected.get(0).getRewardPoints())
                .isEqualTo(100);

        assertThat(expected.get(0).getDistanceInMiles())
                .isEqualTo(distanceToBostonCommon);
    }

    private static List<Attraction> getAttractionList() {
        final List<Attraction> attractions = new ArrayList<>();
        attractions.add(new Attraction("Disneyland", "Anaheim", "CA", 33.817595, -117.922008));
        attractions.add(new Attraction("Jackson Hole", "Jackson Hole", "WY", 43.582767, -110.821999));
        attractions.add(new Attraction("Mojave National Preserve", "Kelso", "CA", 35.141689, -115.510399));
        attractions.add(new Attraction("Joshua Tree National Park", "Joshua Tree National Park", "CA", 33.881866, -115.90065));
        attractions.add(new Attraction("Buffalo National River", "St Joe", "AR", 35.985512, -92.757652));
        attractions.add(new Attraction("Hot Springs National Park", "Hot Springs", "AR", 34.52153, -93.042267));
        attractions.add(new Attraction("Kartchner Caverns State Park", "Benson", "AZ", 31.837551, -110.347382));
        attractions.add(new Attraction("Legend Valley", "Thornville", "OH", 39.937778, -82.40667));
        attractions.add(new Attraction("Flowers Bakery of London", "Flowers Bakery of London", "KY", 37.131527, -84.07486));
        attractions.add(new Attraction("McKinley Tower", "Anchorage", "AK", 61.218887, -149.877502));
        attractions.add(new Attraction("Boston Common", "BostonTest", "BostonTest", 42.355, -71.065));
        return attractions;
    }
}
