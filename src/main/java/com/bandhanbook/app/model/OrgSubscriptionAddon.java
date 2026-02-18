package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.AddOnStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "org_subscription_addons")
public class OrgSubscriptionAddon {

    @Id
    private ObjectId id;

    @Field("org_id")
    private ObjectId orgId;

    @Field("subscription_id")
    private ObjectId subscriptionId;

    @Field("max_agents")
    private int maxAgents;

    @Field("max_users")
    private int maxUsers;

    @Field("max_banners")
    private int maxBanners;

    @Field("max_advertisements")
    private int maxAdvertisements;

    @Field("price")
    private double price;

    @Field("status")
    private AddOnStatus status;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    @Builder.Default
    private LocalDateTime deletedAt = null;

}
