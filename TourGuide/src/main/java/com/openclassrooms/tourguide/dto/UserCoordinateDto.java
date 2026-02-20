package com.openclassrooms.tourguide.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UserCoordinateDto {
    private double userLatitude;
    private double userLongitude;
}
