package com.openclassrooms.tourguide.mapper;

import com.openclassrooms.tourguide.dto.AttractionCoordinateDto;
import com.openclassrooms.tourguide.dto.NearbyAttractionDto;
import com.openclassrooms.tourguide.dto.UserCoordinateDto;
import com.openclassrooms.tourguide.persistences.record.AttractionDistance;
import gpsUtil.location.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AttractionMapper {
    AttractionMapper INSTANCE = Mappers.getMapper(AttractionMapper.class);

    @Mapping(target = "attractionName", source = "attractionDistance.attraction.attractionName")
    @Mapping(target = "attractionCoordinate",source = "attractionDistance", qualifiedByName = "toAttractionCoordinateDto")
    @Mapping(target = "userCoordinate", source = "location", qualifiedByName = "toUserCoordinateDto")
    @Mapping(target = "distanceInMiles", source = "attractionDistance.distanceInMiles")
    @Mapping(target = "rewardPoints", source = "rewardPoints")
    NearbyAttractionDto toNearbyAttractionDto(final AttractionDistance attractionDistance, final int rewardPoints, final Location location);

    @Named("toAttractionCoordinateDto")
    @Mapping(target = "attractionLatitude", source = "attractionDistance.attraction.latitude")
    @Mapping(target = "attractionLongitude", source = "attractionDistance.attraction.longitude")
    AttractionCoordinateDto toAttractionCoordinateDto(final AttractionDistance attractionDistance);

    @Named("toUserCoordinateDto")
    @Mapping(target = "userLatitude", source = "latitude")
    @Mapping(target = "userLongitude", source = "longitude")
    UserCoordinateDto toUserCoordinateDto(final Location location);
}
