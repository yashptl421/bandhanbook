package com.bandhanbook.app.model;

import com.bandhanbook.app.model.constants.RoleNames;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "users")
@CompoundIndex( name = "phone_email_idx", def = "{'phone_number': 1, 'email': 1}", unique = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Users {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @Field("phone_number")
    private String phoneNumber;

    @Field("profile_image")
    private Image profileImage;

    @Field("full_name")
    private String fullName;

    private String email;

    @Builder.Default
    private String password = null;

    @Field("role")
    @Singular
    private List<String> roles = new ArrayList<>();

    @Transient
    private RoleNames activeRole;

    public boolean isCandidate() {
        return activeRole == RoleNames.Candidate;
    }

    public boolean isAgent() {
        return activeRole == RoleNames.Agent;
    }

    public boolean isOrganization() {
        return activeRole == RoleNames.Organization;
    }

    public boolean isSuperUser() {
        return activeRole == RoleNames.SuperUser;
    }

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    @Builder.Default
    private LocalDateTime deletedAt = null;

    @Field("isBlocked")
    @Builder.Default
    private boolean locked = false;

    @Builder.Default
    private String token = null;

    private LocalDateTime expiryDate;

    private boolean revoked;
}
