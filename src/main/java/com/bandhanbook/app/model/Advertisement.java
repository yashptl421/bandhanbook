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

@Document(collection = "advertisements")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Advertisement {

    @Id
    private ObjectId id;

    @Field("event_id")
    private ObjectId eventId;

    @Field("images")
    private Image images;

    private Frequency frequency;

    @Field("duration_in_days")
    private int durationInDays;

    @Field("is_active")
    private boolean active;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
