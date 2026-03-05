package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.AddOnStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "org_subscription_addons")
@CompoundIndex(name = "subscription_id_status_idx", def = "{'subscription_id': 1, 'status': 1}")
public class OrgSubscriptionAddon {

    @Id
    private ObjectId id;

    @Field("org_id")
    @Indexed
    private ObjectId orgId;

    @Field("subscription_id")
    @Indexed
    private ObjectId subscriptionId;

    @Field("price")
    private double price;

    @Field("status")
    private AddOnStatus status;

    private SubscriptionLimits limits;

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
