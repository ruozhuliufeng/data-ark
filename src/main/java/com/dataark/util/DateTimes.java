package com.dataark.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimes {
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private DateTimes() {
    }

    public static String fileTimestamp() {
        return FILE_FORMAT.format(LocalDateTime.now());
    }
}
