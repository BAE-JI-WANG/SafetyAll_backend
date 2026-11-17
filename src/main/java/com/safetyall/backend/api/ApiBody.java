package com.safetyall.backend.api;

import java.util.LinkedHashMap;

/**
 * 응답 본문. 기존 PHP 서버와 같은 {@code {"error": false, "message": "...", ...데이터}} 모양이다.
 */
@SuppressWarnings("serial")
final class ApiBody extends LinkedHashMap<String, Object> {

    private ApiBody(boolean error, String message) {
        put("error", error);
        put("message", message);
    }

    static ApiBody ok(String message) {
        return new ApiBody(false, message);
    }

    static ApiBody error(String message) {
        return new ApiBody(true, message);
    }

    ApiBody and(String key, Object value) {
        put(key, value);
        return this;
    }
}
