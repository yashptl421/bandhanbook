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
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "advertisements")
@CompoundIndexes({

        @CompoundIndex(
                name = "event_active_frequency_created_idx",
                def = "{'event_id':1,'is_active':1,'frequency':1,'created_at':-1}"
        ),

        @CompoundIndex(
                name = "event_active_idx",
                def = "{'event_id':1,'is_active':1}"
        ),

        @CompoundIndex(
                name = "org_event_idx",
                def = "{'organization_id':1,'event_id':1}"
        )
})
public class Advertisement {

    @Id
    private ObjectId id;

    @Field("event_id")
    @Indexed
    private ObjectId eventId;

    @Field("organization_id")
    @Indexed
    private ObjectId organizationId;

    @Field("images")
    private Image images;
    @Indexed
    private Frequency frequency;

    @Field("duration_in_days")
    private int durationInDays;

    @Field("created_by")
    private ObjectId createdBy;

    @Field("is_active")
    @Indexed
    private boolean active;

    @CreatedDate
    @Field("created_at")
    @Indexed(direction = IndexDirection.DESCENDING)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
