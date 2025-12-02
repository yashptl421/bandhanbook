package com.bandhanbook.app.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "tokens")
public class Token {
    @Id
    private String id;
    @Indexed(unique = true)
    private String phoneNumber;
    private String email;
    private String otp;
    @Field("role")
    private String role;
    @Field("created_at")
    @Indexed(expireAfter = "300s")
    private Instant createdAt;

}
