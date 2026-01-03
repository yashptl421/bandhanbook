package com.bandhanbook.app.wrappers;

import com.bandhanbook.app.payload.response.BannerResponse;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BannerWrapper {

    private List<BannerResponse> data;

    private long activeCount;
    private long inactiveCount;

    private ApiResponse.Meta meta;
}
