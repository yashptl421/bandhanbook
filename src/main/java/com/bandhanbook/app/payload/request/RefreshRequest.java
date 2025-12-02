package com.bandhanbook.app.payload.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class RefreshRequest {
    private String refreshToken;
}
