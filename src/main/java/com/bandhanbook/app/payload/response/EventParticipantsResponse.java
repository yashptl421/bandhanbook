package com.bandhanbook.app.payload.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventParticipantsResponse {
    private String id;
    private String candidateId;
    private String eventId;
    private String addedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
