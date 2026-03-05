package com.bandhanbook.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "org_usage_metrics")
@CompoundIndexes({
        @CompoundIndex(
                name = "org_event_subscription_idx",
                def = "{'org_id':1,'event_id':1,'subscription_active':1}"
        ),

        @CompoundIndex(
                name = "event_subscription_idx",
                def = "{'event_id':1,'subscription_active':1}"
        ),

        @CompoundIndex(
                name = "org_subscription_idx",
                def = "{'org_id':1,'subscription_active':1}"
        )

})
public class OrgUsageMetrics {
    @Id
    private ObjectId id;

    @Field("org_id")
    @Indexed
    private ObjectId orgId;

    @Field("event_id")
    @Indexed
    private ObjectId eventId;

    @Field("current_users")
    private int currentUsers;

    @Field("subscription_active")
    @Indexed
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
