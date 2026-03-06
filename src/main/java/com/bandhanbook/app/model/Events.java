package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.EventType;
import com.bandhanbook.app.model.constants.Status;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "events")
@CompoundIndexes({
        @CompoundIndex(name = "organization_event_type_idx", def = "{'organization_id': 1, 'event_type' : 1 , 'created_at': -1}"),
        @CompoundIndex(name = "role_created_at_idx", def = "{'role':1,'created_at':-1}")
})

public class Events {

    @Id
    private ObjectId id;

    private String name;

    @Field("created_by")
    private ObjectId createdBy;

    @Field("organization_id")
    @Indexed
    private ObjectId organizationId;

    private String location;

    @Field("start_date")
    private LocalDateTime startDate;

    @Field("end_date")
    private LocalDateTime endDate;

    @Field("status")
    private Status status = Status.active;

    @Field("registration_fee")
    private double registrationFee;

    @Field("event_type")
    @Indexed
    private EventType eventType;

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
