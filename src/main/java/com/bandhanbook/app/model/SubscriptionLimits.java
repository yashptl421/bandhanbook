package com.bandhanbook.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class SubscriptionLimits {
    @Field("max_agents")
    private int maxAgents;

    @Field("max_users")
    private int maxUsers;

    @Field("max_banners")
    private int maxBanners;

    @Field("max_advertisements")
    private int maxAdvertisements;
}
