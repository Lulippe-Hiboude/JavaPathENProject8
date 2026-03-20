package com.openclassrooms.tourguide.persistences.user;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record UserReward (
        VisitedLocation visitedLocation,
        Attraction attraction,
        int rewardPoints
) {
}
