package com.bandhanbook.app.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "banners")
@JsonInclude(JsonInclude.Include.NON_NULL)
@CompoundIndex(
        name = "banner_org_active_created_idx",
        def = "{'organization_id': 1, 'is_active': 1, 'created_at': -1}"
)
public class Banners {

    @Id
    private ObjectId id;

    @Field("title")
    private String title;

    @Field("image")
    private Image image;

    @Field("description")
    private String description;

    @Field("is_active")
    @Builder.Default
    private boolean active = true;

    @Field("created_by")
    private ObjectId createdBy;   // ref User

    @Field("organization_id")
    @Indexed
    private ObjectId organizationId; // ref Organization

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    private LocalDateTime deletedAt;
}
