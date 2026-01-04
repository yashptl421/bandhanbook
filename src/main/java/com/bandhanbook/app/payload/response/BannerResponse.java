package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Image;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BannerResponse {
    private String id;

    private String title;

    private Image image;

    private String description;

    @Field("is_active")
    private boolean active;

    @Field("created_by")
    private String createdBy;   // ref User

    @Field("organization_id")
    private String organizationId; // ref Organization

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    private LocalDateTime deletedAt;
}
