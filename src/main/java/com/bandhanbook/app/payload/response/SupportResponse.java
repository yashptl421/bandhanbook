package com.bandhanbook.app.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportResponse {
    private String agentName;
    private String agentPhone;
    private String agentEmail;
    private String organizationName;
    private String organizationUserName;
    private String organizationPhone;
    private String organizationEmail;
}
