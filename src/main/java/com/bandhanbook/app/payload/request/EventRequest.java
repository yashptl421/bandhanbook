package com.bandhanbook.app.payload.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@RequiredArgsConstructor
public class EventRequest {
    private String name;
    private String organizationId;
    private String location;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
