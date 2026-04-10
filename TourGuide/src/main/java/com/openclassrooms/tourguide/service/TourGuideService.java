package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.NearbyAttractionDto;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.mapper.AttractionMapper;
import com.openclassrooms.tourguide.persistences.record.AttractionDistance;
import com.openclassrooms.tourguide.persistences.user.User;
import com.openclassrooms.tourguide.persistences.user.UserReward;
import com.openclassrooms.tourguide.utils.LocationUtil;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tripPricer.Provider;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TourGuideService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TourGuideService.class);
    private final GpsService gpsService;
    private final RewardsService rewardsService;
    private final TripPricerService tripPricerService;
    boolean testMode = true;

    public TourGuideService(GpsService gpsService, RewardsService rewardsService, TripPricerService tripPricerService) {
        this.gpsService = gpsService;
        this.rewardsService = rewardsService;
        this.tripPricerService = tripPricerService;
        Locale.setDefault(Locale.US);

        if (testMode) {
            LOGGER.info("TestMode enabled");
            LOGGER.debug("Initializing users");
            initializeInternalUsers();
            LOGGER.debug("Finished initializing users");
        }
    }

    public List<UserReward> getUserRewards(final User user) {
        return user.getUserRewards();
    }

    /**
     * Retrieves the current location of a user.
     *
     * <p>If the user already has at least one recorded location, the most recent
     * visited location is returned. Otherwise, a new location is fetched using
     * {@link #trackUserLocation(User)}.</p>
     *
     * <p>This method ensures that a user always has a valid location.</p>
     *
     * @param user the user whose location is requested
     * @return the last known or newly tracked {@link VisitedLocation}
     */
    public VisitedLocation getUserLocation(final User user) {
        return (!user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation()
                : trackUserLocation(user);
    }

    public User getUser(final String userName) {
        return internalUserMap.get(userName);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(internalUserMap.values());
    }

    public void addUser(final User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
        }
    }

    /**
     * Retrieves trip deals for a given user based on their accumulated reward points.
     *
     * <p>The total reward points of the user are calculated and passed to the
     * {@link TripPricerService} to fetch pricing offers. Multiple calls are made
     * to increase the diversity of providers.</p>
     *
     * <p>The resulting list of {@link Provider} is stored in the user and returned.</p>
     *
     * @param user the user for whom to retrieve trip deals
     * @return a list of trip providers with pricing offers
     */
    public List<Provider> getTripDeals(final User user) {
        final int cumulativeRewardPoints = user.getUserRewards()
                .stream()
                .mapToInt(UserReward::rewardPoints).sum();

        final List<Provider> providers = IntStream.range(0, 2)
                .mapToObj(i -> tripPricerService.getPrice(user,
                        cumulativeRewardPoints))
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        user.setTripDeals(providers);
        return providers;
    }

    /**
     * Tracks and updates the current location of a user.
     *
     * <p>This method performs the following operations:</p>
     * <ul>
     *   <li>Fetches the user's current location from the {@link GpsService}</li>
     *   <li>Adds the location to the user's history of visited locations</li>
     *   <li>Triggers reward calculation via {@link RewardsService}</li>
     * </ul>
     *
     * <p>This method has side effects as it mutates the user's state.</p>
     *
     * @param user the user to track
     * @return the newly recorded {@link VisitedLocation}
     */
    public VisitedLocation trackUserLocation(final User user) {
        final VisitedLocation visitedLocation = gpsService.getUserLocation(user.getUserId());
        user.addToVisitedLocations(visitedLocation);
        rewardsService.calculateRewards(user);
        return visitedLocation;
    }

    /**
     * Retrieves the five closest tourist attractions to the user's current location.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Determines the user's current location</li>
     *   <li>Calculates the distance between the user and all known attractions</li>
     *   <li>Sorts attractions by ascending distance</li>
     *   <li>Selects the five nearest attractions</li>
     *   <li>Maps each attraction to a {@link NearbyAttractionDto}, including reward points</li>
     * </ol>
     *
     * <p>Distance is computed using {@link LocationUtil#getDistanceInMiles}.</p>
     *
     * @param user the user for whom to find nearby attractions
     * @return a list of the five closest attractions with distance and reward information
     */
    public List<NearbyAttractionDto> getFiveClosestAttractions(final User user) {
        final VisitedLocation visitedLocation = getUserLocation(user);

        final List<AttractionDistance> closestAttractions = gpsService.getAttractions()
                .stream()
                .map(attraction -> new AttractionDistance(attraction, LocationUtil.getDistanceInMiles(visitedLocation.location, attraction)))
                .sorted(Comparator.comparingDouble(AttractionDistance::distanceInMiles))
                .limit(5)
                .toList();

        return closestAttractions.stream()
                .map(attractionDistance -> {
                    final int rewardPoints = rewardsService.getRewardPoints(attractionDistance.attraction(), user);
                    return AttractionMapper.INSTANCE.toNearbyAttractionDto(attractionDistance, rewardPoints, visitedLocation.location);
                })
                .toList();
    }

    /**********************************************************************************
     *
     * Methods Below: For Internal Testing
     *
     **********************************************************************************/
    private static final String tripPricerApiKey = "test-server-api-key";
    // Database connection will be used for external users, but for testing purposes
    // internal users are provided and stored in memory
    private final Map<String, User> internalUserMap = new HashMap<>();

    private void initializeInternalUsers() {
        IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
            String userName = "internalUser" + i;
            String phone = "000";
            String email = userName + "@tourGuide.com";
            User user = new User(UUID.randomUUID(), userName, phone, email);
            generateUserLocationHistory(user);

            internalUserMap.put(userName, user);
        });
        LOGGER.debug("Created {} internal test users.", InternalTestHelper.getInternalUserNumber());
    }

    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> {
            user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
                    new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
        });
    }

    private double generateRandomLongitude() {
        double leftLimit = -180;
        double rightLimit = 180;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private double generateRandomLatitude() {
        double leftLimit = -85.05112878;
        double rightLimit = 85.05112878;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private Date getRandomTime() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }
}
