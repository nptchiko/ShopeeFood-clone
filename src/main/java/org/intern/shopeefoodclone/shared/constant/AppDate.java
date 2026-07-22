package org.intern.shopeefoodclone.shared.constant;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class AppDate {
    public static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
    private AppDate(){}

    public static final int DEFAULT_TIME_OFFSET = 7;
}
