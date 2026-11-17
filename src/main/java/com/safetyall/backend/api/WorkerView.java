package com.safetyall.backend.api;

/** 대시보드가 읽는 작업자 한 명. 기기 ID는 노출하지 않는다. */
record WorkerView(
        String workerName,
        String workerGroup,
        String workerPhoneNum,
        String status,
        double latitude,
        double longitude,
        String dateTime) {
}
