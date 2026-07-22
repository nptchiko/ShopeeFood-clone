package org.intern.shopeefoodclone.shared.utils;

import org.intern.shopeefoodclone.shared.constant.AppDate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class DateUtils {

    private DateUtils(){}

    public static OffsetDateTime toOffsetDateTime(Instant instant) {

        if (instant == null) return null;

        ZoneOffset offset = ZoneOffset.ofHours(AppDate.DEFAULT_TIME_OFFSET);

        return OffsetDateTime.ofInstant(instant, offset);

    }
}
