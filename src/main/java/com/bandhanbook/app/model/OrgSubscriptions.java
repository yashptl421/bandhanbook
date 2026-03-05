package com.bandhanbook.app.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "orgsubscriptions")
@CompoundIndexes({

        @CompoundIndex(
                name = "org_event_active_idx",
                def = "{'org_id':1,'event_id':1,'active':1}"
        ),

        @CompoundIndex(
                name = "event_active_idx",
                def = "{'event_id':1,'active':1}"
        ),

        @CompoundIndex(
                name = "org_idx",
                def = "{'org_id':1}"
        ),

        @CompoundIndex(
                name = "org_event_active_partial_idx",
                def = "{'org_id':1,'event_id':1}",
                partialFilter = "{ 'active': true }"
        )
})
public class OrgSubscriptions {

    @Id
    private ObjectId id;

    @Field("org_id")
    private ObjectId orgId;

    private SubscriptionLimits limits;
    @Field("event_id")
    private ObjectId eventId;

    @Field("plan_id")
    private ObjectId planId;

    @Field("plan_name")
    private String planName;

    @Field("registration_period")
    private String registrationPeriod;

    @Field("start_date")
    private String startDate;

    @Field("end_date")
    private String endDate;

    @Field("is_active")
    private boolean active;

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
