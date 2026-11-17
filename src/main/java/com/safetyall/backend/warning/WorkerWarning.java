package com.safetyall.backend.warning;

import java.time.Instant;

import com.safetyall.backend.common.Location;
import com.safetyall.backend.worker.WorkerStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 경고 이력 한 건. 작업자가 종료되어도 남도록 작업자 정보를 복사해 둔다.
 * resolvedAt이 null이면 아직 처리되지 않은 경고다.
 */
@Entity
@Table(name = "worker_warnings")
public class WorkerWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    @Column(name = "device_id", nullable = false, updatable = false, length = 64)
    private String deviceId;

    @Column(name = "worker_name", nullable = false, updatable = false, length = 50)
    private String workerName;

    @Column(name = "worker_phone_num", nullable = false, updatable = false, length = 30)
    private String workerPhoneNum;

    @Column(name = "worker_group", nullable = false, updatable = false, length = 50)
    private String workerGroup;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, updatable = false, length = 100)
    private String reason;

    @Column(length = 500)
    private String result;

    @Column(nullable = false, updatable = false)
    private double latitude;

    @Column(nullable = false, updatable = false)
    private double longitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected WorkerWarning() {
    }

    public WorkerWarning(String deviceId, String workerName, String workerPhoneNum, String workerGroup,
                         String status, String reason, Location location, Instant createdAt) {
        this.deviceId = deviceId;
        this.workerName = workerName;
        this.workerPhoneNum = workerPhoneNum;
        this.workerGroup = workerGroup;
        this.status = status;
        this.reason = reason;
        this.latitude = location.latitude();
        this.longitude = location.longitude();
        this.createdAt = createdAt;
    }

    /** 담당자가 처리 내용을 기록했다. 다시 호출하면 내용을 고쳐 쓴다. */
    public void resolve(String result, Instant now) {
        this.result = result;
        this.status = WorkerStatus.RESOLVED;
        this.resolvedAt = now;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }

    public Long getIdx() {
        return idx;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getWorkerPhoneNum() {
        return workerPhoneNum;
    }

    public String getWorkerGroup() {
        return workerGroup;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getResult() {
        return result;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
