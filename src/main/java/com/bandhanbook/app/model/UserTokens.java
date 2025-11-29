package com.bandhanbook.app.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "tokens")
//@CompoundIndex(def = "{'phone_number': 1, 'role': 1}", unique = true)
public class UserTokens {

    @Field("phone_number")
    private String phoneNumber;
    private String email;
    private String otp;
    private String role;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

}
