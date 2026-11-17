package com.safetyall.backend.api;

/** 대시보드가 읽는 경고 한 건. idx는 updateWarningResult에 그대로 돌려보내는 번호다. */
record WarningView(
        long idx,
        String workerName,
        String workerGroup,
        String workerPhoneNum,
        String status,
        String reason,
        String result,
        double latitude,
        double longitude,
        String dateTime) {
}
