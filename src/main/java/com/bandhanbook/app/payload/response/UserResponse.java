package com.bandhanbook.app.payload.response;

import com.bandhanbook.app.model.Image;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    @JsonIgnore
    private String id;
    private String fullName;
    private String email;

    private String phoneNumber;
    private Image profileImage;

    private List<String> roles;
}
