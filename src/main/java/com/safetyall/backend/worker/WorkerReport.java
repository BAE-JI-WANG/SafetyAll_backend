package com.safetyall.backend.worker;

import com.safetyall.backend.common.Location;

/**
 * 앱이 보낸 보고 한 건. 형식 검증은 api 계층(WorkerReportForm)에서 끝난 상태로 들어온다.
 *
 * @param workerName 비어 있을 수 있다(그때는 서버가 아는 값으로 채운다)
 * @param reason     경고 보고에만 있다. 그 외에는 null.
 */
public record WorkerReport(
        String deviceId,
        String workerName,
        String workerPhoneNum,
        String workerGroup,
        String status,
        String reason,
        Location location) {

    /**
     * 보고에서 비어 있는 신원과 알 수 없는 위치를, 서버가 이미 아는 작업자 정보로 채운 사본.
     * 앱 강제 종료 경로처럼 정보를 잃은 채 온 안전 보고도 누가 어디서 보냈는지 남기기 위한 것이다.
     */
    public WorkerReport filledFrom(Worker known) {
        return new WorkerReport(
                deviceId,
                workerName.isEmpty() ? known.getWorkerName() : workerName,
                workerPhoneNum.isEmpty() ? known.getWorkerPhoneNum() : workerPhoneNum,
                workerGroup.isEmpty() ? known.getWorkerGroup() : workerGroup,
                status,
                reason,
                location.isUnknown() ? known.location() : location);
    }
}
