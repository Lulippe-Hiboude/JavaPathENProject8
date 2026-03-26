package com.openclassrooms.tourguide.persistences.user;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import tripPricer.Provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;


public class User {
    private final ReentrantLock rewardsLock = new ReentrantLock();
    private final ReentrantLock visitedLocationsLock = new ReentrantLock();
    private final UUID userId;
    private final String userName;
    private String phoneNumber;
    private String emailAddress;
    private Date latestLocationTimestamp;
    private final List<VisitedLocation> visitedLocations = new CopyOnWriteArrayList<>();
    private final List<UserReward> userRewards = new CopyOnWriteArrayList<>();
    private UserPreferences userPreferences = new UserPreferences();
    private List<Provider> tripDeals = new ArrayList<>();

    public User(UUID userId, String userName, String phoneNumber, String emailAddress) {
        this.userId = userId;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.emailAddress = emailAddress;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }


    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setLatestLocationTimestamp(Date latestLocationTimestamp) {
        this.latestLocationTimestamp = latestLocationTimestamp;
    }

    public Date getLatestLocationTimestamp() {
        return latestLocationTimestamp;
    }


    public void addToVisitedLocations(VisitedLocation visitedLocation) {
        visitedLocationsLock.lock();
        try {
            visitedLocations.add(visitedLocation);
        } finally {
            visitedLocationsLock.unlock();
        }
    }

    public List<VisitedLocation> getVisitedLocations() {
        return List.copyOf(visitedLocations);
    }

    public void clearVisitedLocations() {
        visitedLocationsLock.lock();
        try {
            visitedLocations.clear();
        } finally {
            visitedLocationsLock.unlock();
        }

    }

    public void addUserReward(UserReward userReward) {
        rewardsLock.lock();
        try {
            if (!hasRewardForAttraction(userReward.attraction())) {
                userRewards.add(userReward);
            }
        } finally {
            rewardsLock.unlock();
        }
    }

    public List<UserReward> getUserRewards() {
        return List.copyOf(userRewards);
    }

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }


    public void setUserPreferences(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }

    public VisitedLocation getLastVisitedLocation() {
        visitedLocationsLock.lock();
        try {
            return visitedLocations.getLast();
        } finally {
            visitedLocationsLock.unlock();
        }
    }

    public void setTripDeals(List<Provider> tripDeals) {
        this.tripDeals = tripDeals;
    }

    public boolean hasRewardForAttraction(final Attraction attraction) {
        rewardsLock.lock();
        try {
            return userRewards.stream().anyMatch(r -> r.attraction().attractionName.equals(attraction.attractionName));
        } finally {
            rewardsLock.unlock();
        }
    }
}
