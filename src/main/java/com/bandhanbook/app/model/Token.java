package com.bandhanbook.app.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "tokens")
@CompoundIndex(
        name = "phone_role_idx",
        def = "{'phone_number': 1, 'role': 1}",
        unique = true
)
public class Token {
    @Id
    private ObjectId id;
    @Field("phone_number")
    private String phoneNumber;
    private String role;
    @Field("last_sent_at")
    private Instant lastSentAt;
    @Field("window_start")
    private Instant windowStart;
    @Field("request_count_in_window")
    private int requestCountInWindow;
    @Field("failed_attempts")
    private int failedAttempts;
    private String email;
    private String otp;

    @Field("created_at")
    private Instant createdAt;
    @Field("expires_at")
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
}
