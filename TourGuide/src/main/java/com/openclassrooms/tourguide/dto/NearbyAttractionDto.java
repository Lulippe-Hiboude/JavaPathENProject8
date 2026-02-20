package com.openclassrooms.tourguide.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NearbyAttractionDto {
    private String attractionName;
    private AttractionCoordinateDto attractionCoordinate;
    private UserCoordinateDto userCoordinate;
    private double distanceInMiles;
    private int rewardPoints;
}
