package com.openclassrooms.tourguide.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.tourguide.dto.NearbyAttractionDto;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.persistences.user.UserReward;
import com.openclassrooms.tourguide.service.TourGuideService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TourGuideController Integration Tests")
class TourGuideControllerIT {
    final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourGuideService tourGuideService;

    private static final String VALID_USER_NAME = "internalUser1";

    @BeforeAll
    static void setUp() {
        InternalTestHelper.setInternalUserNumber(5);
    }

    @Nested
    @DisplayName("GET / - Index Endpoint Tests")
    class IndexEndpointTests {

        @Test
        @DisplayName("Should return welcome message")
        void testIndexEndpoint() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Greetings from TourGuide!"));
        }

        @Test
        @DisplayName("Should return status 200 OK")
        void testIndexEndpointReturnsOkStatus() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /getLocation - User Location Tests")
    class GetLocationEndpointTests {

        @Test
        @DisplayName("Should return user location for valid user and contain required fields (200 OK!)")
        void testGetLocationWithValidUser() throws Exception {
            mockMvc.perform(get("/getLocation")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.location").exists())
                    .andExpect(jsonPath("$.location.latitude").isNumber())
                    .andExpect(jsonPath("$.location.longitude").isNumber())
                    .andExpect(jsonPath("$.timeVisited").exists());
        }

        @Test
        @DisplayName("Should require userName parameter or return bad request 400")
        void testGetLocationWithoutUserNameParameter() throws Exception {
            mockMvc.perform(get("/getLocation"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should work with multiple different users")
        void testMultipleUsersData() throws Exception {
            for (int i = 0; i < 3; i++) {
                String userName = "internalUser" + i;
                mockMvc.perform(get("/getLocation")
                                .param("userName", userName))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.userId").exists());
            }
        }
    }

    @Nested
    @DisplayName("GET /getNearbyAttractions - Nearby Attractions Tests")
    class GetNearbyAttractionsEndpointTests {

        @Test
        @DisplayName("Should return 5 nearby attractions for valid user")
        void testGetNearbyAttractionsReturnsCorrectCount() throws Exception {
            mockMvc.perform(get("/getNearbyAttractions")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @DisplayName("Should return attractions with required fields")
        void testGetNearbyAttractionsDataStructure() throws Exception {
            final MvcResult result = mockMvc.perform(get("/getNearbyAttractions")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk())
                    .andReturn();

            final String response = result.getResponse().getContentAsString();

            final List<Map<String, Object>> attractions = OBJECT_MAPPER.readValue(response, new TypeReference<>() {
            });
            assertThat(attractions).hasSize(5);
            final Set<String> expectedFields = Set.of(
                    "attractionName",
                    "attractionCoordinate",
                    "userCoordinate",
                    "distanceInMiles",
                    "rewardPoints"
            );

            for (Map<String, Object> attraction : attractions) {
                assertThat(attraction.keySet()).containsExactlyInAnyOrderElementsOf(expectedFields);
                for (String field : expectedFields) {
                    assertThat(attraction.get(field)).isNotNull();
                }
            }
        }

        @Test
        @DisplayName("Should return attractions sorted by distance")
        void testGetNearbyAttractionsSortedByDistance() throws Exception {
            final MvcResult result = mockMvc.perform(get("/getNearbyAttractions")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk())
                    .andReturn();

            final String response = result.getResponse().getContentAsString();
            List<NearbyAttractionDto> attractions = OBJECT_MAPPER.readValue(response, new TypeReference<>() {
            });

            assertThat(attractions).isNotEmpty();

            final List<Double> distances = attractions.stream()
                    .map(NearbyAttractionDto::getDistanceInMiles)
                    .toList();
            final List<Double> sortedDistances = new ArrayList<>(distances);
            sortedDistances.sort(Double::compareTo);

            assertThat(distances).isEqualTo(sortedDistances);
        }

        @Test
        @DisplayName("Should require userName parameter")
        void testGetNearbyAttractionsWithoutUserNameParameter() throws Exception {
            mockMvc.perform(get("/getNearbyAttractions"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return positive reward points")
        void testGetNearbyAttractionsRewardPoints() throws Exception {
            mockMvc.perform(get("/getNearbyAttractions")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].rewardPoints", everyItem(greaterThanOrEqualTo(0))));
        }
    }

    @Nested
    @DisplayName("GET /getRewards - User Rewards Tests")
    class GetRewardsEndpointTests {

        @Test
        @DisplayName("Should return rewards list for valid user")
        void testGetRewardsWithValidUser() throws Exception {
            final MvcResult result = mockMvc.perform(get("/getRewards")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(List.class)))
                    .andReturn();
            final String response = result.getResponse().getContentAsString();
            final List<UserReward> rewards = OBJECT_MAPPER.readValue(response, new TypeReference<>() {
            });
            assertThat(rewards).isNotNull();

            for (UserReward reward : rewards) {
                assertThat(reward.attraction()).isNotNull();
                assertThat(reward.visitedLocation()).isNotNull();
                assertThat(reward.rewardPoints()).isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("Should require userName parameter")
        void testGetRewardsWithoutUserNameParameter() throws Exception {
            mockMvc.perform(get("/getRewards"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /getTripDeals - Trip Deals Tests")
    class GetTripDealsEndpointTests {

        @Test
        @DisplayName("Should return trip deals for valid user")
        void testGetTripDealsWithValidUser() throws Exception {
            mockMvc.perform(get("/getTripDeals")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThan(0)))) // au moins un deal
                    .andExpect(jsonPath("$[*].name", everyItem(not(emptyOrNullString()))))
                    .andExpect(jsonPath("$[*].price", everyItem(greaterThan(0.0))))
                    .andExpect(jsonPath("$[*].tripId", everyItem(notNullValue())));
        }

        @Test
        @DisplayName("Should require userName parameter")
        void testGetTripDealsWithoutUserNameParameter() throws Exception {
            mockMvc.perform(get("/getTripDeals"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Integration Tests - Combined Flows")
    class CombinedFlowTests {

        @Test
        @DisplayName("Should complete full user journey")
        void testFullUserJourney() throws Exception {
            // Step 1: Get user location
            mockMvc.perform(get("/getLocation")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").exists());

            // Step 2: Get nearby attractions
            mockMvc.perform(get("/getNearbyAttractions")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));

            // Step 3: Get rewards
            mockMvc.perform(get("/getRewards")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk());

            // Step 4: Get trip deals
            mockMvc.perform(get("/getTripDeals")
                            .param("userName", VALID_USER_NAME))
                    .andExpect(status().isOk());
        }
    }
}
