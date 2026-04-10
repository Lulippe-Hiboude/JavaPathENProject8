package com.openclassrooms.tourguide.utils;

import gpsUtil.location.Location;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LocationUtil {
    private static final double MILES_PER_NAUTICAL_MILES = 1.15077945;

    /**
     * Calculates the distance in miles between two geographic locations.
     *
     * <p>The distance is calculated using the great-circle distance formula,
     * which determines the shortest path between two points on the surface
     * of a sphere (the Earth).</p>
     *
     * <p>The algorithm performs the following steps:</p>
     * <ul>
     *   <li>Convert both locations from degrees to radians</li>
     *   <li>Compute the central angle between the two points</li>
     *   <li>Convert the angular distance to nautical miles</li>
     *   <li>Convert nautical miles to statute miles</li>
     * </ul>
     *
     * @param location1 the first geographic location
     * @param location2 the second geographic location
     * @return the distance between the two locations expressed in miles
     */
    public double getDistanceInMiles(final Location location1, final Location location2) {
        final Location location1InRadian = toRadian(location1);
        final Location location2InRadian = toRadian(location2);

        final double angle = calculateCentralAngleInRadians(location1InRadian, location2InRadian);

        final double nauticalMiles = 60 * Math.toDegrees(angle);
        return nauticalMiles * MILES_PER_NAUTICAL_MILES;
    }

    private static double calculateCentralAngleInRadians(final Location location1InRadian, final Location location2InRadian) {
        return Math.acos(
                Math.sin(location1InRadian.latitude) * Math.sin(location2InRadian.latitude)
                        + Math.cos(location1InRadian.latitude) * Math.cos(location2InRadian.latitude)
                        * Math.cos(location1InRadian.longitude - location2InRadian.longitude));
    }

    private static Location toRadian(final Location location) {
        return new Location(
                Math.toRadians(location.latitude),
                Math.toRadians(location.longitude));
    }
}
