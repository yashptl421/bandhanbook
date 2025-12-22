package com.bandhanbook.app.model;

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
@Document(collection = "events")
public class Events {

    @Id
    private ObjectId id;

    private String name;

    @Field("created_by")
    private ObjectId createdBy;

    @Field("organization_id")
    private ObjectId organizationId;

    private String location;

    @Field("start_date")
    private LocalDateTime startDate;

    @Field("end_date")
    private LocalDateTime endDate;

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
