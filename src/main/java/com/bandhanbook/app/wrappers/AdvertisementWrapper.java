package com.bandhanbook.app.wrappers;

import com.bandhanbook.app.model.Advertisement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementWrapper {
    private List<Advertisement> data = new ArrayList<>();

    private List<CountWrapper> total = new ArrayList<>();
    private List<CountWrapper> activeCount = new ArrayList<>();

    public long getTotalCount() {
        return total.isEmpty() ? 0 : total.get(0).getCount();
    }

    public long getActiveCount() {
        return activeCount.isEmpty() ? 0 : activeCount.get(0).getCount();
    }

    @Data
    public static class CountWrapper {
        private long count;
    }
}
