package com.safetyall.backend.api;

import static com.safetyall.backend.api.RequestFields.coordinate;
import static com.safetyall.backend.api.RequestFields.optional;
import static com.safetyall.backend.api.RequestFields.required;

import com.safetyall.backend.common.Location;
import com.safetyall.backend.worker.WorkerReport;
import com.safetyall.backend.worker.WorkerStatus;

/**
 * 앱이 보내는 폼 파라미터(insertWorker / updateWorkerStatus / insertWorkerWarning 공통).
 * 값은 전부 문자열로 받아 직접 검증하므로 형식이 틀려도 항상 같은 모양의 400 응답이 나간다.
 */
record WorkerReportForm(
        String deviceId,
        String latitude,
        String longitude,
        String workerName,
        String workerPhoneNum,
        String workerGroup,
        String status,
        String reason) {

    /** insertWorker, updateWorkerStatus. */
    WorkerReport forWorker() {
        return report(required("status", status, 30), null);
    }

    /** insertWorkerWarning. status를 안 보내면 "경고", reason은 필수. */
    WorkerReport forWarning() {
        String warningStatus = optional("status", status, 30);
        return report(
                warningStatus != null ? warningStatus : WorkerStatus.WARNING,
                required("reason", reason, 100));
    }

    private WorkerReport report(String status, String reason) {
        return new WorkerReport(
                required("deviceId", deviceId, 64),
                required("workerName", workerName, 50),
                required("workerPhoneNum", workerPhoneNum, 30),
                required("workerGroup", workerGroup, 50),
                status,
                reason,
                new Location(coordinate("latitude", latitude, 90), coordinate("longitude", longitude, 180)));
    }
}
