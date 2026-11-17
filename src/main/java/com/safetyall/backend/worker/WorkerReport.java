package com.safetyall.backend.worker;

import com.safetyall.backend.common.Location;

/**
 * 앱이 보낸 보고 한 건. 값 검증은 api 계층(WorkerReportForm)에서 끝난 상태로 들어온다.
 *
 * @param reason 경고 보고에만 있다. 그 외에는 null.
 */
public record WorkerReport(
        String deviceId,
        String workerName,
        String workerPhoneNum,
        String workerGroup,
        String status,
        String reason,
        Location location) {
}
