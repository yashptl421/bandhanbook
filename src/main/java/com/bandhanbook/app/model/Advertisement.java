package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.Frequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "advertisements")
public class Advertisement {

    @Id
    private ObjectId id;

    @Field("event_id")
    private ObjectId eventId;

    @Field("organization_id")
    private ObjectId organizationId;

    @Field("images")
    private Image images;

    private Frequency frequency;

    @Field("duration_in_days")
    private int durationInDays;

    @Field("created_by")
    private ObjectId createdBy;

    @Field("is_active")
    private boolean active;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
