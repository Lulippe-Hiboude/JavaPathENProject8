package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.config.RewardProperties;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.tracker.Tracker;
import gpsUtil.GpsUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rewardCentral.RewardCentral;

@Configuration
public class TourGuideModule {

    @Bean
    public GpsUtil gpsUtil() {
        return new GpsUtil();
    }

    @Bean
    public RewardsService rewardsService() {
        return new RewardsService(gpsUtil(), rewardCentral(), rewardProperties());
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
    TourGuideService tourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
        return new TourGuideService(gpsUtil, rewardsService);
    }

    @Bean
    Tracker tracker(TourGuideService tourGuideService) {
        return new Tracker(tourGuideService);
    }

}
