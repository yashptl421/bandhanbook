package com.bandhanbook.app.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "users")
//@CompoundIndex( name = "phone_role_otp_idx", def = "{'phone_number': 1, 'role': 1}", unique = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Users {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("phone_number")
    private String phoneNumber;

    @Field("full_name")
    private String fullName;

    private String email;

    @Builder.Default
    private String token = null;

    @Builder.Default
    private String password = null;

    @Field("role")
    private String role;

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
}
