package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.ProfileStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "organizations")
//@CompoundIndex(name = "phone_role_otp_idx", def = "{'phone_number': 1, 'role': 1}", unique = true)
public class Organization {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String organizationName;

    private String address;

    private int country;

    private int state;

    private int city;


    private String zip;

    @Field("phone_verified_at")
    private String phoneVerifiedAt = null;

    @Field("phone_verification_code")
    private String phoneVerificationCode = null;

    private String status = ProfileStatus.pending.name();

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
