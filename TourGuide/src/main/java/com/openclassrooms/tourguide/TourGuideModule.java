package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.config.RewardProperties;
import com.openclassrooms.tourguide.service.GpsService;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.service.TripPricerService;
import com.openclassrooms.tourguide.tracker.Tracker;
import gpsUtil.GpsUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rewardCentral.RewardCentral;
import tripPricer.TripPricer;

@Configuration
public class TourGuideModule {

    @Bean
    public GpsService gpsService() {
        return new GpsService(new GpsUtil());
    }

    @Bean
    public RewardsService rewardsService() {
        return new RewardsService(gpsService(), rewardCentral(), rewardProperties());
    }

    @Bean
    public RewardCentral rewardCentral() {
        return new RewardCentral();
    }

    @Bean
    @ConfigurationProperties(prefix = "reward")
    public RewardProperties rewardProperties() {
        return new RewardProperties();
    }

    @Bean
    TripPricerService tripPricerService (@Value("${trip.pricer.api.key}") String apiKey) {
        return new TripPricerService(new TripPricer(), apiKey);
    }

    @Bean
    TourGuideService tourGuideService(final GpsService gpsService, final RewardsService rewardsService, TripPricerService tripPricerService) {
        return new TourGuideService(gpsService, rewardsService,tripPricerService);
    }

    @Bean
    Tracker tracker(final TourGuideService tourGuideService) {
        return new Tracker(tourGuideService);
    }
}
