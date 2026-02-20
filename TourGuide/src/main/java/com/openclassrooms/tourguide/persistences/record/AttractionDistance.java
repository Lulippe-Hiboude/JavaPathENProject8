package com.openclassrooms.tourguide.persistences.record;

import gpsUtil.location.Attraction;

public record AttractionDistance(Attraction attraction, double distanceInMiles) {
}
