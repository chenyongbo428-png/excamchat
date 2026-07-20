package com.cheat.exam;

import com.jayway.jsonpath.JsonPath;

public final class JsonTestUtils {

    private JsonTestUtils() {
    }

    public static String readJson(String content, String path) {
        Object value = JsonPath.read(content, path);
        return value == null ? null : value.toString();
    }
}
