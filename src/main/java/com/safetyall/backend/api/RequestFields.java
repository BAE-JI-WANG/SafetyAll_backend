package com.safetyall.backend.api;

import com.safetyall.backend.common.InvalidRequestException;

/** 요청 파라미터 검증/변환. 오류 메시지는 어떤 파라미터가 왜 잘못되었는지 알려 준다. */
final class RequestFields {

    private RequestFields() {
    }

    /** 앞뒤 공백을 제거한 값. 비어 있거나 maxLength를 넘으면 거부한다. */
    static String required(String name, String value, int maxLength) {
        String text = optional(name, value, maxLength);
        if (text == null) {
            throw new InvalidRequestException(name + " is required");
        }
        return text;
    }

    /** 없거나 공백뿐이면 null. */
    static String optional(String name, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.strip();
        if (text.length() > maxLength) {
            throw new InvalidRequestException(name + " must be at most " + maxLength + " characters");
        }
        return text;
    }

    /** 위도/경도. 없으면 0.0(알 수 없음, 앱의 기존 동작과 동일). NaN/Infinity와 범위 밖은 거부한다. */
    static double coordinate(String name, String value, double limit) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        double parsed;
        try {
            parsed = Double.parseDouble(value.strip());
        } catch (NumberFormatException e) {
            throw new InvalidRequestException(name + " must be a number");
        }
        if (!Double.isFinite(parsed) || Math.abs(parsed) > limit) {
            throw new InvalidRequestException(name + " must be between " + -limit + " and " + limit);
        }
        return parsed;
    }

    static long positiveId(String name, String value) {
        String text = required(name, value, 19);
        try {
            long id = Long.parseLong(text);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException e) {
            // 아래에서 같은 메시지로 거부한다
        }
        throw new InvalidRequestException(name + " must be a positive integer");
    }
}
