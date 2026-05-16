package com.dataark.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class Strings2 {
    private Strings2() {
    }

    public static List<String> commaList(String text) {
        List<String> result = new ArrayList<String>();
        if (StringUtils.isBlank(text)) {
            return result;
        }
        String[] parts = text.split(",");
        for (String part : parts) {
            String value = StringUtils.trimToEmpty(part);
            if (StringUtils.isNotBlank(value)) {
                result.add(value);
            }
        }
        return result;
    }
}
