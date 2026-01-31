package com.bandhanbook.app.utilities;

import java.util.concurrent.atomic.AtomicLong;

public class BatchNumberGenerator {
    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis());

    public static long generateUniqueSequenceBatchNumber() {
        return counter.incrementAndGet();
    }
}