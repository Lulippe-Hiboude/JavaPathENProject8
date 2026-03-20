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

    public List<Provider> getPrice(final User user, final int cumulativeRewardPoints) {
        return tripPricer.getPrice(apiKey, user.getUserId(),
                user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
                user.getUserPreferences().getTripDuration(), cumulativeRewardPoints);
    }
}

