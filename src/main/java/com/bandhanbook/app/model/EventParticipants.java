package com.bandhanbook.app.model;


import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "eventparticipants")
@CompoundIndex(name = "candidate_event_id_idx", def = "{'candidate_id': 1, 'event_id': 1}", unique = true)
public class EventParticipants {

    @Id
    private ObjectId id;

    @Field("candidate_id")
    private ObjectId candidateId;

    @Field("event_id")
    private ObjectId eventId;

    @Field("added_by")
    private ObjectId addedBy;

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
