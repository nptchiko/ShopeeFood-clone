package org.intern.shopeefoodclone.shared.constant;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class DATE {
    public static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
