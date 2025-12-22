package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.GenderOptions;
import com.bandhanbook.app.model.constants.ProfileStatus;
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
@Document(collection = "agents")
public class Agents {

    @Id
    private ObjectId id;

    @Field("user_id")
    private ObjectId userId;

    @Field("organization_id")
    private ObjectId organizationId;

    @Builder.Default
    private String gender = GenderOptions.MALE.name();

    @Field("profile_image")
    private Image profileImage;

    private String caste;

    private String address;

    @Field("phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Field("phone_verification_code")
    @Builder.Default
    private String phoneVerificationCode = null;

    @Builder.Default
    private String status = ProfileStatus.pending.name();


    private int country = 101; // India
    private int state = 4039; // Madhya Pradesh
    private int city;
    private String zip;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    @Builder.Default
    private LocalDateTime deletedAt = null;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Image {
        @Builder.Default
        private String url = null;
        @Builder.Default
        private String id = null;
    }
}
