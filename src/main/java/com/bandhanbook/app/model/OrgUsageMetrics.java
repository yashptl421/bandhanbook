package com.bandhanbook.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "org_usage_metrics")
public class OrgUsageMetrics {
    @Id
    private ObjectId id;

    @Field("org_id")
    private ObjectId orgId;

    @Field("event_id")
    private ObjectId eventId;

    @Field("current_users")
    private int currentUsers;

    @Field("subscription_active")
    private boolean subscriptionActive;

    @Field("current_agents")
    private int currentAgents;

    @Field("current_banners")
    private int currentBanners;

    @Field("current_advertisements")
    private int currentAdvertisements;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
