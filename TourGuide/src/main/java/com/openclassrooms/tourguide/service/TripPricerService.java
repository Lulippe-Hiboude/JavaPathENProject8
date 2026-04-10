package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.persistences.user.User;
import tripPricer.Provider;
import tripPricer.TripPricer;

import java.util.List;

public class TripPricerService {
    private final TripPricer tripPricer;

    private final String apiKey;

    public TripPricerService(TripPricer tripPricer, String apiKey) {
        this.tripPricer = tripPricer;
        this.apiKey = apiKey;
    }

    /**
     * Retrieves pricing offers for a user based on their travel preferences
     * and accumulated reward points.
     *
     * <p>This method delegates the request to the external {@link TripPricer}
     * service. The returned providers represent different travel offers
     * available to the user.</p>
     *
     * <p>The price calculation takes into account:</p>
     * <ul>
     *   <li>The user's unique identifier</li>
     *   <li>The number of adults and children traveling</li>
     *   <li>The duration of the trip</li>
     *   <li>The cumulative reward points of the user</li>
     * </ul>
     *
     * @param user the user requesting trip offers
     * @param cumulativeRewardPoints the total reward points accumulated by the user
     * @return a list of {@link Provider} representing available trip deals
     */
    public List<Provider> getPrice(final User user, final int cumulativeRewardPoints) {
        return tripPricer.getPrice(
                apiKey,
                user.getUserId(),
                user.getUserPreferences().getNumberOfAdults(),
                user.getUserPreferences().getNumberOfChildren(),
                user.getUserPreferences().getTripDuration(),
                cumulativeRewardPoints);
    }
}

