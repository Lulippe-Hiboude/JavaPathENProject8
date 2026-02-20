package com.openclassrooms.tourguide.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rewards")
@Data
public class RewardProperties {
    private int defaultProximityBuffer;
    private int attractionProximityRange;
}
