package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.dto.NearbyAttractionDto;
import com.openclassrooms.tourguide.persistences.user.User;
import com.openclassrooms.tourguide.persistences.user.UserReward;
import com.openclassrooms.tourguide.service.TourGuideService;
import gpsUtil.location.VisitedLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tripPricer.Provider;

import java.util.List;

@RestController
public class TourGuideController {

    private final TourGuideService tourGuideService;

    public TourGuideController(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
    }

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    @RequestMapping("/getLocation")
    public VisitedLocation getLocation(@RequestParam final String userName) {
        return tourGuideService.getUserLocation(getUser(userName));
    }

    @RequestMapping("/getNearbyAttractions")
    public List<NearbyAttractionDto> getNearbyAttractions(@RequestParam final String userName) {
        final User user = getUser(userName);
        return tourGuideService.getFiveClosestAttractions(user);
    }

    @RequestMapping("/getRewards")
    public List<UserReward> getRewards(@RequestParam final String userName) {
        return tourGuideService.getUserRewards(getUser(userName));
    }

    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam final String userName) {
        return tourGuideService.getTripDeals(getUser(userName));
    }

    private User getUser(final String userName) {
        return tourGuideService.getUser(userName);
    }
}